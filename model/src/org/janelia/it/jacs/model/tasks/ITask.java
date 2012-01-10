
package org.janelia.it.jacs.model.tasks;

import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * User: aresnick
 * Date: Jun 23, 2009
 * Time: 12:18:59 PM
 * <p/>
 * <p/>
 * Description:
 */
public interface ITask {
    // Property accessors
    Long getObjectId();

    Set<Node> getInputNodes();

    void setInputNodes(Set<Node> inputNodes);

    String getOwner();

    void setOwner(String owner);

    List<Event> getEvents();

    void setEvents(List<Event> events);

    Set<TaskParameter> getTaskParameterSet();// needed for Hibernate

    void setTaskParameterSet(Set<TaskParameter> taskParameterSet);

    String getTaskName();

    String getJobName();

    void setJobName(String jobName);

    boolean isTaskDeleted();

    void setTaskDeleted(boolean taskDeleted);

    String getParameter(String key);

    TaskParameter getTaskParameter(String key);

    void addParameter(TaskParameter taskParam);

    void setParameter(String key, String value);

    ParameterVO getParameterVO(String key) throws ParameterException;// This method should be called by subclasses as super.validate() within validate

    void validate() throws ParameterException;

    Set<String> getParameterKeySet();

    String getDisplayName();

    boolean isDone();

    void addEvent(Event e);

    Event getFirstEvent();

    Event getLastEvent();

    Event getLastNonDeletedEvent();

    Set<Node> getOutputNodes();

    void setOutputNodes(Set<Node> outputNodes);

    void addOutputNode(Node outputNode);

    Set<TaskMessage> getMessages();

    void setMessages(Set<TaskMessage> messages);

    void addMessage(String message);

    Date getExpirationDate();

    void setExpirationDate(Date expirationDate);

    void setObjectId(Long objectId);

    Long getParentTaskId();

    void setParentTaskId(Long parentTaskId);

    boolean isParameterRequired(String parameterKeyName);
}
