package org.janelia.it.jacs.compute.wsrest.dataresources;

import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.Session;
import org.janelia.it.jacs.compute.util.HibernateSessionUtils;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.LinkedList;
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
    public List<Task> getAllTasks(@PathParam("owner") String owner,
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
    public Task getTask(@PathParam("owner") String owner, @PathParam("task-id") Long taskId) {
        Session dbSession = null;
        try {
            dbSession = HibernateSessionUtils.getSession();
            Query q = dbSession.createQuery("select t from Task t " +
                    "where t.owner = :task_owner " +
                    "and t.objectId = :id");
            q.setParameter("task_owner", owner);
            q.setParameter("id", taskId);
            Task t = (Task) q.uniqueResult();
            if (t != null) {
                Hibernate.initialize(t);
                return t;
            } else {
                throw new IllegalArgumentException("Record not found");
            }
        } finally {
            HibernateSessionUtils.closeSession(dbSession);
        }
    }

    @GET
    @Path("/{owner}/tasks/{task-id}/events")
    @Produces({
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML
    })
    public List<Event> getTaskEvents(@PathParam("owner") String owner, @PathParam("task-id") Long taskId) {
        Session dbSession = null;
        try {
            dbSession = HibernateSessionUtils.getSession();
            Query q = dbSession.createQuery("select t from Task t " +
                    "where t.owner = :task_owner " +
                    "and t.objectId = :id");
            q.setParameter("task_owner", owner);
            q.setParameter("id", taskId);
            Task t = (Task) q.uniqueResult();
            if (t != null) {
                List<Event> taskEvents = new ArrayList<>();
                for (Event e : t.getEvents()) {
                    taskEvents.add(e);
                }
                return taskEvents;
            } else {
                throw new IllegalArgumentException("Record not found");
            }
        } finally {
            HibernateSessionUtils.closeSession(dbSession);
        }
    }

}
