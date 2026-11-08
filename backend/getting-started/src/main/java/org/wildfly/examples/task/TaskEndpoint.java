package org.wildfly.examples.task;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import java.util.List;

@Path("/api/task")
public class TaskEndpoint {

    @PersistenceContext(name = "getting-started-PU")
    EntityManager em;

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

        if (files == null || files.isEmpty()) return Response.status(Response.Status.NO_CONTENT).build();

        for (InputPart file : files)
            System.out.println(file.getFileName());

        return Response.ok().build();
    }
}
