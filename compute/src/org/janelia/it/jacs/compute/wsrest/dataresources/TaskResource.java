package org.janelia.it.jacs.compute.wsrest.dataresources;

import org.hibernate.Query;
import org.hibernate.Session;
import org.janelia.it.jacs.compute.util.HibernateSessionUtils;
import org.janelia.it.jacs.model.tasks.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * Created by goinac on 9/2/15.
 */
@Path("/")
public class TaskResource {
    private static final Logger LOG = LoggerFactory.getLogger(TaskResource.class);
    private static final int DEFAULT_MAX_LENGTH = 100;

    @GET
    @Path("/{owner}/tasks")
    @Produces({
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML
    })
    public List<Task> get(@PathParam("owner") String owner,
                          @QueryParam("offset") int offset,
                          @QueryParam("length") int length) {
        Session dbSession = null;
        try {
            dbSession = HibernateSessionUtils.getSession();
            Query q = dbSession.createQuery("select t " +
                    "from Task t " +
                    "where t.owner = :task_owner");
            q.setParameter("task_owner", owner);
            if (offset > 0) {
                q.setFirstResult(offset);
            }
            if (length > 0) {
                q.setMaxResults(length);
            } else {
                q.setMaxResults(DEFAULT_MAX_LENGTH);
            }
            return q.list();
        } finally {
            HibernateSessionUtils.closeSession(dbSession);
        }
    }

    @GET
    @Path("/{owner}/tasks/{task-id}")
    @Produces({
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML
    })
    public Task get(@PathParam("owner") String owner, @PathParam("task-id") Long taskId) {
        Session dbSession = null;
        try {
            dbSession = HibernateSessionUtils.getSession();
            Query q = dbSession.createQuery("select t from Task t " +
                    "where t.owner = :task_owner " +
                    "and t.objectId = :id");
            q.setParameter("task_owner", owner);
            q.setParameter("id", taskId);
            return (Task) q.uniqueResult();
        } finally {
            HibernateSessionUtils.closeSession(dbSession);
        }
    }

}
