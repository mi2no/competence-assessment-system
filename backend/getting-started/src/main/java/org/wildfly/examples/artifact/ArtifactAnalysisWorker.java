package org.wildfly.examples.artifact;

import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import org.wildfly.examples.task.TaskEndpoint;

import java.util.concurrent.atomic.AtomicBoolean;

@ApplicationScoped
public class ArtifactAnalysisWorker {
    @Inject
    ArtifactQueueRepository repo;

    @Resource
    ManagedExecutorService executor;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile boolean rerun;

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

    }
}
