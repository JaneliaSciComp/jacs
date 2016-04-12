package org.janelia.it.jacs.compute.wsrest.data;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponses;
import org.hibernate.Query;
import org.hibernate.Session;
import org.janelia.it.jacs.compute.util.HibernateSessionUtils;
import org.janelia.it.jacs.model.entity.json.JsonTaskEvent;
import org.janelia.it.jacs.model.entity.json.JsonTask;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by goinac on 9/2/15.
 */
@Path("/data")
@Api(value = "Janelia Workstation Domain Data")
public class TaskWebService {
    private static final Logger LOG = LoggerFactory.getLogger(TaskWebService.class);
    private static final int DEFAULT_MAX_LENGTH = 100;

    @GET
    @Path("/tasks")
    @ApiOperation(value = "Gets Task Objects for a User",
            notes = "")
    @ApiResponses(value = {

    })
    @Produces({
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML
    })
    public List<JsonTask> getAllTasks(@QueryParam("owner") String owner,
                                      @QueryParam("offset") Integer offset,
                                      @QueryParam("length") Integer length) {
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
            List<Task> taskResults = q.list();
            List<JsonTask> jsonTasks = new ArrayList<>();
            for (Task t : taskResults) {
                jsonTasks.add(new JsonTask(t));
            }
            return jsonTasks;
        } finally {
            HibernateSessionUtils.closeSession(dbSession);
        }
    }

    @GET
    @Path("/task")
    @ApiOperation(value = "Gets a task given it's Id",
            notes = "")
    @ApiResponses(value = {

    })
    @Produces({
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML
    })
    public JsonTask getTask(@QueryParam("owner") String owner,
                            @QueryParam("task-id") Long taskId) {
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
                return new JsonTask(t);
            } else {
                throw new IllegalArgumentException("Record not found");
            }
        } finally {
            HibernateSessionUtils.closeSession(dbSession);
        }
    }

    @GET
    @Path("/task/events")
    @ApiOperation(value = "Gets the ev",
            notes = "")
    @ApiResponses(value = {

    })
    @Produces({
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML
    })
    public List<JsonTaskEvent> getTaskEvents(@QueryParam("owner") String owner,
                                             @QueryParam("task-id") Long taskId) {
        Session dbSession = null;
        try {
            dbSession = HibernateSessionUtils.getSession();
            Query q = dbSession.createQuery("select t from org.janelia.it.jacs.model.tasks.Task t " +
                    "where t.owner = :task_owner " +
                    "and t.objectId = :id");
            q.setParameter("task_owner", owner);
            q.setParameter("id", taskId);
            Task t = (Task) q.uniqueResult();
            if (t != null) {
                List<JsonTaskEvent> taskEvents = new ArrayList<>();
                for (Event evt : t.getEvents()) {
                    taskEvents.add(new JsonTaskEvent(evt));
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
