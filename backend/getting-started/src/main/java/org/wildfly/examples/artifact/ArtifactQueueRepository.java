package org.wildfly.examples.artifact;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.util.List;

@ApplicationScoped
public class ArtifactQueueRepository {

    @PersistenceContext(name = "getting-started-PU")
    EntityManager em;

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void resetInProgress() {          // sprzątanie po awarii/restarcie
        em.createQuery("UPDATE ArtifactQueue q SET q.state = :w WHERE q.state = :p")
                .setParameter("w", ArtifactQueue.State.WAITING)
                .setParameter("p", ArtifactQueue.State.IN_PROGRESS)
                .executeUpdate();
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public Artifact claimNext() {
        final List<ArtifactQueue> list = em.createQuery(
                        "SELECT q FROM ArtifactQueue q JOIN FETCH q.artifact WHERE q.state = :w ORDER BY q.id", ArtifactQueue.class)
                .setParameter("w", ArtifactQueue.State.WAITING)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .setMaxResults(1)
                .getResultList();

        if (list.isEmpty()) return null;

        final ArtifactQueue item = list.get(0);
        item.state = ArtifactQueue.State.IN_PROGRESS;

        return item.artifact;
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void finish(int id) {
        final ArtifactQueue queued = em.createQuery(
                "SELECT q FROM ArtifactQueue q WHERE q.artifact_id = :id", ArtifactQueue.class
        ).setParameter("id", id).getSingleResultOrNull();

        if (queued == null) throw new NotFoundException();

        queued.state = ArtifactQueue.State.DONE;
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void fail(int id) {
        final ArtifactQueue queued = em.createQuery(
                "SELECT q FROM ArtifactQueue q WHERE q.artifact_id = :id", ArtifactQueue.class
        ).setParameter("id", id).getSingleResultOrNull();

        if (queued == null) throw new NotFoundException();

        queued.state = ArtifactQueue.State.FAILED;
    }
}
