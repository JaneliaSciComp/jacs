package org.janelia.it.jacs.model.entity.json;

import com.google.common.collect.Ordering;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by goinac on 10/23/15.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class JsonTask {

    private String taskId;
    private String taskName;
    private String parentTaskId;
    private String owner;
    private String taskStatusUrl;
    private String taskUrl;
    private String status;

    private List<JsonTaskEvent> taskEvents = new ArrayList<>();
    private Map<String, String> taskParameters = new LinkedHashMap<>();
    private List<JsonTaskData> inputNodes = new ArrayList<>();
    private List<JsonTaskData> outputNodes = new ArrayList<>();
    private List<JsonTask> childrenTasks = new ArrayList<>();

    JsonTask() {
        // needed by JAXB serializer
    }

    public JsonTask(Task t) {
        taskId = t.getObjectId().toString();
        taskName = t.getTaskName();
        parentTaskId = t.getParentTaskId() != null && t.getParentTaskId() != 0 ? t.getParentTaskId().toString() : null;
        owner = t.getOwner();
        Ordering<Event> taskEventsOrdering = new Ordering<Event>() {
            @Override
            public int compare(Event left, Event right) {
                return left.getTimestamp().compareTo(right.getTimestamp());
            }
        };
        for (Event e : taskEventsOrdering.immutableSortedCopy(t.getEvents())) {
            JsonTaskEvent jsonEvent = new JsonTaskEvent(e);
            taskEvents.add(jsonEvent);
            status = jsonEvent.getEventType();
        }
        for (TaskParameter p : t.getTaskParameterSet()) {
            taskParameters.put(p.getName(), p.getValue());
        }
        for (Node n : t.getInputNodes()) {
            if (n instanceof FileNode) {
                inputNodes.add(new JsonTaskData((FileNode) n));
            }
        }
        for (Node n : t.getOutputNodes()) {
            if (n instanceof FileNode) {
                outputNodes.add(new JsonTaskData((FileNode) n));
            }
        }
    }

    public String getTaskId() {
        return taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public String getParentTaskId() {
        return parentTaskId;
    }

    public String getOwner() {
        return owner;
    }

    public String getTaskStatusUrl() {
        return taskStatusUrl;
    }

    public void setTaskStatusUrl(String taskStatusUrl) {
        this.taskStatusUrl = taskStatusUrl;
    }

    public String getTaskUrl() {
        return taskUrl;
    }

    public void setTaskUrl(String taskUrl) {
        this.taskUrl = taskUrl;
    }

    public List<JsonTaskEvent> getTaskEvents() {
        return taskEvents;
    }

    public Map<String, String> getTaskParameters() {
        return taskParameters;
    }

    public List<JsonTaskData> getInputNodes() {
        return inputNodes;
    }

    public List<JsonTaskData> getOutputNodes() {
        return outputNodes;
    }

    public List<JsonTask> getChildrenTasks() {
        return childrenTasks;
    }

    public void setChildrenTasks(List<JsonTask> childrenTasks) {
        this.childrenTasks = childrenTasks;
    }

    public void addChildTask(JsonTask childTask) {
        childrenTasks.add(childTask);
    }
}
