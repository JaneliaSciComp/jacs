
package org.janelia.it.jacs.model.tasks;
// Generated Aug 17, 2006 3:17:24 PM by Hibernate Tools 3.2.0.beta6a

import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

import java.io.Serializable;
import java.lang.String;
import java.util.*;

import javax.xml.bind.annotation.*;

/**
 * Task generated by hbm2java
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@XmlType
public abstract class Task implements Serializable, IsSerializable {

    transient public static final String PARAM_project = "project";
	
    // The parameter below copies all the results to a location of the user's choosing
    // Could this be a gridFtp dir someday? Maybe we allow that to happen by default?
    transient public static final String PARAM_finalOutputDirectory = "finalOutputDirectory";

    // Fields
	@XmlAttribute(name="guid")
    private Long objectId;

	@XmlAttribute(name="parentTaskGuid")
    private Long parentTaskId;

	@XmlAttribute
    protected String taskName;

	@XmlAttribute
    private String jobName = "";

	@XmlAttribute
    private boolean taskDeleted;

	@XmlTransient
    private Set<Node> inputNodes = new HashSet<Node>();

    @XmlElementWrapper(name = "outNodes")
    @XmlElements(
        @XmlElement(name = "outputFileNode", type = FileNode.class)
    )
    private Set<Node> outputNodes = new HashSet<Node>();

	@XmlAttribute
    private String owner = "";

	@XmlTransient
    private List<Event> events = new ArrayList<Event>();

    @XmlTransient
    private Set<TaskParameter> taskParameterSet = new HashSet<TaskParameter>();

	@XmlTransient
    private Set<TaskMessage> messages = new HashSet<TaskMessage>();

    @XmlElement
    private String taskNote;

    @XmlTransient
    private Date expirationDate;

    // Constructors

    /**
     * default constructor
     */
    public Task() {
        Event event = new Event();
        event.setEventType(Event.CREATED_EVENT);
        event.setTimestamp(new Date());
        addEvent(event);
        this.taskDeleted = false;
    }

    /**
     * full constructor
     */
    public Task(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet) {
        this();
        if (inputNodes != null) {
            this.inputNodes = inputNodes;
        }
        setOwner(owner);
        if (taskParameterSet != null) {
            for (TaskParameter aTaskParameter : taskParameterSet) {
                addParameter(aTaskParameter);
            }
        }
        if (events != null) {
            for (Event event : events) {
                addEvent(event);
            }
        }
    }

    // Property accessors
    public Long getObjectId() {
        return this.objectId;
    }

    public Set<Node> getInputNodes() {
        return this.inputNodes;
    }

    public void setInputNodes(Set<Node> inputNodes) {
        this.inputNodes = inputNodes;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
    	this.owner = owner.contains(":") ? owner.split(":")[1] : owner;
    }

    public List<Event> getEvents() {
        return this.events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }

    public Set<TaskParameter> getTaskParameterSet() {
        return this.taskParameterSet;
    }

    // needed for Hibernate
    public void setTaskParameterSet(Set<TaskParameter> taskParameterSet) {
        this.taskParameterSet = taskParameterSet;
    }

    // todo This needs to be abstract to enfore assignment by the subclasses.
    public String getTaskName() {
        return taskName;
    }

    protected void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public boolean isTaskDeleted() {
        return taskDeleted;
    }

    public void setTaskDeleted(boolean taskDeleted) {
        this.taskDeleted = taskDeleted;
    }

    public String getParameter(String key) {
        TaskParameter resParam = getTaskParameter(key);
        if (resParam != null)
            return resParam.getValue();
        else
            return null;
    }

    public Boolean getParameterAsBoolean(String key) {
        String value = getParameter(key);
        if (value != null) {
            return Boolean.valueOf(value);
        } else {
            return null;
        }
    }

    public Integer getParameterAsInteger(String key) {
        String value = getParameter(key);
        if (value != null && value.trim().length() > 0) {
            return Integer.valueOf(value);
        } else {
            return null;
        }
    }

    public Long getParameterAsLong(String key) {
        String value = getParameter(key);
        if (value != null && value.trim().length() > 0) {
            return Long.valueOf(value);
        } else {
            return null;
        }
    }

    public Double getParameterAsDouble(String key) {
        String value = getParameter(key);
        if (value != null && value.trim().length() > 0) {
            return Double.valueOf(value);
        } else {
            return null;
        }
    }

    public TaskParameter getTaskParameter(String key) {
        TaskParameter resParam = null;
        for (Object aParameterStringMap : taskParameterSet) {
            TaskParameter taskParameter = (TaskParameter) aParameterStringMap;
            if (taskParameter.getName().equals(key)) {
                resParam = taskParameter;
                break;
            }
        }
        return resParam;
    }

    public void addParameter(TaskParameter taskParam) {
        TaskParameter existingParam = getTaskParameter(taskParam.getName());
        if (existingParam == null) {
            taskParam.setTask(this);
            this.taskParameterSet.add(taskParam); // add the new one
        } else {
            existingParam.setValue(taskParam.getValue());
        }
    }

    public void setParameter(String key, String value) {
        TaskParameter existingParam = getTaskParameter(key);
        if (existingParam == null) {
            this.taskParameterSet.add(new TaskParameter(key, value, this)); // add the new one
        } else {
            existingParam.setValue(value);
        }
    }

    public void setParameterAsBoolean(String key, Boolean value) {
        if (value != null) {
            setParameter(key, value.toString());
        }
    }

    public void setParameterAsInteger(String key, Integer value) {
        if (value != null) {
            setParameter(key, value.toString());
        }
    }

    public void setParameterAsLong(String key, Long value) {
        if (value != null) {
            setParameter(key, value.toString());
        }
    }

    public void setParameterAsDouble(String key, Double value) {
        if (value != null) {
            setParameter(key, value.toString());
        }
    }

    public abstract ParameterVO getParameterVO(String key) throws ParameterException;

    // This method should be called by subclasses as super.validate() within validate
    public void validate() throws ParameterException {
        for (TaskParameter aParameterStringMap : taskParameterSet) {
            ParameterVO pv = getParameterVO(aParameterStringMap.getName());
            if (pv == null) {
                throw new ParameterException("Could not find parameter value for key " + aParameterStringMap.getName());
            }
        }
        if (owner == null) {
            throw new ParameterException("Invalid user login");
        }
    }

    public Set<String> getParameterKeySet() {
        Set<String> parameterNames = new HashSet<String>();
        for (TaskParameter aParameterStringMap : taskParameterSet) {
            parameterNames.add(aParameterStringMap.getName());
        }
        return parameterNames;
    }

    public abstract String getDisplayName();

    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("Task{objectId=").append(objectId != null ? objectId.toString() : "null").append(", taskName=").append(taskName);
        // TODO: The following breaks if the session is closed since these properties are lazy
        buf.append(", owner=").append(this.getOwner());
        return buf.toString();
    }

    /**
     * Determines if the last event is Event.COMPLETED_EVENT or Event.ERROR_EVENT
     *
     * @return true if the task is complete; false otherwise
     */
    public boolean isDone() {
        Event lastEvent = getLastEvent();
        return lastEvent != null && isDone(lastEvent.getEventType());
    }

    /**
     * Determines if the given event is Event.COMPLETED_EVENT or Event.ERROR_EVENT
     *
     * @param event checking if event passed is terminal
     * @return boolean as to processing done or not
     */
    public static boolean isDone(String event) {
        return event.equals(Event.COMPLETED_EVENT) || event.equals(Event.ERROR_EVENT) || event.equals(Event.CANCELED_EVENT);
    }

    /**
     * Adds a new event to the event list
     *
     * @param e event
     */
    public void addEvent(Event e) {
        e.setTask(this);
        e.setEventIndex(events.size());
        events.add(e);
    }

    public Event getFirstEvent() {
        return events != null && events.size() > 0 ?
                events.get(0) :
                null;
    }

    public Event getLastEvent() {
        return events != null && events.size() > 0 ?
                events.get(events.size() - 1) :
                null;
    }

    public Event getLastNonDeletedEvent() {
        Event lastEvent = getLastEvent();
        if (lastEvent != null) {
            if (lastEvent.getEventType().equals(Event.DELETED_EVENT) && events.size() > 1) {
                lastEvent = events.get(events.size() - 2);
            }
        }

        return lastEvent;
    }

    public Set<Node> getOutputNodes() {
        return this.outputNodes;
    }

    public void setOutputNodes(Set<Node> outputNodes) {
        this.outputNodes = outputNodes;
    }

    public void addOutputNode(Node outputNode) {
        this.outputNodes.add(outputNode);
    }

    public Set<TaskMessage> getMessages() {
        return messages;
    }

    public void setMessages(Set<TaskMessage> messages) {
        this.messages = messages;
    }

    public void addMessage(String message) {
        this.messages.add(new TaskMessage(this, message));
    }

    public static List<String> listOfStringsFromCsvString(String listString) {
        if (null==listString||"".equals(listString)) {
            return new ArrayList<String>();
        }
        String[] listArr = listString.split(",");
        ArrayList<String> list = new ArrayList<String>();
        for (String aListArr : listArr) {
            list.add(aListArr.trim());
        }
        return list;
    }

    public static String csvStringFromCollection(Collection<?> collection) {
        StringBuilder sb = new StringBuilder("");
        if (collection != null) {
            Iterator iterator = collection.iterator();
            while (iterator.hasNext()) {
                Object next = iterator.next();
                sb.append(next);
                if (iterator.hasNext()) { sb.append(","); }
            }
        }
        return sb.toString();
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    public void setObjectId(Long objectId) {
        this.objectId = objectId;
    }

    public Long getParentTaskId() {
        return parentTaskId;
    }

    public void setParentTaskId(Long parentTaskId) {
        this.parentTaskId = parentTaskId;
    }

    public boolean isParameterRequired(String parameterKeyName) {
        return !(null == parameterKeyName || "".equals(parameterKeyName));
    }

    public String getFinalOutputDirectory() {
        return getParameter(PARAM_finalOutputDirectory);
    }


    protected void addRequiredCommandParameter(StringBuffer sb, String prefix, String parameter) throws ParameterException {
        if (!addCommandParameter(sb, prefix, parameter)) {
            throw new ParameterException("Could not find required parameter=" + parameter);
        }
    }

    protected boolean addCommandParameter(StringBuffer sb, String prefix, String parameter) {
        if (parameterDefined(parameter)) {
            sb.append(" ").append(prefix).append(" ").append(getParameter(parameter));
            return true;
        }
        else {
            return false;
        }
    }

    protected void addRequiredCommandParameterQuoted(StringBuffer sb, String prefix, String parameter, String quote) throws ParameterException {
        if (!addCommandParameterQuoted(sb, prefix, parameter, quote)) {
            throw new ParameterException("Could not find required parameter=" + parameter);
        }
    }

    protected boolean addCommandParameterQuoted(StringBuffer sb, String prefix, String parameter, String quote) {
        if (parameterDefined(parameter)) {
            sb.append(" ").append(prefix).append(" ").append(quote).append(getParameter(parameter)).append(quote);
            return true;
        }
        else {
            return false;
        }
    }

    protected void addRequiredCommandParameterEquals(StringBuffer sb, String prefix, String parameter) throws ParameterException {
        if (!addCommandParameterEquals(sb, prefix, parameter)) {
            throw new ParameterException("Could not find required parameter=" + parameter);
        }
    }

    protected boolean addCommandParameterEquals(StringBuffer sb, String prefix, String parameter) {
        if (parameterDefined(parameter)) {
            sb.append(" ").append(prefix).append("=").append(getParameter(parameter));
            return true;
        }
        else {
            return false;
        }
    }

    protected boolean addCommandParameterFlag(StringBuffer sb, String flag, String parameter) {
        if (parameterDefined(parameter)) {
            sb.append(" ").append(flag);
            return true;
        }
        else {
            return false;
        }
    }

    protected void addRequiredCommandParameterValue(StringBuffer sb, String parameter) throws ParameterException {
        if (!addCommandParameterValue(sb, parameter)) {
            throw new ParameterException("Could not find requried value=" + parameter);
        }
    }

    protected boolean addCommandParameterValue(StringBuffer sb, String parameter) {
        if (parameterDefined(parameter)) {
            sb.append(" ").append(getParameter(parameter));
            return true;
        }
        else {
            return false;
        }
    }

    public boolean parameterDefined(String parameter) {
        String value = getParameter(parameter);
        return value != null && !value.trim().equals("");
    }

    public String getTaskNote() {
        return this.taskNote;
    }

    public void setTaskNote(String taskNote) {
        this.taskNote = taskNote;
    }

}
