package org.wildfly.examples.competence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;

@ApplicationScoped
public class CompetenceRepository {

    @PersistenceContext(name = "getting-started-PU")
    EntityManager em;

    public List<Competence> getAll() {
        return em.createQuery("SELECT c FROM Competence c", Competence.class).getResultList();
    }
}
