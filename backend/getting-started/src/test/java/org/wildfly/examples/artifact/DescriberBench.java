package org.wildfly.examples.artifact;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.AiServices;
import org.wildfly.examples.competence.BenchCompetenceIndex;
import org.wildfly.examples.competence.Competence;
import org.wildfly.examples.competence.CompetenceIndexer;
import org.wildfly.examples.task.AssignmentDescriber;

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
 * Runs the analysis flow of {@link ArtifactAnalysisWorker} against local sample solutions and
 * several Ollama models, without WildFly: {@link ArtifactDescriber} writes the description, then
 * {@link CompetenceIndexer} searches the competence catalogue with the description and with the
 * assignment. Uses the real prompts, {@link ArtifactTools}, {@link ArtifactStorage} and indexer.
 *
 * <p>Samples directory layout (one subdirectory per sample):
 * <pre>
 * samples/
 *   bst/
 *     assignment.txt      task description
 *     solution/           solution files (or solution.zip instead)
 *     expected.txt        optional: competences that should be found, one name or id per line
 *   calculator/
 *     ...
 * </pre>
 *
 * <p>Model settings come from the describer-model entries in microprofile-config.properties.
 * Any of them can be overridden with a system property of the same name, e.g.
 * {@code -Ddev.langchain4j.cdi.plugin.describer-model.config.num-ctx=16384}.
 *
 * <p>Other options:
 * <ul>
 *   <li>{@code -Dbench.runs=N} repeats per sample,</li>
 *   <li>{@code -Dbench.log=true} logs HTTP requests/responses,</li>
 *   <li>{@code -Dbench.competences=false} skips the competence search,</li>
 *   <li>{@code -Dbench.topK=N} results per search (default 20, as in the worker),</li>
 *   <li>{@code -Dbench.env=path} database settings file (default config/.env),
 *       see {@link BenchCompetenceIndex}.</li>
 * </ul>
 *
 * <p>Results are printed and saved to {@code <samples>/results/<timestamp>.md}.
 */
public class DescriberBench {
    private static final String PREFIX = "dev.langchain4j.cdi.plugin.describer-model.config.";

    record Sample(String name, String assignment, UUID uuid, String structure, List<String> expected) {}

    /** A competence found by the search, with its score from each query (null if not in that query's top K). */
    record Hit(int competenceId, Double descriptionScore, Double descriptionDivScore, Double assignmentRawScore, Double assignmentDescriptionScore, Double assignmentCompetenciesScore) {
        double best() {
            return Math.max(
                        Math.max(
                            Math.max(
                                    descriptionScore == null ? 0 : descriptionScore,
                                    assignmentRawScore == null ? 0 : assignmentRawScore
                            ),
                            assignmentDescriptionScore == null ? 0 : assignmentDescriptionScore
                        ),
                        assignmentCompetenciesScore == null ? 0 : assignmentCompetenciesScore
            );
        }
    }

    record Result(String model, Sample sample, int run, long millis, Stats stats, String description,
                  String assignmentRaw, String assignmentDescription, String assignmentComp, List<Hit> hits) {}

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: DescriberBench <samples-dir> <model> [<model> ...]");
            System.exit(1);
        }

        final Path samplesDir = Path.of(args[0]).toAbsolutePath();
        final List<String> models = Arrays.asList(args).subList(1, args.length);
        final int runs = Integer.getInteger("bench.runs", 1);
        final int topK = Integer.getInteger("bench.topK", 20);
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

        final BenchCompetenceIndex.Loaded catalogue = loadCatalogue(samplesDir, samples);
        final List<Result> results = new ArrayList<>();

        // models in the outer loop, so Ollama loads each model only once
        for (String model : models) {
            final Stats stats = new Stats();
            final ArtifactDescriber describer = AiServices.builder(ArtifactDescriber.class)
                    .chatModel(buildModel(config, model, stats, log))
                    .tools(tools)
                    .build();
            final AssignmentDescriber assignmentDescriber = AiServices.builder(AssignmentDescriber.class)
                    .chatModel(buildModel(config, model, stats, log))
                    //.tools(tools)
                    .build();

            for (Sample sample : samples) {
                for (int run = 1; run <= runs; run++) {
                    System.out.printf("\033[1m\033[32m%n=== %s | %s | run %d ===%n\033[0m", model, sample.name(), run);
                    stats.reset();

                    final long start = System.currentTimeMillis();
                    String description, assignmentDescription, assignmentComp;

                    try {
                        description = describer.describe(sample.assignment(), sample.structure(),
                                InvocationParameters.from(Map.of("uuid", sample.uuid())));
                    } catch (Exception e) {
                        description = "ERROR: " + e;
                    }
                    try {
                        assignmentDescription = assignmentDescriber.describe(sample.assignment());
                    } catch (Exception e) {
                        assignmentDescription = "ERROR: " + e;
                    }
                    try {
                        assignmentComp = assignmentDescriber.listSkills(sample.assignment());
                    } catch (Exception e) {
                        assignmentComp = "ERROR: " + e;
                    }

                    final long millis = System.currentTimeMillis() - start;

                    final Result result = new Result(model, sample, run, millis, stats.copy(), description,
                            sample.assignment(), assignmentDescription, assignmentComp, new ArrayList<>());
                    results.add(result);

                    System.out.println(summary(result, config));
                    System.out.println(assignmentDescription);
                    System.out.println();
                    System.out.println(description);
                }
            }
        }

        // after all descriptions, so Ollama does not swap the chat and embedding models on every run
        if (catalogue != null) searchCompetences(results, catalogue, topK);

        final Path report = writeReport(samplesDir, results, config, catalogue);
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
        set(config, "seed", Integer::valueOf, builder::seed);
        set(config, "repeat-penalty", Double::valueOf, builder::repeatPenalty);
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
                    .filter(d -> {
                        final String name = d.getFileName().toString();
                        return !name.equals("results") && !name.startsWith(".");
                    })
                    .sorted()
                    .toList();
        }

        for (Path dir : dirs) {
            final Path assignment = dir.resolve("assignment.txt");
            final Path zip = dir.resolve("solution.zip");
            final Path solution = dir.resolve("solution");
            final Path expected = dir.resolve("expected.txt");

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

            final List<String> expectedCompetences = Files.isRegularFile(expected)
                    ? Files.readAllLines(expected, StandardCharsets.UTF_8).stream()
                        .map(String::strip)
                        .filter(l -> !l.isEmpty() && !l.startsWith("#"))
                        .toList()
                    : List.of();

            samples.add(new Sample(dir.getFileName().toString(),
                    Files.readString(assignment, StandardCharsets.UTF_8), uuid, structure.toString(),
                    expectedCompetences));
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

    // ---------------------------------------------------------------- competences

    static BenchCompetenceIndex.Loaded loadCatalogue(Path samplesDir, List<Sample> samples) {
        if (!Boolean.parseBoolean(System.getProperty("bench.competences", "true"))) return null;

        final BenchCompetenceIndex.Loaded catalogue;
        try {
            catalogue = BenchCompetenceIndex.load(samplesDir.resolve(".cache"));
        } catch (Exception e) {
            System.err.println("Competence catalogue not available, competence search skipped: " + e);
            System.err.println("(-Dbench.competences=false skips it without this message)");
            return null;
        }

        // typos in expected.txt would silently lower the score, so report them up front
        for (Sample sample : samples) {
            for (String expected : sample.expected()) {
                if (catalogue.competences().values().stream().noneMatch(c -> matches(expected, c)))
                    System.err.printf("%s/expected.txt: \"%s\" is not in the catalogue%n", sample.name(), expected);
            }
        }
        return catalogue;
    }

    static void searchCompetences(List<Result> results, BenchCompetenceIndex.Loaded catalogue, int topK) {
        System.out.printf("%nSearching competences (top %d for the description + top %d for the assignment)...%n",
                topK, topK);

        // the assignment query does not depend on the model, so search it once per sample
        final Map<String, List<CompetenceIndexer.Match>> byAssignment = new HashMap<>();

        for (Result r : results) {
            final List<CompetenceIndexer.Match> fromAssignmentRaw = byAssignment.computeIfAbsent(r.sample().name(),
                    k -> {
                        final String content = String.format("""
Instruct: Given a description of an assignment, retrieve competencies required to solve it
Query: %s""",
                                r.sample.assignment);
                        return catalogue.indexer().search(content, topK);
                    });

            final List<CompetenceIndexer.Match> fromAssignmentDesc = r.assignmentDescription().startsWith("ERROR:")
                    ? List.of()
                    : catalogue.indexer().search(
                            String.format("""
Instruct: Given a description of an assignment, retrieve competencies required to solve it
Query: %s""",
                                    r.assignmentDescription()), topK);

            final List<CompetenceIndexer.Match> fromAssignmentComp = r.description().startsWith("ERROR:")
                    ? List.of()
                    : catalogue.indexer().search(
                            String.format("""
Instruct: Given a list of competencies, retrieve competencies that are similar, or are a part of them
Query: %s""",
                            r.assignmentComp()), topK);

            final List<CompetenceIndexer.Match> fromDescription = r.description().startsWith("ERROR:")
                    ? List.of()
                    : catalogue.indexer().search(
                            String.format("""
Instruct: Given an observation about a student's program, retrieve competencies demonstrated by it
Query: %s""",
                    r.description()), topK);

            final List<CompetenceIndexer.Match> fromDescriptionDivided = new ArrayList<>();
            {
                final Map<Integer, Double> competenceMap = new HashMap<>();
                final String[] divided = r.description().split("\n");

                for (String s : divided) {
                    if (s.isEmpty()) continue;

                    final List<CompetenceIndexer.Match> matches = catalogue.indexer().search(
                            String.format("""
Instruct: Given an observation about a student's program, retrieve competencies demonstrated by it
Query: %s""",
                            s), topK);

                    for (CompetenceIndexer.Match m : matches) {
                        if (competenceMap.containsKey(m.competenceId())) competenceMap.put(m.competenceId(), Math.max(competenceMap.get(m.competenceId()), m.score()));
                        else competenceMap.put(m.competenceId(), m.score());
                    }
                }

                fromDescriptionDivided.addAll(competenceMap.entrySet().stream().map(e -> new CompetenceIndexer.Match(e.getKey(), "", e.getValue())).toList());
            }

            r.hits().addAll(merge(fromDescription, fromDescriptionDivided, fromAssignmentRaw, fromAssignmentDesc, fromAssignmentComp));

            System.out.printf("\033[1m\033[32m%n=== %s | %s | run %d ===%n\033[0m", r.model(), r.sample().name(), r.run());
            System.out.println(competenceSummary(r, catalogue.competences()));
            System.out.print(competenceTable(r, catalogue.competences()));
        }
    }

    static List<Hit> merge(List<CompetenceIndexer.Match> fromDescription, List<CompetenceIndexer.Match> fromDescriptionDiv,
                           List<CompetenceIndexer.Match> fromAssignmentRaw, List<CompetenceIndexer.Match> fromAssignmentDesc,
                           List<CompetenceIndexer.Match> fromAssignmentComp) {
        final Map<Integer, Double[]> scores = new LinkedHashMap<>();

        for (var m : fromDescription) scores.computeIfAbsent(m.competenceId(), k -> new Double[5])[0] = m.score();
        for (var m : fromDescriptionDiv) scores.computeIfAbsent(m.competenceId(), k -> new Double[5])[1] = m.score();
        for (var m : fromAssignmentRaw) scores.computeIfAbsent(m.competenceId(), k -> new Double[5])[2] = m.score();
        for (var m : fromAssignmentDesc) scores.computeIfAbsent(m.competenceId(), k -> new Double[5])[3] = m.score();
        for (var m : fromAssignmentComp) scores.computeIfAbsent(m.competenceId(), k -> new Double[5])[4] = m.score();

        return scores.entrySet().stream()
                .map(e -> new Hit(e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2], e.getValue()[3], e.getValue()[4]))
                .sorted(Comparator.comparingDouble(Hit::best).reversed())
                .toList();
    }

    static boolean matches(String expected, Competence c) {
        return expected.equals(String.valueOf(c.id)) || expected.equalsIgnoreCase(c.name.strip());
    }

    static boolean isExpected(Sample sample, Competence c) {
        return c != null && sample.expected().stream().anyMatch(e -> matches(e, c));
    }

    static String competenceSummary(Result r, Map<Integer, Competence> competences) {
        final List<Hit> hits = r.hits();
        final long descriptionOnly = hits.stream().filter(
                h -> h.descriptionScore() != null && h.assignmentRawScore() == null
                        && h.assignmentDescriptionScore() == null && h.assignmentCompetenciesScore() == null
        ).count();
        final long assignmentRawOnly = hits.stream().filter(
                h -> h.descriptionScore() == null && h.assignmentRawScore() != null
                        && h.assignmentDescriptionScore() == null && h.assignmentCompetenciesScore() == null
        ).count();
        final long assignmentDescOnly = hits.stream().filter(
                h -> h.descriptionScore() == null && h.assignmentRawScore() == null
                        && h.assignmentDescriptionScore() != null && h.assignmentCompetenciesScore() == null
        ).count();
        final long assignmentCompOnly = hits.stream().filter(
                h -> h.descriptionScore() == null && h.assignmentRawScore() == null
                        && h.assignmentDescriptionScore() == null && h.assignmentCompetenciesScore() != null
        ).count();
        final long all = hits.size() - descriptionOnly - assignmentRawOnly - assignmentDescOnly - assignmentCompOnly;

        final StringBuilder s = new StringBuilder(String.format(
                "competences: %d unique (description only %d, assignmentRaw only %d,\n assignmentDesc only %d, assignmentComp only %d, all %d)",
                hits.size(), descriptionOnly, assignmentRawOnly, assignmentDescOnly, assignmentCompOnly, all));

        final List<String> expected = r.sample().expected();
        if (!expected.isEmpty()) {
            final List<String> missing = expected.stream()
                    .filter(e -> hits.stream().noneMatch(h -> {
                        final Competence c = competences.get(h.competenceId());
                        return c != null && matches(e, c);
                    }))
                    .toList();
            s.append(String.format(" | expected found %d/%d", expected.size() - missing.size(), expected.size()));
            if (!missing.isEmpty()) s.append(" | missing: ").append(String.join("; ", missing));
        }
        return s.toString();
    }

    /** Markdown table; ✓ marks competences listed in expected.txt. */
    static String competenceTable(Result r, Map<Integer, Competence> competences) {
        final StringBuilder t = new StringBuilder("| solutionDescription | solutionDescriptionDiv | assignmentRaw | assignmentDesc | assignmentComp | competence |\n|---:|---:|---:|---:|---:|---|\n");
        for (Hit h : r.hits()) {
            final Competence c = competences.get(h.competenceId());
            t.append("| ").append(score(h.descriptionScore()))
                    .append(" | ").append(score(h.descriptionDivScore()))
                    .append(" | ").append(score(h.assignmentRawScore()))
                    .append(" | ").append(score(h.assignmentDescriptionScore()))
                    .append(" | ").append(score(h.assignmentCompetenciesScore()))
                    .append(" | ").append(isExpected(r.sample(), c) ? "✓ " : "")
                    .append(c == null ? "?" : c.name).append(" (").append(h.competenceId()).append(") |\n");
        }
        return t.toString();
    }

    static String score(Double score) {
        return score == null ? "" : String.format(Locale.ROOT, "%.3f", score);
    }

    // ---------------------------------------------------------------- stats

    static final class Stats implements ChatModelListener {
        int requests;
        int toolCalls;
        int peakInputTokens;
        int totalOutputTokens;
        boolean toolCallAsText;
        boolean cutOff;

        void reset() {
            requests = toolCalls = peakInputTokens = totalOutputTokens = 0;
            toolCallAsText = cutOff = false;
        }

        Stats copy() {
            final Stats s = new Stats();
            s.requests = requests;
            s.toolCalls = toolCalls;
            s.peakInputTokens = peakInputTokens;
            s.totalOutputTokens = totalOutputTokens;
            s.toolCallAsText = toolCallAsText;
            s.cutOff = cutOff;
            return s;
        }

        @Override
        public void onResponse(ChatModelResponseContext context) {
            requests++;
            if (context.chatResponse().finishReason() == FinishReason.LENGTH) cutOff = true;

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

        if (r.stats().cutOff) warnings.add("output hit num-predict - answer cut off");
        return warnings;
    }

    static String summary(Result r, Properties config) {
        final Stats s = r.stats();
        final List<String> warnings = warnings(r, config);
        return String.format("time %.1fs | requests %d | readFile %d | peak prompt %d tok | output %d tok%s",
                r.millis() / 1000.0, s.requests, s.toolCalls, s.peakInputTokens, s.totalOutputTokens,
                warnings.isEmpty() ? "" : " | WARN: " + String.join(", ", warnings));
    }

    static Path writeReport(Path samplesDir, List<Result> results, Properties config,
                            BenchCompetenceIndex.Loaded catalogue) throws IOException {
        final StringBuilder md = new StringBuilder();
        md.append("# Describer bench ").append(LocalDateTime.now().withNano(0)).append("\n\n");

        md.append("Config:\n\n");
        config.stringPropertyNames().stream()
                .filter(k -> k.startsWith(PREFIX))
                .filter(k -> !k.endsWith("model-name") && !k.contains(".log-"))
                .sorted()
                .forEach(k -> md.append("- ").append(k.substring(PREFIX.length()))
                        .append(" = ").append(config.getProperty(k)).append('\n'));
        md.append("- competence search = ").append(catalogue == null
                ? "off"
                : catalogue.competences().size() + " competences, top " + Integer.getInteger("bench.topK", 20)
                        + " per query").append('\n');

        // grouped by sample, so models can be compared side by side
        final Map<String, List<Result>> bySample = new LinkedHashMap<>();
        for (Result r : results) bySample.computeIfAbsent(r.sample().name(), k -> new ArrayList<>()).add(r);

        for (var entry : bySample.entrySet()) {
            md.append("\n## ").append(entry.getKey()).append("\n");
            for (Result r : entry.getValue()) {
                md.append("\n### ").append(r.model()).append(" (run ").append(r.run()).append(")\n\n")
                        .append(summary(r, config)).append("\n\n### Description\n\n```\n")
                        .append(r.assignmentDescription().strip()).append("\n```\n\n### Competencies\n\n```\n")
                        .append(r.assignmentComp().strip()).append("\n```\n\n### Artifact Description\n\n```\n")
                        .append(r.description().strip()).append("\n```\n");

                if (catalogue != null) {
                    md.append("\n").append(competenceSummary(r, catalogue.competences())).append("\n\n")
                            .append(competenceTable(r, catalogue.competences()));
                }
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
