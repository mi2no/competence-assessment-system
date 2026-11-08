package org.wildfly.examples.competence;

import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
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

    public record TaskDTO(
            String content
    ) {}

    public record DTO(
            Integer id, String name
    ) {}

    @POST
    @Path("/extract")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response extractCompetence(TaskDTO dto) {
        final String template = """
                Analyze the student artifact and identify the competencies demonstrated
                by the submitted work.
        
                EXISTING COMPETENCE CATALOGUE:
        
                %s
        
                STUDENT ARTIFACT:
        
                %s
        
                Return two separate collections:
        
                1. matchedCompetences:
                   Competencies that can be accurately matched to entries in the
                   existing catalogue.
        
                2. proposedCompetences:
                   Competencies demonstrated by the artifact that do not have
                   sufficiently accurate equivalents in the existing catalogue.
        
                For matched competencies, return:
                - competenceId: the exact identifier from the catalogue;
                - evidence: a list of supporting evidence, each containing:
                    - quotation: an exact quotation from the artifact;
                    - justification: an explanation of why this quotation supports
                      the selected competence.
        
                For proposed competencies, return:
                - name: a concise competence name;
                - description: a clear explanation of the competence's meaning and scope;
                - evidence: a list of supporting evidence, each containing:
                    - quotation: an exact quotation from the artifact;
                    - justification: an explanation of why this quotation supports
                      the proposed competence.
        
                Do not create proposals for competencies that can already be matched
                to the existing catalogue.
        
                Do not force every observation into either collection. Only return
                competencies supported by sufficient evidence.
        """;

        try (Jsonb jsonb = JsonbBuilder.create()) {
            final List<String> competencies =
                    em.createQuery("SELECT c.name FROM Competence c", String.class).getResultList();

            final String message = template.formatted(jsonb.toJson(competencies), dto.content());
            competencies.clear();

            try {
                return Response.ok(aiService.chat(message))
                        .build();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error while chatting with assistant for question.", e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("My failure reason is:\n\n" + e.getMessage())
                        .build();
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
