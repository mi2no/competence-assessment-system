package org.wildfly.examples.artifact;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Path("/api/artifact")
public class ArtifactEndpoint {
    @PersistenceContext(name = "getting-started-PU")
    EntityManager em;

    @Inject
    ArtifactStorage storage;

    @GET
    @Path("/queue")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getQueue() {
        final List<ArtifactQueue> queue =
                em.createQuery("SELECT q FROM ArtifactQueue q WHERE q.userId = :id", ArtifactQueue.class)
                        .setParameter("id", 1).getResultList();

        return Response.ok().entity(queue).build();
    }

    @GET
    @Path("/structure")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStructure(@QueryParam("uuid") UUID artifactId) {
        final List<ArtifactStorage.FileInfo> structure = storage.getStructure(artifactId);

        return structure == null ?
                Response.status(Response.Status.NOT_FOUND).build()
                : Response.ok().entity(structure).build();
    }
}
