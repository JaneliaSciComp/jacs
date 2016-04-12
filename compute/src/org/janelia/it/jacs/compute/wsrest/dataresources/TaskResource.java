package org.janelia.it.jacs.compute.wsrest.dataresources;

import org.hibernate.Session;
import org.janelia.it.jacs.compute.access.TaskDAO;
import org.janelia.it.jacs.compute.util.HibernateSessionUtils;
import org.janelia.it.jacs.model.entity.json.JsonTaskEvent;
import org.janelia.it.jacs.model.entity.json.JsonTask;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by goinac on 9/2/15.
 */
@Path("/")
public class TaskResource {
    private TaskDAO taskDAO = new TaskDAO();

    @GET
    @Path("/{owner}/tasks")
    @Produces({
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML
    })
    public List<JsonTask> getAllTasks(@PathParam("owner") String owner,
                                      @QueryParam("offset") int offset,
                                      @QueryParam("length") int length) {
        Session dbSession = null;
        try {
            dbSession = HibernateSessionUtils.getSession();
            List<Task> taskResults = taskDAO.getAllTasksByOwner(owner, offset, length, dbSession);
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
    @Path("/tasks/{task-id}")
    @Produces({
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML
    })
    public JsonTask getTask(@PathParam("task-id") Long taskId) {
        Session dbSession = null;
        try {
            dbSession = HibernateSessionUtils.getSession();
            Task t = taskDAO.getTaskById(taskId, dbSession);
            if (t != null) {
                JsonTask mainTask = new JsonTask(t);
                List<Task> childrenTasks = taskDAO.getChildrenTasksByParentTaskId(t.getObjectId(), dbSession);
                for (Task c : childrenTasks) {
                    mainTask.addChildTask(new JsonTask(c));
                }
                return mainTask;
            }
        } finally {
            HibernateSessionUtils.closeSession(dbSession);
        }
        throw new IllegalArgumentException("Record not found");
    }

    @GET
    @Path("/tasks/{task-id}/events")
    @Produces({
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML
    })
    public List<JsonTaskEvent> getTaskEvents(@PathParam("task-id") Long taskId) {
        Session dbSession = null;
        try {
            dbSession = HibernateSessionUtils.getSession();
            Task t = taskDAO.getTaskById(taskId, dbSession);
            if (t != null) {
                List<JsonTaskEvent> taskEvents = new ArrayList<>();
                for (Event evt : t.getEvents()) {
                    taskEvents.add(new JsonTaskEvent(evt));
                }
                return taskEvents;
            }
        } finally {
            HibernateSessionUtils.closeSession(dbSession);
        }
        throw new IllegalArgumentException("Record not found");
    }

}
