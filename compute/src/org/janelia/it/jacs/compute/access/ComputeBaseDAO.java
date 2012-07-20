
package org.janelia.it.jacs.compute.access;

import org.apache.log4j.Logger;
import org.hibernate.*;
import org.hibernate.criterion.Expression;
import org.janelia.it.jacs.compute.engine.def.ProcessDef;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskMessage;
import org.janelia.it.jacs.model.tasks.search.SearchTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.search.SearchResultNode;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Oct 26, 2007
 * Time: 2:24:24 PM
 */
public class ComputeBaseDAO {
    public static final int STATUS_TYPE = 0;
    public static final int STATUS_DESCRIPTION = 1;

    protected Logger _logger;
    protected SessionFactory sessionFactory;
    protected Session externalSession;

    public Connection getJdbcConnection() throws DaoException {
    	try {
            String jdbcDriver=SystemConfigurationProperties.getString("jdbc.driverClassName");
            String jdbcUrl=SystemConfigurationProperties.getString("jdbc.url");
            String jdbcUser=SystemConfigurationProperties.getString("jdbc.username");
            String jdbcPw=SystemConfigurationProperties.getString("jdbc.password");
            Class.forName(jdbcDriver);
            Connection connection = DriverManager.getConnection(
                    jdbcUrl,
                    jdbcUser,
                    jdbcPw);
            connection.setAutoCommit(false);
//            _logger.debug("getJdbcConnection() using these parameters: driverClassName="+jdbcDriver+" url="+jdbcUrl+" user="+jdbcUser);
            return connection;
    	}
    	catch (Exception e) {
    		throw new DaoException(e);
    	}
    }

    public ComputeBaseDAO(Logger logger) {
        getSessionFactory();
        _logger = logger;
    }

    public ComputeBaseDAO(Session externalSession) {
        this.externalSession = externalSession;
    }

    private SessionFactory getSessionFactory() {
        try {
        	if (sessionFactory==null) {
        		sessionFactory = (SessionFactory) createInitialContext().lookup("java:/hibernate/ComputeSessionFactory");
        	}
            return sessionFactory;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public void recordProcessSuccess(ProcessDef processDef, Long processId) {
        try {
            if (_logger.isInfoEnabled()) {
                _logger.info("********************************\nProcess: " + processDef.getName() + " Id:" + processId + " completed successfully\n");
            }
            updateTaskStatus(processId, Event.COMPLETED_EVENT, "Process " + processId + " completed successfully");
        }
        catch (Exception e) {
            _logger.error("Caught exception updating status of process: " + processDef, e);
        }
    }

    public void updateTaskStatus(long taskId, String status, String comment) throws DaoException {
        try {
            Session session = getCurrentSession();
            Task task = (Task) session.load(Task.class, taskId, LockMode.READ);
            if (task.isDone() && !Event.ERROR_EVENT.equals(status)) {
                _logger.error("Cannot update a task to status \"" + status + "\" as it is already in DONE status: Task= " + task.toString());
                return;
            }
            if (_logger.isDebugEnabled()) {
                _logger.debug("Retrieved task=" + task.getObjectId().toString() + " from the db.");
            }
            Event event = new Event(comment, new Date(), status);
            task.addEvent(event);
            if (_logger.isInfoEnabled()) {
                _logger.info("Updating task " + task.getObjectId() + " to status " + status);
            }
            session.saveOrUpdate(task);
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public void saveTaskMessages(long taskId, Set<TaskMessage> messages) throws DaoException {
        try {
            Session session = getCurrentSession();
            Task task = (Task) session.load(Task.class, taskId, LockMode.READ);
            if (_logger.isDebugEnabled()) {
                _logger.debug("Retrieved task=" + task.getObjectId().toString() + " from the db.");
            }
            if (_logger.isInfoEnabled()) {
                _logger.info("Updating task " + task.getObjectId() + " with " + messages.size() + " messages");
            }
            task.getMessages().addAll(messages);
            session.saveOrUpdate(task);
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public Task getTaskById(long taskId) {
        return (Task) getCurrentSession().get(Task.class, taskId);
    }

    public SearchResultNode getSearchTaskResultNode(long searchTaskId) throws DaoException {
        SearchTask st = (SearchTask) getCurrentSession().get(Task.class, searchTaskId);
        return st.getSearchResultNode();
    }

    public Task getTaskWithEventsById(long taskId) {
        Task task = null;
        Query query = getCurrentSession().getNamedQuery("findTaskWithEvents");
        query.setParameter("taskId", taskId);
        List list = query.list();
        if (list.size() > 0) {
            task = (Task) list.get(0);
        }
        return task;
    }

    public Task getTaskWithMessages(long taskId) {
        Task task = null;
        Query query = getCurrentSession().getNamedQuery("findTaskWithMessages");
        query.setParameter("taskId", taskId);
        List list = query.list();
        if (list.size() > 0) {
            task = (Task) list.get(0);
        }
        return task;
    }

    public Task getTaskWithMessagesAndParameters(long taskId) {
        Task task = null;
        Query query = getCurrentSession().getNamedQuery("findTaskWithMessagesAndParameters");
        query.setParameter("taskId", taskId);
        List list = query.list();
        if (list.size() > 0) {
            task = (Task) list.get(0);
        }
        return task;
    }

    public Task getTaskWithResultsById(long taskId) {
        Task task = null;
        Query query = getCurrentSession().getNamedQuery("findTaskWithResults");
        query.setParameter("taskId", taskId);
        List list = query.list();
        if (list.size() > 0) {
            task = (Task) list.get(0);
        }
        return task;
    }

    public Node getNodeById(long nodeId) {
        return (Node) getCurrentSession().get(Node.class, nodeId);
    }

    public Node getBlastDatabaseFileNodeByName(String name) {
        Query query = getCurrentSession().getNamedQuery("findBlastDatabaseNodeByName"); // Accesion is sic
        query.setParameter("name", name); // accesion is sic
        return (Node) query.uniqueResult();
    }

    /**
     * This method returns the first result node for a given task.
     * ie if you know the task this will return the node result of that task
     *
     * @param taskId - task you're interested in
     * @return FIRST result node of the task
     */
    public Node getResultNodeByTaskId(long taskId) {
        Session session = getCurrentSession();
        Query query = session.createSQLQuery("select node_id from task t, node n where t.task_id=" + taskId + " and t.task_id=n.task_id");
        List results = query.list();
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
        Session session = getCurrentSession();
        Query query = session.createSQLQuery("select node_id from task t, node n where t.task_id=" + taskId + " and t.task_id=n.task_id");
        List results = query.list();
        Iterator iter = results.iterator();
        ArrayList<Node> nodeList = new ArrayList<Node>();
        while (iter.hasNext()) {
            long nodeId = ((BigInteger) iter.next()).longValue();
            Node n = getNodeById(nodeId);
            nodeList.add(n);
        }
        return nodeList;
    }

    // An array with 2 members is expected back.
    // First member string is the event type.
    // Second member string is the description.
    public String[] getTaskStatus(long taskId) throws DaoException {
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
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public Event createEvent(Long taskId, String eventType, String description, Date timestamp) throws DaoException {
        try {
            Task task = getTaskById(taskId);
            return createEvent(task, eventType, description, timestamp);
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public Event createEvent(Task task, String eventType, String description, Date timestamp) throws DaoException {
        try {
            Session session = getCurrentSession();
            Event event = new Event(description, timestamp, eventType);
            task.addEvent(event);
            session.saveOrUpdate(task);
            return event;
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public User getUserByName(String name) {
        Session session = getCurrentSession();
        Criteria c = session.createCriteria(User.class);
        c.add(Expression.eq("userLogin", name));
        List l = c.list();
        if (l.size() == 0) return null;
        return (User) l.get(0);
    }

    public List getUsers(){
        Session session = getCurrentSession();
        Criteria c = session.createCriteria(User.class);
        return c.list();
    }

    public Object genericGet(Class c, Long id) {
        return getCurrentSession().get(c, id);
    }

    public Object genericLoad(Class c, Long id) throws DaoException {
        try {
            return getCurrentSession().load(c, id);
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public void genericSave(Object object) throws DaoException {
        try {
            getCurrentSession().saveOrUpdate(object);
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public void genericDelete(Object object) throws DaoException {
        try {
            getCurrentSession().delete(object);
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public Object genericCreateAndReturn(Object object) throws DaoException {
        try {
            getCurrentSession().save(object);
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
        return object;
    }

    public Session getCurrentSession() {
        if (externalSession == null)
            return getSessionFactory().getCurrentSession();
        else
            return externalSession;
    }

    public Session getSession() {
        return getCurrentSession();
    }

    public void saveOrUpdate(Object item) throws DaoException {
        try {
            getCurrentSession().saveOrUpdate(item);
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public Task getTaskForNode(long nodeId) {
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
        _logger.error(e);
        return new DaoException(e, actionWhichProducedError);
    }

    /*
     * Returns all child tasks starting with parent task id
     */
    public List<Task> getChildTasksByParentTaskId(long taskId) {
        Session session = getCurrentSession();
        Query query = session.createSQLQuery("select t.task_id from task t where t.parent_task_id=" + taskId + " order by t.task_id");
        List results = query.list();
        Iterator iter = results.iterator();
        ArrayList<Task> taskList = new ArrayList<Task>();
        while (iter.hasNext()) {
            long childTaskId = ((BigInteger) iter.next()).longValue();
            Task t = getTaskById(childTaskId);
            taskList.add(t);
        }
        return taskList;
    }

}
