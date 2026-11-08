package org.wildfly.examples.task;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.wildfly.examples.ArtifactStorage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Path("/api/task")
public class TaskEndpoint {

    @PersistenceContext(name = "getting-started-PU")
    EntityManager em;

    @Inject
    ArtifactStorage storage;

    @POST
    @Path("{id}/solution/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadSolution(@PathParam("id") int taskId, MultipartFormDataInput input) {
        /*{ // Check if task of given id exists
            final Integer id = em.createQuery("SELECT t.id FROM Task t WHERE t.id = :id", Integer.class)
                    .setParameter("id", taskId).getSingleResultOrNull();

            if (id == null) return Response.status(Response.Status.NOT_FOUND).build();
        }*/

        final List<InputPart> files = input.getFormDataMap().get("files");

        if (files == null || files.isEmpty()) return Response.status(Response.Status.BAD_REQUEST).build();

        for (InputPart file : files)
            System.out.println(file.getFileName());

        try {
            final List<ArtifactStorage.FileData> filesData = new ArrayList<>(files.size());

            for (var file : files) filesData.add(new ArtifactStorage.FileData(file.getFileName(), file.getBody()));

            storage.store(filesData);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return Response.ok().build();
    }
}
