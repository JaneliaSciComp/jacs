package org.janelia.it.jacs.compute.wsrest.data;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponses;
import org.hibernate.Query;
import org.hibernate.Session;
import org.janelia.it.jacs.compute.access.TaskDAO;
import org.janelia.it.jacs.compute.util.HibernateSessionUtils;
import org.janelia.it.jacs.model.entity.json.JsonTaskEvent;
import org.janelia.it.jacs.model.entity.json.JsonTask;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
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
    private TaskDAO taskDAO = new TaskDAO();

    @GET
    @Path("/tasks")
    @ApiOperation(value = "Gets Task Objects for a User",
            notes = "")
    @ApiResponses(value = {

    })
    @Produces({
            MediaType.APPLICATION_XML,
            MediaType.APPLICATION_JSON
    })
    public List<JsonTask> getAllTasks(@QueryParam("owner") String owner,
                                      @QueryParam("offset") Integer offset,
                                      @QueryParam("length") Integer length,
                                      @Context UriInfo uriInfo) {
        Session dbSession = null;
        try {
            dbSession = HibernateSessionUtils.getSession();
            List<Task> taskResults = taskDAO.getAllTasksByOwner(owner, offset, length, dbSession);
            List<JsonTask> jsonTasks = new ArrayList<>();
            for (Task t : taskResults) {
                JsonTask jsonTask = new JsonTask(t);
                jsonTask.setTaskUrl(getTaskUrl(uriInfo, t.getObjectId()));
                jsonTask.setTaskStatusUrl(getTaskStatusUrl(uriInfo, t.getObjectId()));
                jsonTasks.add(jsonTask);
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
            MediaType.APPLICATION_XML,
            MediaType.APPLICATION_JSON
    })
    public JsonTask getTask(@QueryParam("owner") String owner,
                            @QueryParam("task-id") Long taskId, @Context UriInfo uriInfo) {
        Session dbSession = null;
        try {
            dbSession = HibernateSessionUtils.getSession();
            Task t = taskDAO.getTaskById(taskId, dbSession);
            if (t != null) {
                JsonTask mainTask = new JsonTask(t);
                mainTask.setTaskUrl(getTaskUrl(uriInfo, t.getObjectId()));
                mainTask.setTaskStatusUrl(getTaskStatusUrl(uriInfo, t.getObjectId()));
                List<Task> childrenTasks = taskDAO.getChildrenTasksByParentTaskId(t.getObjectId(), dbSession);
                for (Task c : childrenTasks) {
                    JsonTask jsonTask = new JsonTask(c);
                    jsonTask.setTaskUrl(getTaskUrl(uriInfo, c.getObjectId()));
                    jsonTask.setTaskStatusUrl(getTaskStatusUrl(uriInfo, c.getObjectId()));
                    mainTask.addChildTask(jsonTask);
                }
                return mainTask;
            }
        } finally {
            HibernateSessionUtils.closeSession(dbSession);
        }
        throw new IllegalArgumentException("Record not found");
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

    private String getNormalizedBaseUrlString(UriInfo uriInfo) {
        StringBuilder sb = new StringBuilder(uriInfo.getBaseUri().toString());
        if (sb.charAt(sb.length() - 1) != '/') {
            sb.append('/');
        }
        return sb.toString();
    }

    private String getTaskUrl(UriInfo uriInfo, Long taskId) {
        return getNormalizedBaseUrlString(uriInfo) + "tasks/" + taskId;
    }

    private String getTaskStatusUrl(UriInfo uriInfo, Long taskId) {
        return getNormalizedBaseUrlString(uriInfo) + "task/" + taskId + "/currentStatus";
    }
}
