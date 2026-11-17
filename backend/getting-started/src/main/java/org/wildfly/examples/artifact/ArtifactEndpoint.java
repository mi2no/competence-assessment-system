package org.wildfly.examples.artifact;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
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
    @Path("/{id}/state")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getState(@PathParam("id") int id) {
        final ArtifactQueue entry = em.find(ArtifactQueue.class, id);

        return entry == null ?
                Response.status(Response.Status.NOT_FOUND).build()
                : Response.ok().entity(entry.state).build();
    }

    /*
    TODO: replace uuid's with just id's, since using uuid requires to check if the artifact belongs to the user anyway.
    Just use id and em.find(id)
     */

    @GET
    @Path("/structure")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStructure(@QueryParam("uuid") UUID artifactId) {
        final List<ArtifactStorage.FileInfo> structure = storage.getStructure(artifactId);

        return structure == null ?
                Response.status(Response.Status.NOT_FOUND).build()
                : Response.ok().entity(structure).build();
    }

    @GET
    @Path("/file")
    public Response file(@QueryParam("uuid") UUID artifactId, @QueryParam("path") String path) throws IOException {
        final ArtifactStorage.StoredFileData data = storage.getFile(artifactId, path);

        if (data == null) return Response.status(Response.Status.NOT_FOUND).build();

        StreamingOutput body = data.stream()::transferTo;

        String type = Files.probeContentType(java.nio.file.Path.of(path));   // może zwrócić null
        return Response.ok(body, type != null ? type : MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Length", data.size())
                .build();
    }

}
