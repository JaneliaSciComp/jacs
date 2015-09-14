
package org.janelia.it.jacs.model.tasks;
// Generated Aug 17, 2006 3:17:24 PM by Hibernate Tools 3.2.0.beta6a

import com.google.gwt.user.client.rpc.IsSerializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType
public class TaskParameter implements Serializable, IsSerializable {

    // Fields
    private Task task;
    private String name;
    private String value;

    // Constructors

    /**
     * default constructor
     */
    public TaskParameter() {
    }

    /**
     * full constructor
     */
    public TaskParameter(String name, String value, Task task) {
        this.name = name;
        this.value = value;
        this.task = task;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof TaskParameter)) return false;

        TaskParameter that = (TaskParameter) o;

        return !(task != null ? !task.equals(that.task) : that.task != null) &&
               !(name != null ? !name.equals(that.name) : that.name != null);
    }

    public int hashCode() {
        int result;
        result = (task != null && task.getObjectId() != null ? task.getObjectId().hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    public String toString() {
        return "TaskParameter{" +
                "task='" + (task != null && task.getObjectId() != null ? task.getObjectId().toString() : "<none>") + '\'' + "," +
                "name='" + name + '\'' + "," +
                "value='" + value +
                '}';
    }

}
