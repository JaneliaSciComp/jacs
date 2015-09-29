
package org.janelia.it.jacs.model.tasks;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: tnabeel
 * Date: May 22, 2007
 * Time: 11:30:18 AM
 */
@XmlAccessorType(XmlAccessType.NONE)
public class TaskMessage implements Serializable {

    private Long messageId;

    private String message;

    private Task task;

    public TaskMessage() {
    }

    public TaskMessage(Task task, String message) {
        this.task = task;
        this.message = message;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public String toString() {
        return message;
    }
}
