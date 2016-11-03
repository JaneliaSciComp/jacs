package org.janelia.it.jacs.compute.access;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.search.SearchTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.search.SearchResultNode;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Oct 26, 2007
 * Time: 2:24:24 PM
 */
public class ComputeBaseDAO extends AbstractBaseDAO {
    
    public static final int STATUS_TYPE = 0;
    private static final int STATUS_DESCRIPTION = 1;

    @Inject
    protected Logger log;

    @PersistenceContext(unitName = "FlyPortal_pu")
    protected EntityManager entityManager;


    public void recordProcessSuccess(String processDefName, Long processId) {
        if (log.isTraceEnabled()) {
            log.trace("recordProcessSuccess(processDef="+processDefName+", processId="+processId+")");    
        }
        try {
            if (log.isInfoEnabled()) {
                log.info("Process: " + processDefName + " Id:" + processId + " completed successfully");
            }
            updateTaskStatus(processId, Event.COMPLETED_EVENT, "Process " + processId + " completed successfully");
        }
        catch (Exception e) {
            log.error("Caught exception updating status of process: " + processDefName, e);
        }
    }

    public SearchResultNode getSearchTaskResultNode(long searchTaskId) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("getSearchTaskResultNode(searchTaskId="+searchTaskId+")");    
        }
        
        SearchTask st = entityManager.find(SearchTask.class, searchTaskId);
        return st.getSearchResultNode();
    }

    public Node getNodeById(long nodeId) {
        if (log.isTraceEnabled()) {
            log.trace("getNodeById(nodeId="+nodeId+")");    
        }
        
        return entityManager.find(Node.class, nodeId);
    }

    /**
     * This method returns the first result node for a given task.
     * ie if you know the task this will return the node result of that task
     *
     * @param taskId - task you're interested in
     * @return FIRST result node of the task
     */
    public Node getResultNodeByTaskId(long taskId) {
        if (log.isTraceEnabled()) {
            log.trace("getResultNodeByTaskId(taskId="+taskId+")");    
        }
        
        Query query = session.createSQLQuery("select node_id from task t, node n where t.task_id=" + taskId + " and t.task_id=n.task_id");
        List results = query.getResultList();
        // If no result node exists for the task, return null
        if (null == results || 0 >= results.size()) {
            return null;
        }
        long result = ((BigInteger) results.get(0)).longValue();
        return getNodeById(result);
    }

    /*
     * Variation of above but returns all nodes associated with task
     */
    public List<Node> getResultNodesByTaskId(long taskId) {
        if (log.isTraceEnabled()) {
            log.trace("getResultNodesByTaskId(taskId="+taskId+")");    
        }
        
        TypedQuery<Node> query = entityManager.createQuery("select n from Node where n.task.objectId = :taskId", Node.class);
        return query.getResultList();
    }

    // An array with 2 members is expected back.
    // First member string is the event type.
    // Second member string is the description.
    public String[] getTaskStatus(long taskId) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("getTaskStatus(taskId="+taskId+")");    
        }
        
        try {
            Task task = getTaskById(taskId);
            String[] taskStatusArr = new String[2];
            if (null != task) {
                Event lastEvent = task.getLastEvent();
                if (lastEvent != null) {
                	taskStatusArr[STATUS_TYPE] = lastEvent.getEventType();
                	taskStatusArr[STATUS_DESCRIPTION] = lastEvent.getDescription();
                }
                else {
                    taskStatusArr[STATUS_TYPE] = "Null";
                    taskStatusArr[STATUS_DESCRIPTION] = "No events found for task "+taskId;
                }
            }
            else {
                taskStatusArr[STATUS_TYPE] = "Not Applicable";
                taskStatusArr[STATUS_DESCRIPTION] = "Task id " + taskId + " was not found in the database.";
            }
            return taskStatusArr;
        } catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public Event createEvent(Long taskId, String eventType, String description, Date timestamp) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("createEvent(taskId="+taskId+", eventType="+eventType+", description="+description+", timestamp="+timestamp+")");    
        }
        
        try {
            Task task = getTaskById(taskId);
            return createEvent(task, eventType, description, timestamp);
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public Event createEvent(Task task, String eventType, String description, Date timestamp) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("createEvent(task="+task+", eventType="+eventType+", description="+description+", timestamp="+timestamp+")");    
        }
        
        try {
            Event event = new Event(description, timestamp, eventType);
            task.addEvent(event);
            entityManager.persist(event);
            return event;
        } catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public Task getTaskForNode(long nodeId) {
        if (log.isTraceEnabled()) {
            log.trace("getTaskForNode(nodeId="+nodeId+")");    
        }
        Node tmpNode = getNodeById(nodeId);
        return tmpNode.getTask();
    }

    /**
     * Create a default InitialContext
     *
     * @return InitialContext
     * @throws javax.naming.NamingException naming exception
     */
    private static InitialContext createInitialContext() throws NamingException {
        return new InitialContext();
    }

    protected DaoException handleException(Exception e, String actionWhichProducedError) {
        log.error(e);
        return new DaoException(e, actionWhichProducedError);
    }

    /*
     * Returns all child tasks starting with parent task id
     */
    public List<Task> getChildTasksByParentTaskId(long taskId) {
        if (log.isTraceEnabled()) {
            log.trace("getChildTasksByParentTaskId(taskId="+taskId+")");    
        }
        TypedQuery<Task> query = entityManager.createQuery("select t from Task t where t.parentTaskId = :taskId order by t.objectId", Task.class);
        query.setParameter("taskId", taskId);
        return query.getResultList();
    }
}
