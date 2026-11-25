package org.wildfly.examples.artifact;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.AiServices;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Runs {@link ArtifactDescriber} against local sample solutions and several Ollama models,
 * without WildFly. Uses the real prompts, {@link ArtifactTools} and {@link ArtifactStorage}.
 *
 * <p>Samples directory layout (one subdirectory per sample):
 * <pre>
 * samples/
 *   bst/
 *     assignment.txt      task description
 *     solution/           solution files (or solution.zip instead)
 *   calculator/
 *     ...
 * </pre>
 *
 * <p>Model settings come from the describer-model entries in microprofile-config.properties.
 * Any of them can be overridden with a system property of the same name, e.g.
 * {@code -Ddev.langchain4j.cdi.plugin.describer-model.config.num-ctx=16384}.
 *
 * <p>Other options: {@code -Dbench.runs=N} (repeats per sample), {@code -Dbench.log=true}
 * (log HTTP requests/responses).
 *
 * <p>Results are printed and saved to {@code <samples>/results/<timestamp>.md}.
 */
public class DescriberBench {
    private static final String PREFIX = "dev.langchain4j.cdi.plugin.describer-model.config.";

    record Sample(String name, String assignment, UUID uuid, String structure) {}

    record Result(String model, Sample sample, int run, long millis, Stats stats, String description) {}

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: DescriberBench <samples-dir> <model> [<model> ...]");
            System.exit(1);
        }

        final Path samplesDir = Path.of(args[0]).toAbsolutePath();
        final List<String> models = Arrays.asList(args).subList(1, args.length);
        final int runs = Integer.getInteger("bench.runs", 1);
        final boolean log = Boolean.getBoolean("bench.log");
        final Properties config = loadConfig();

        final ArtifactStorage storage = new ArtifactStorage();
        storage.dir = Files.createTempDirectory("describer-bench").toString();
        storage.init();

        final ArtifactTools tools = new ArtifactTools();
        tools.storage = storage;

        final List<Sample> samples = loadSamples(samplesDir, storage);
        if (samples.isEmpty()) {
            System.err.println("No samples found in " + samplesDir);
            System.exit(1);
        }

        final List<Result> results = new ArrayList<>();

        // models in the outer loop, so Ollama loads each model only once
        for (String model : models) {
            final Stats stats = new Stats();
            final ArtifactDescriber describer = AiServices.builder(ArtifactDescriber.class)
                    .chatModel(buildModel(config, model, stats, log))
                    .tools(tools)
                    .build();

            for (Sample sample : samples) {
                for (int run = 1; run <= runs; run++) {
                    System.out.printf("\033[1m\033[32m%n=== %s | %s | run %d ===%n\033[0m", model, sample.name(), run);
                    stats.reset();

                    final long start = System.currentTimeMillis();
                    String description;
                    try {
                        description = describer.describe(sample.assignment(), sample.structure(),
                                InvocationParameters.from(Map.of("uuid", sample.uuid())));
                    } catch (Exception e) {
                        description = "ERROR: " + e;
                    }
                    final long millis = System.currentTimeMillis() - start;

                    final Result result = new Result(model, sample, run, millis, stats.copy(), description);
                    results.add(result);

                    System.out.println(summary(result, config));
                    System.out.println(description);
                }
            }
        }

        final Path report = writeReport(samplesDir, results, config);
        System.out.println("\nReport: " + report);
    }

    // ---------------------------------------------------------------- model

    static ChatModel buildModel(Properties config, String model, ChatModelListener listener, boolean log) {
        final var builder = OllamaChatModel.builder()
                .baseUrl(config.getProperty(PREFIX + "base-url", "http://localhost:11434"))
                .modelName(model)
                .listeners(List.of(listener))
                .logRequests(log)
                .logResponses(log);

        set(config, "temperature", Double::valueOf, builder::temperature);
        set(config, "top-p", Double::valueOf, builder::topP);
        set(config, "top-k", Integer::valueOf, builder::topK);
        set(config, "num-ctx", Integer::valueOf, builder::numCtx);
        set(config, "num-predict", Integer::valueOf, builder::numPredict);
        set(config, "think", Boolean::valueOf, builder::think);
        set(config, "timeout", Duration::parse, builder::timeout);

        return builder.build();
    }

    static <T> void set(Properties config, String key, Function<String, T> parse, Consumer<T> setter) {
        final String value = config.getProperty(PREFIX + key);
        if (value != null && !value.isBlank()) setter.accept(parse.apply(value.trim()));
    }

    static Properties loadConfig() throws IOException {
        final Properties config = new Properties();
        try (InputStream in = DescriberBench.class.getClassLoader()
                .getResourceAsStream("META-INF/microprofile-config.properties")) {
            if (in != null) config.load(in);
        }
        // system properties override the file, like in MicroProfile Config
        for (String key : System.getProperties().stringPropertyNames()) {
            if (key.startsWith(PREFIX)) config.setProperty(key, System.getProperty(key));
        }
        return config;
    }

    // ---------------------------------------------------------------- samples

    static List<Sample> loadSamples(Path samplesDir, ArtifactStorage storage) throws IOException {
        final List<Sample> samples = new ArrayList<>();
        final List<Path> dirs;
        try (Stream<Path> list = Files.list(samplesDir)) {
            dirs = list.filter(Files::isDirectory)
                    .filter(d -> !d.getFileName().toString().equals("results"))
                    .sorted()
                    .toList();
        }

        for (Path dir : dirs) {
            final Path assignment = dir.resolve("assignment.txt");
            final Path zip = dir.resolve("solution.zip");
            final Path solution = dir.resolve("solution");

            if (!Files.isRegularFile(assignment)) {
                System.err.println("Skipping " + dir.getFileName() + ": no assignment.txt");
                continue;
            }

            final UUID uuid = UUID.randomUUID();
            final Path target = storage.root.resolve(uuid + ".zip");
            if (Files.isRegularFile(zip)) Files.copy(zip, target);
            else if (Files.isDirectory(solution)) zipDirectory(solution, target);
            else {
                System.err.println("Skipping " + dir.getFileName() + ": no solution/ or solution.zip");
                continue;
            }

            // same structure string as ArtifactAnalysisWorker
            final StringBuilder structure = new StringBuilder();
            for (var file : storage.getStructure(uuid)) structure.append(file.name()).append('\n');

            samples.add(new Sample(dir.getFileName().toString(),
                    Files.readString(assignment, StandardCharsets.UTF_8), uuid, structure.toString()));
        }
        return samples;
    }

    static void zipDirectory(Path dir, Path target) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(target));
             Stream<Path> walk = Files.walk(dir)) {
            for (Path file : walk.filter(Files::isRegularFile).sorted().toList()) {
                zip.putNextEntry(new ZipEntry(dir.relativize(file).toString().replace('\\', '/')));
                Files.copy(file, zip);
                zip.closeEntry();
            }
        }
    }

    // ---------------------------------------------------------------- stats

    static final class Stats implements ChatModelListener {
        int requests;
        int toolCalls;
        int peakInputTokens;
        int totalOutputTokens;
        boolean toolCallAsText;

        void reset() {
            requests = toolCalls = peakInputTokens = totalOutputTokens = 0;
            toolCallAsText = false;
        }

        Stats copy() {
            final Stats s = new Stats();
            s.requests = requests;
            s.toolCalls = toolCalls;
            s.peakInputTokens = peakInputTokens;
            s.totalOutputTokens = totalOutputTokens;
            s.toolCallAsText = toolCallAsText;
            return s;
        }

        @Override
        public void onResponse(ChatModelResponseContext context) {
            requests++;

            final TokenUsage usage = context.chatResponse().tokenUsage();
            if (usage != null) {
                if (usage.inputTokenCount() != null)
                    peakInputTokens = Math.max(peakInputTokens, usage.inputTokenCount());
                if (usage.outputTokenCount() != null)
                    totalOutputTokens += usage.outputTokenCount();
            }

            final AiMessage message = context.chatResponse().aiMessage();
            if (message.hasToolExecutionRequests()) {
                toolCalls += message.toolExecutionRequests().size();
            } else if (message.text() != null && message.text().contains("\"readFile\"")) {
                // the model wrote the call as plain text instead of a real tool call
                toolCallAsText = true;
            }
        }
    }

    // ---------------------------------------------------------------- output

    static List<String> warnings(Result r, Properties config) {
        final List<String> warnings = new ArrayList<>();
        if (r.description().startsWith("ERROR:")) warnings.add("error");
        if (r.stats().toolCalls == 0) warnings.add("no file read - description is made up");
        if (r.stats().toolCallAsText) warnings.add("tool call written as text");

        final String numCtx = config.getProperty(PREFIX + "num-ctx");
        if (numCtx != null && r.stats().peakInputTokens >= Integer.parseInt(numCtx.trim()) * 0.95)
            warnings.add("prompt hit num-ctx - context probably truncated");
        return warnings;
    }

    static String summary(Result r, Properties config) {
        final Stats s = r.stats();
        final List<String> warnings = warnings(r, config);
        return String.format("time %.1fs | requests %d | readFile %d | peak prompt %d tok | output %d tok%s",
                r.millis() / 1000.0, s.requests, s.toolCalls, s.peakInputTokens, s.totalOutputTokens,
                warnings.isEmpty() ? "" : " | WARN: " + String.join(", ", warnings));
    }

    static Path writeReport(Path samplesDir, List<Result> results, Properties config) throws IOException {
        final StringBuilder md = new StringBuilder();
        md.append("# Describer bench ").append(LocalDateTime.now().withNano(0)).append("\n\n");

        md.append("Config:\n\n");
        config.stringPropertyNames().stream()
                .filter(k -> k.startsWith(PREFIX))
                .filter(k -> !k.endsWith("model-name") && !k.contains(".log-"))
                .sorted()
                .forEach(k -> md.append("- ").append(k.substring(PREFIX.length()))
                        .append(" = ").append(config.getProperty(k)).append('\n'));

        // grouped by sample, so models can be compared side by side
        final Map<String, List<Result>> bySample = new LinkedHashMap<>();
        for (Result r : results) bySample.computeIfAbsent(r.sample().name(), k -> new ArrayList<>()).add(r);

        for (var entry : bySample.entrySet()) {
            md.append("\n## ").append(entry.getKey()).append("\n");
            for (Result r : entry.getValue()) {
                md.append("\n### ").append(r.model()).append(" (run ").append(r.run()).append(")\n\n")
                        .append(summary(r, config)).append("\n\n```\n")
                        .append(r.description().strip()).append("\n```\n");
            }
        }

        final Path dir = samplesDir.resolve("results");
        Files.createDirectories(dir);
        final Path file = dir.resolve(
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".md");
        Files.writeString(file, md, StandardCharsets.UTF_8);
        return file;
    }
}
