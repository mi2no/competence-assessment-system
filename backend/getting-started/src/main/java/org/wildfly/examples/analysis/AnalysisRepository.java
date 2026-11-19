package org.wildfly.examples.analysis;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.wildfly.examples.artifact.Artifact;

import java.time.LocalDateTime;

@ApplicationScoped
public class AnalysisRepository {

    @PersistenceContext(name = "getting-started-PU")
    EntityManager em;

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public Analysis create(Artifact artifact, String description) {
        final Analysis analysis = new Analysis();
        analysis.setCreationDate(LocalDateTime.now());
        analysis.setArtifact(artifact);
        analysis.setDescription(description);

        em.persist(analysis);
        return analysis;
    }
}