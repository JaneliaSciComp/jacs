package org.janelia.it.jacs.compute.access;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.*;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;
import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.hibernate.*;
import org.janelia.it.jacs.compute.engine.def.ProcessDef;
import org.janelia.it.jacs.model.entity.*;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskMessage;
import org.janelia.it.jacs.model.tasks.search.SearchTask;
import org.janelia.it.jacs.model.user_data.*;
import org.janelia.it.jacs.model.user_data.search.SearchResultNode;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Oct 26, 2007
 * Time: 2:24:24 PM
 */
public class ComputeBaseDAO {
    
    public static final int STATUS_TYPE = 0;
    public static final int STATUS_DESCRIPTION = 1;
    
    private final String jndiPath = SystemConfigurationProperties.getString("batch.jdbc.jndiName", null);
    private final String jdbcDriver = SystemConfigurationProperties.getString("batch.jdbc.driverClassName", null);
    private final String jdbcUrl = SystemConfigurationProperties.getString("batch.jdbc.url", null);
    private final String jdbcUser = SystemConfigurationProperties.getString("batch.jdbc.username", null);
    private final String jdbcPw = SystemConfigurationProperties.getString("batch.jdbc.password", null);
    
    protected Logger log;
    protected SessionFactory sessionFactory;
    protected Session externalSession;

    public ComputeBaseDAO(Logger log) {
        getSessionFactory();
        this.log = log;
    }

    public ComputeBaseDAO(Session externalSession) {
        this.externalSession = externalSession;
    }

    public Connection getJdbcConnection() throws DaoException {
    	try {
    	    Connection connection = null;
            if (!StringUtils.isEmpty(jndiPath)) {
                if (log.isTraceEnabled()) {
                    log.trace("getJdbcConnection() using these parameters: jndiPath="+jndiPath);
                }
                Context ctx = new InitialContext();
                DataSource ds = (DataSource) PortableRemoteObject.narrow(ctx.lookup(jndiPath), DataSource.class);
                connection = ds.getConnection();
            }
            else {
                if (log.isTraceEnabled()) {
                    log.trace("getJdbcConnection() using these parameters: driverClassName="+jdbcDriver+" url="+jdbcUrl+" user="+jdbcUser);
                }
                Class.forName(jdbcDriver);
                connection = DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPw);
            }
            connection.setAutoCommit(false);
            return connection;
    	}
    	catch (Exception e) {
    		throw new DaoException(e);
    	}
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

    public void updateTaskStatus(long taskId, String status, String comment) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("updateTaskStatus(taskId="+taskId+", status="+status+", comment="+comment+")");    
        }
        
        try {
            Session session = getCurrentSession();
            Task task = (Task) session.load(Task.class, taskId, LockMode.READ);
            if (task.isDone() && !Event.ERROR_EVENT.equals(status)) {
                log.warn("Cannot update task "+task.getObjectId()+" to status \"" + status + "\" as it is already in DONE status.");
                return;
            }
            
            if (task.getLastEvent().getEventType().equals(status) && task.getLastEvent().getDescription().equals(comment)) {
                // Compensate for bad error handling in the process framework. Some errors can be logged multiple times,
                // so this attempts to dedup them so that we keep the database clean.
                log.debug("Cannot update task "+task.getObjectId()+" to status \"" + status + "\" as it is already in that status with the same message.");
                return;
            }
            
            if (log.isDebugEnabled()) {
                log.debug("Retrieved task=" + task.getObjectId().toString() + " from the db.");
            }
            Event event = new Event(comment, new Date(), status);
            task.addEvent(event);
            if (log.isInfoEnabled()) {
                log.info("Updating task " + task.getObjectId() + " to status " + status);
            }
            session.saveOrUpdate(task);
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public void saveTaskMessages(long taskId, Set<TaskMessage> messages) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("saveTaskMessages(taskId="+taskId+", messages.size="+messages+")");    
        }
        
        try {
            Session session = getCurrentSession();
            Task task = (Task) session.load(Task.class, taskId, LockMode.READ);
            if (log.isDebugEnabled()) {
                log.debug("Retrieved task=" + task.getObjectId().toString() + " from the db.");
            }
            if (log.isInfoEnabled()) {
                log.info("Updating task " + task.getObjectId() + " with " + messages.size() + " messages");
            }
            task.getMessages().addAll(messages);
            session.saveOrUpdate(task);
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public Task getTaskById(long taskId) {
        if (log.isTraceEnabled()) {
            log.trace("getTaskById(taskId="+taskId+")");    
        }
        return (Task) getCurrentSession().get(Task.class, taskId);
    }

    public List<Task> getMostRecentTasksWithName(String owner, String taskName) {
        if (log.isTraceEnabled()) {
            log.trace("getMostRecentTasksWithName(owner="+owner+", taskName="+taskName+")");    
        }
        
        String ownerName = EntityUtils.getNameFromSubjectKey(owner);
        Session session = getCurrentSession();
        StringBuffer hql = new StringBuffer("select t from Task t ");
        hql.append("where t.owner = :owner ");
        hql.append("and t.taskName = :name ");
        hql.append("order by t.objectId desc");
        Query query = session.createQuery(hql.toString());
        query.setString("owner", ownerName);
        query.setString("name", taskName);
        return (List<Task>)query.list();
    }

    public int cancelIncompleteTasksWithName(String owner, String taskName) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("getIncompleteTasks(owner="+owner+", taskName="+taskName+")");    
        }
        
        String ownerName = EntityUtils.getNameFromSubjectKey(owner);
        Session session = getCurrentSession();
        StringBuffer hql = new StringBuffer("select t from Task t ");
        hql.append("inner join fetch t.events ");
        hql.append("where t.owner = :owner ");
        if (null!=taskName) {hql.append("and t.taskName = :name ");}
        hql.append("order by t.objectId desc");
        Query query = session.createQuery(hql.toString());
        query.setString("owner", ownerName);
        query.setString("name", taskName);
        
        int c = 0;
        for(Task task : (List<Task>)query.list()) {
            if (!task.isDone()) {
                c++;
                cancelTaskById(task);
            }
            for(Task subtask : getChildTasksByParentTaskId(task.getObjectId())) {
                if (!subtask.isDone()) {
                    cancelTaskById(task);
                }
            }
        }
        
        return c;
    }

    public int cancelIncompleteTasksForUser(String owner) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("cancelIncompleteTasks(owner="+owner+")");
        }

        String ownerName = owner.contains(":") ? owner.split(":")[1] : owner;
        Session session = getCurrentSession();
        StringBuilder search = new StringBuilder("select task.task_id ");
        search.append("from task join task_event on task.task_id = task_event.task_id ");
        search.append("where task.task_owner= :ownerName and event_no in ( ");
        search.append("select max(event_no) as event_no ");
        search.append("from task_event task_event1 where  task_event1.task_id=task_event.task_id ");
        search.append("order by task.task_id asc ) and event_type != 'completed' and task_event.event_type!='error' and task_event.event_type!='canceled'");
        SQLQuery query = session.createSQLQuery(search.toString());
        query.setString("ownerName", ownerName);

        int c = 0;
        for(Object taskId : query.list()) {
            Task task = getTaskById(((BigInteger)taskId).longValue());
            if (!task.isDone()) {
                c++;
                cancelTaskById(task);
            }
//            for(Task subtask : getChildTasksByParentTaskId(task.getObjectId())) {
//                if (!subtask.isDone()) {
//                    c++;
//                    cancelTaskById(task);
//                }
//            }
        }

        return c;
    }

    public void cancelTaskById(Task task) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("cancelTaskById(task.objectId="+task.getObjectId()+")");    
        }
        if (task.getLastEvent()==null || task.getLastEvent().getEventType()==null || !task.getLastEvent().getEventType().equals(Event.CANCELED_EVENT)) {
            Event event = new Event();
            event.setEventType(Event.CANCELED_EVENT);
            event.setTimestamp(new Date());
            task.addEvent(event);
            saveOrUpdate(task);
        }
    }
    
    public SearchResultNode getSearchTaskResultNode(long searchTaskId) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("getSearchTaskResultNode(searchTaskId="+searchTaskId+")");    
        }
        
        SearchTask st = (SearchTask) getCurrentSession().get(Task.class, searchTaskId);
        return st.getSearchResultNode();
    }

    public Task getTaskWithEventsById(long taskId) {
        if (log.isTraceEnabled()) {
            log.trace("getTaskWithEventsById(taskId="+taskId+")");    
        }
        
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
        if (log.isTraceEnabled()) {
            log.trace("getTaskWithMessages(taskId="+taskId+")");    
        }
        
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
        if (log.isTraceEnabled()) {
            log.trace("getTaskWithMessagesAndParameters(taskId="+taskId+")");    
        }
        
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
        if (log.isTraceEnabled()) {
            log.trace("getTaskWithResultsById(taskId="+taskId+")");    
        }
        
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
        if (log.isTraceEnabled()) {
            log.trace("getNodeById(nodeId="+nodeId+")");    
        }
        
        return (Node) getCurrentSession().get(Node.class, nodeId);
    }

    public FileNode getFileNodeByPathOverride(String pathOverride) {
        if (log.isTraceEnabled()) {
            log.trace("getFileNodeByPathOverride(pathOverride="+pathOverride+")");    
        }
        
        Session session = getCurrentSession();
        StringBuffer hql = new StringBuffer("select n from Node n ");
        hql.append("where n.pathOverride = :pathOverride ");
        Query query = session.createQuery(hql.toString());
        query.setString("pathOverride", pathOverride);
        return (FileNode)query.uniqueResult();
    }
    
    public Node getBlastDatabaseFileNodeByName(String name) {
        if (log.isTraceEnabled()) {
            log.trace("getBlastDatabaseFileNodeByName(name="+name+")");    
        }
        
        Query query = getCurrentSession().getNamedQuery("findBlastDatabaseNodeByName"); // Accesion is sic
        query.setParameter("name", name); // accesion is sic
        return (Node) query.uniqueResult();
    }

    public int bulkUpdateNodePathOverridePrefix(String oldPrefix, String newPrefix) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("bulkUpdateNodePathOverridePrefix(oldPrefix="+oldPrefix+", newPrefix="+newPrefix+")");    
        }
        
        try {
            StringBuilder hql = new StringBuilder();
            hql.append("update Node n set n.pathOverride = concat(:newPrefix,substring(n.pathOverride, :prefixOffset)) "); 
            hql.append("where n.pathOverride like :oldPrefix");
            
            final Session currentSession = getCurrentSession();
            Query query = currentSession.createQuery(hql.toString());
            query.setParameter("newPrefix", newPrefix);
            query.setParameter("prefixOffset", oldPrefix.length()+1);
            query.setParameter("oldPrefix", oldPrefix+"%");
            
            int rows = query.executeUpdate();
            log.debug("Bulk updated node path override prefix for "+rows+" rows");
            return rows;
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
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
        if (log.isTraceEnabled()) {
            log.trace("getResultNodesByTaskId(taskId="+taskId+")");    
        }
        
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
        }
        catch (Exception e) {
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

    public User getUserByNameOrKey(String nameOrKey) {
        if (log.isTraceEnabled()) {
            log.trace("getUserByNameOrKey(nameOrKey="+nameOrKey+")");    
        }
        
        Session session = getCurrentSession();
        StringBuffer hql = new StringBuffer("select u from User u ");
        hql.append("left outer join fetch u.groupRelationships gr ");
        hql.append("left outer join fetch gr.group ");
        hql.append("where u.name = :name or u.key = :name ");
        Query query = session.createQuery(hql.toString());
        query.setString("name", nameOrKey);
        return (User)query.uniqueResult();
    }
    
    public Group getGroupByNameOrKey(String nameOrKey) {
        if (log.isTraceEnabled()) {
            log.trace("getGroupByNameOrKey(nameOrKey="+nameOrKey+")");    
        }
        
        Session session = getCurrentSession();
        StringBuffer hql = new StringBuffer("select g from Group g ");
        hql.append("left outer join fetch g.userRelationships ur ");
        hql.append("left outer join fetch ur.user ");
        hql.append("where g.name = :name or g.key = :name ");
        Query query = session.createQuery(hql.toString());
        query.setString("name", nameOrKey);
        return (Group)query.uniqueResult();
    }

    public Subject getSubjectByNameOrKey(String nameOrKey) {
        if (log.isTraceEnabled()) {
            log.trace("getSubjectByNameOrKey(nameOrKey="+nameOrKey+")");    
        }
        
        Session session = getCurrentSession();
        StringBuffer hql = new StringBuffer("select s from Subject s ");
        hql.append("where s.name = :name or s.key = :name ");
        Query query = session.createQuery(hql.toString());
        query.setString("name", nameOrKey);
        return (Subject)query.uniqueResult();
    }
    
    
    public List<String> getGroupKeysForUsernameOrSubjectKey(String userKey) {
        if (log.isTraceEnabled()) {
            log.trace("getGroupKeysForUsernameOrSubjectKey(userKey="+userKey+")");    
        }
        
        StringBuffer hql = new StringBuffer();
        hql.append("select g.key from Group g ");
        hql.append("join g.userRelationships ur ");
        hql.append("join ur.user u ");
        hql.append("where u.name = :userKey or u.key = :userKey ");
        Query query = getCurrentSession().createQuery(hql.toString());
        query.setString("userKey", userKey);
        List<String> list = query.list();
        return list;
    }

    public List<String> getSubjectKeys(String subjectKey) {
        List<String> subjectKeyList = new ArrayList<String>();
        if (subjectKey == null || "".equals(subjectKey.trim())) return subjectKeyList;
        subjectKeyList.add(subjectKey);
        subjectKeyList.addAll(getGroupKeysForUsernameOrSubjectKey(subjectKey));
        return subjectKeyList;
    }

    public Set<String> getSubjectKeySet(String subjectKey) {
        Set<String> subjectKeys = null;
        if (subjectKey!=null) {
            subjectKeys = new HashSet<String>(getSubjectKeys(subjectKey));
        }
        return subjectKeys;
    }
    
    public Object genericGet(Class c, Long id) {
        if (log.isTraceEnabled()) {
            log.trace("genericGet(c="+c+", id="+id+")");    
        }
        
        return getCurrentSession().get(c, id);
    }

    public Object genericLoad(Class c, Long id) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("genericLoad(c="+c+", id="+id+")");    
        }
    
        try {
            return getCurrentSession().load(c, id);
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public void genericSave(Object object) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("genericSave(object="+object+")");    
        }
        try {
            getCurrentSession().saveOrUpdate(object);
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public void genericDelete(Object object) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("genericDelete(object="+object+")");    
        }
        try {
            getCurrentSession().delete(object);
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public Object genericCreateAndReturn(Object object) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("genericCreateAndReturn(object="+object+")");    
        }
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

    public Session openNewExternalSession() {
        externalSession = getSessionFactory().openSession();
        return externalSession;
    }
    
    public void closeExternalSession() {
        if (externalSession!=null) externalSession.close();
        externalSession = null;
    }
    
    public Session getSession() {
        return getCurrentSession();
    }

    public void saveOrUpdate(Object item) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("saveOrUpdate(item="+item+")");    
        }
        try {
            getCurrentSession().saveOrUpdate(item);
        }
        catch (Exception e) {
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
