package org.wildfly.examples.competence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/api/competence")
public class CompetenceEndpoint {

    @PersistenceContext(name = "getting-started-PU")
    EntityManager em;

    public record SimpleDTO(
            Integer id, String name, String definition
    ) {}

    @GET
    @Path("/get")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCompetences() {
        List<SimpleDTO> result = em.createQuery("SELECT new SimpleDTO(c.id, c.name, c.definition) from Competence c", SimpleDTO.class).getResultList();

        return Response.ok().entity(result).build();
    }
}
