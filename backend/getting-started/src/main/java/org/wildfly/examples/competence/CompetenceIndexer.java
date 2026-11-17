package org.wildfly.examples.competence;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class CompetenceIndexer {
    private static final Logger LOG = Logger.getLogger(CompetenceIndexer.class.getName());
    private static final int BATCH_SIZE = 32;

    /*@Inject
    @ConfigProperty(name = "embedding-model.base-url")*/
    String baseUrl = "http://localhost:11434";

    /*@Inject
    @ConfigProperty(name = "embedding-model.model-name")*/
    String modelName = "bge-m3";

    OllamaEmbeddingModel embeddingModel = OllamaEmbeddingModel.builder()
            .baseUrl(baseUrl)
            .modelName(modelName)
            .timeout(Duration.ofMinutes(5))
            .build();

    @Inject
    CompetenceRepository repository;

    @Resource
    ManagedExecutorService executor;

    private volatile EmbeddingStore<TextSegment> store;

    void onStartup(@Observes @Initialized(ApplicationScoped.class) Object ignored) {
        executor.submit(this::rebuild);
    }

    public void rebuild() {
        try {
            final long start = System.currentTimeMillis();
            final List<Competence> all = repository.getAll();
            final InMemoryEmbeddingStore<TextSegment> newStore = new InMemoryEmbeddingStore<>();

            for (int i = 0; i < all.size(); i += BATCH_SIZE) {
                final List<Competence> batch = all.subList(i, Math.min(i + BATCH_SIZE, all.size()));

                final List<TextSegment> segments = batch.stream()
                        .map(c -> TextSegment.from(
                                c.name + ": " + Objects.toString(c.description, ""),
                                new Metadata().put("competenceId", c.id)))
                        .toList();
                final List<String> ids = batch.stream().map(c -> "competence-" + c.id).toList();
                final List<Embedding> embeddings = embeddingModel.embedAll(segments).content();

                newStore.addAll(ids, embeddings, segments);
            }

            store = newStore;
            LOG.info("Competence index built: " + all.size() + " entries in "
                    + (System.currentTimeMillis() - start) + " ms");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Building competence index failed", e);
        }
    }

    public record Match(int competenceId, String text, double score) {}

    public List<Match> search(String text, int maxResults) {
        final EmbeddingStore<TextSegment> current = store;
        if (current == null) throw new IllegalStateException("Competence index is not ready yet");

        final Embedding query = embeddingModel.embed(text).content();

        return current.search(EmbeddingSearchRequest.builder()
                        .queryEmbedding(query)
                        .maxResults(maxResults)
                        .build())
                .matches().stream()
                .map(m -> new Match(
                        m.embedded().metadata().getInteger("competenceId"),
                        m.embedded().text(),
                        m.score()))
                .toList();
    }
}
