package org.janelia.it.jacs.model.status;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskMessage;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Simple representation of a task's current (latest) status information.
 *
 * @author Eric Trautman
 */
@XmlRootElement
@XmlType(name="", propOrder={"taskId", "lastEventType", "lastEventDescription", "lastEventTimestamp", "href", "messages"})
public class CurrentTaskStatus {

    private Long taskId;
    private String lastEventType;
    private String lastEventDescription;
    private String lastEventTimestamp;
    private String href;
    private List<String> messages;

    @SuppressWarnings("UnusedDeclaration")
    public CurrentTaskStatus() {
    }

    public CurrentTaskStatus(Task task) {
        this.taskId = task.getObjectId();
        final Event lastEvent = task.getLastEvent();
        if (lastEvent != null) {
            this.lastEventType = lastEvent.getEventType();
            this.lastEventDescription = lastEvent.getDescription();
            final SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            this.lastEventTimestamp = sdf.format(lastEvent.getTimestamp());
        }
        final Set<TaskMessage> taskMessages = task.getMessages();
        if ((taskMessages != null) && (taskMessages.size() > 0)) {
            this.messages = new ArrayList<String>(taskMessages.size());
            for (TaskMessage taskMessage : taskMessages) {
                this.messages.add(taskMessage.getMessage());
            }
        }
    }

    @XmlElement
    public Long getTaskId() {
        return taskId;
    }

    @SuppressWarnings("UnusedDeclaration")
    @XmlElement
    public String getLastEventType() {
        return lastEventType;
    }

    @SuppressWarnings("UnusedDeclaration")
    @XmlElement
    public String getLastEventDescription() {
        return lastEventDescription;
    }

    @SuppressWarnings("UnusedDeclaration")
    @XmlElement
    public String getLastEventTimestamp() {
        return lastEventTimestamp;
    }

    @XmlElement
    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    @XmlElement(name = "message")
    public List<String> getMessages() {
        return messages;
    }

    @Override
    public String toString() {
        return "CurrentTaskStatus{taskId=" + taskId +
                ", lastEventType='" + lastEventType + '\'' +
                ", lastEventDescription='" + lastEventDescription + '\'' +
                ", lastEventTimestamp=" + lastEventTimestamp +
                ", href='" + href + '\'' +
                ", messages=" + messages +
                '}';
    }
}
