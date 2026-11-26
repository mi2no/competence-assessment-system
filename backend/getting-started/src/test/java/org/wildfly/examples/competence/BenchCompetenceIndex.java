package org.wildfly.examples.competence;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

/**
 * Competence catalogue and embedding index for benchmarks, without WildFly.
 *
 * <p>Competences are read from MariaDB with plain JDBC. Connection settings come from
 * {@code config/.env} (path can be changed with {@code -Dbench.env=...}); environment
 * variables with the same names (MARIADB_HOST, MARIADB_PORT, MARIADB_DATABASE, MARIADB_USER,
 * MARIADB_PASSWORD) take precedence.
 *
 * <p>The index is built by the real {@link CompetenceIndexer}, then cached as JSON. The cache
 * file name contains a hash of the catalogue and the embedding model, so any change to the
 * competences produces a new index automatically.
 */
public final class BenchCompetenceIndex {

    public record Loaded(Map<Integer, Competence> competences, CompetenceIndexer indexer) {}

    private BenchCompetenceIndex() {}

    public static Loaded load(Path cacheDir) throws Exception {
        final List<Competence> all = loadFromDb();
        if (all.isEmpty()) throw new IllegalStateException("The competence table is empty");

        final CompetenceIndexer indexer = new CompetenceIndexer();
        indexer.repository = new CompetenceRepository() {
            @Override
            public List<Competence> getAll() {
                return all;
            }
        };

        final Path cache = cacheDir.resolve("competence-index-" + hash(all, indexer.modelName) + ".json");
        if (Files.isRegularFile(cache)) {
            indexer.store = InMemoryEmbeddingStore.fromFile(cache);
            System.out.printf("Competence index: %d entries loaded from %s%n", all.size(), cache);
        } else {
            System.out.printf("Competence index: embedding %d competences with %s (only on the first run)...%n",
                    all.size(), indexer.modelName);
            indexer.rebuild();
            if (indexer.store == null) throw new IllegalStateException("Building the competence index failed, see the log above");

            Files.createDirectories(cacheDir);
            ((InMemoryEmbeddingStore<TextSegment>) indexer.store).serializeToFile(cache);
            System.out.println("Competence index cached in " + cache);
        }

        final Map<Integer, Competence> byId = new LinkedHashMap<>();
        for (Competence c : all) byId.put(c.id, c);
        return new Loaded(byId, indexer);
    }

    static List<Competence> loadFromDb() throws Exception {
        final Map<String, String> cfg = dbConfig();
        final String url = "jdbc:mariadb://%s:%s/%s".formatted(
                cfg.getOrDefault("MARIADB_HOST", "localhost"),
                cfg.getOrDefault("MARIADB_PORT", "3306"),
                require(cfg, "MARIADB_DATABASE"));

        final List<Competence> result = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(url,
                require(cfg, "MARIADB_USER"), cfg.getOrDefault("MARIADB_PASSWORD", ""));
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT id, concept_uri, name, skill_type, description FROM competence ORDER BY id")) {
            while (rs.next()) {
                result.add(new Competence(rs.getInt("id"), rs.getString("concept_uri"), rs.getString("name"),
                        rs.getString("skill_type"), rs.getString("description")));
            }
        }
        return result;
    }

    static Map<String, String> dbConfig() throws IOException {
        final Map<String, String> cfg = new HashMap<>();

        final Path envFile = Path.of(System.getProperty("bench.env", "config/.env"));
        if (Files.isRegularFile(envFile)) {
            for (String line : Files.readAllLines(envFile, StandardCharsets.UTF_8)) {
                line = line.strip();
                final int eq = line.indexOf('=');
                if (line.isEmpty() || line.startsWith("#") || eq < 0) continue;

                String value = line.substring(eq + 1).strip();
                if (value.length() >= 2 && (value.startsWith("\"") && value.endsWith("\"")
                        || value.startsWith("'") && value.endsWith("'")))
                    value = value.substring(1, value.length() - 1);
                cfg.put(line.substring(0, eq).strip(), value);
            }
        }

        System.getenv().forEach((k, v) -> {
            if (k.startsWith("MARIADB_")) cfg.put(k, v);
        });
        return cfg;
    }

    private static String require(Map<String, String> cfg, String key) {
        final String value = cfg.get(key);
        if (value == null || value.isBlank())
            throw new IllegalStateException(key + " is not set (config/.env or environment variable)");
        return value;
    }

    private static String hash(List<Competence> all, String model) throws Exception {
        final MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(model.getBytes(StandardCharsets.UTF_8));
        for (Competence c : all) {
            digest.update(("\n" + c.id + "\t" + c.name + ": " + Objects.toString(c.description, ""))
                    .getBytes(StandardCharsets.UTF_8));
        }
        return HexFormat.of().formatHex(digest.digest()).substring(0, 16);
    }
}
