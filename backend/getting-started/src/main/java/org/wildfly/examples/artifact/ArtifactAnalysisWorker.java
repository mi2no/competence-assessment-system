package org.wildfly.examples.artifact;

import dev.langchain4j.invocation.InvocationParameters;
import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import org.hibernate.Hibernate;
import org.wildfly.examples.competence.CompetenceRepository;
import org.wildfly.examples.task.Task;
import org.wildfly.examples.task.TaskEndpoint;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@ApplicationScoped
public class ArtifactAnalysisWorker {
    @Inject
    ArtifactQueueRepository repo;

    @Inject
    CompetenceRepository competenceRepo;

    @Inject
    ArtifactAnalyzer analyzer;

    @Resource
    ManagedExecutorService executor;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile boolean rerun;
    @Inject
    private ArtifactStorage artifactStorage;

    void onStartup(@Observes @Initialized(ApplicationScoped.class) Object ignored) {
        repo.resetInProgress();
        trigger();
    }

    void onQueued(@Observes(during = TransactionPhase.AFTER_SUCCESS) TaskEndpoint.QueuedArtifact artifact) {
        System.out.println("Queued noticed!");
        trigger();
    }

    public void trigger() {
        rerun = true;
        if (running.compareAndSet(false, true)) {
            executor.submit(this::drain);
        }
    }

    private void drain() {
        try {
            do {
                rerun = false;
                Artifact item;
                while ((item = repo.claimNext()) != null) {
                    try {
                        analyze(item);
                        repo.finish(item.id);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        repo.fail(item.id);
                    }
                }
            } while (rerun);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            running.set(false);
            if (rerun) trigger();
        }
    }

    private void analyze(Artifact item) {
        final Task task = item.getTask();
        final List<ArtifactStorage.FileInfo> files = artifactStorage.getStructure(item.uuid);

        final StringBuilder catalogue = new StringBuilder();
        for (var c : competenceRepo.getAll()) catalogue.append(c.name).append(":").append(c.description).append('\n');

        final StringBuilder structure = new StringBuilder();
        for (var file : files) structure.append(file.name()).append('\n');

        final String prompt = ArtifactAnalyzer.USER_PROMPT.formatted(catalogue.toString(), task.getDescription(), structure.toString());

        var result = analyzer.analyze(prompt, InvocationParameters.from(Map.of("uuid", item.uuid)));

        if (result == null) System.out.println("Result is null");
        else {
            System.out.println("MATCHED:");
            if (result.matchedCompetencies() != null) {
                for (var matched : result.matchedCompetencies())
                    System.out.println("[" + matched.name() + ", " + matched.justification() + "]");
            } else System.out.println("null");

            System.out.println("UNMATCHED:");
            if (result.unmatchedCompetencies() != null) {
                for (var unmatched : result.unmatchedCompetencies())
                    System.out.println("[" + unmatched.name() + ", " + unmatched.justification() + "]");
            } else System.out.println("null");
        }
    }
}
