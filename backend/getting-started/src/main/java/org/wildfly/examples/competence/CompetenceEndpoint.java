package org.wildfly.examples.competence;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.logging.Level;

import static dev.langchain4j.cdi.plugin.CommonLLMPluginCreator.LOGGER;

@Path("/api/competence")
public class CompetenceEndpoint {

    @PersistenceContext(name = "getting-started-PU")
    EntityManager em;

    public record SimpleDTO(
            Integer id, String name, String description
    ) {}

    @GET
    @Path("/get")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCompetences() {
        final List<SimpleDTO> result = em.createQuery("SELECT new SimpleDTO(c.id, c.name, c.description) FROM Competence c", SimpleDTO.class).getResultList();

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

    @Inject
    CompetenceAIService aiService;

    @GET
    @Path("/chat")
    @Produces(MediaType.TEXT_PLAIN)
    public Response chatWithAssistant(@QueryParam("question") String question) {
        try {
            return Response.ok(aiService.chat(question))
                    .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error while chatting with assistant for question: " + question, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("My failure reason is:\n\n" + e.getMessage())
                    .build();
        }
    }
}
