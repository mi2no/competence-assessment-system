package org.wildfly.examples.competence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.ws.rs.*;
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
        final List<SimpleDTO> result = em.createQuery("SELECT new SimpleDTO(c.id, c.name, c.definition) FROM Competence c", SimpleDTO.class).getResultList();

        return Response.ok().entity(result).build();
    }

    @GET
    @Path("/get/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCompetenceById(@PathParam("id") Integer id) {
        final Competence competence =
                em.createQuery("SELECT c FROM Competence c WHERE c.id = ?1", Competence.class)
                        .setParameter(1, id).getSingleResultOrNull();

        return competence == null ?
                Response.status(Response.Status.NOT_FOUND).build() : Response.ok().entity(competence).build();
    }
}
