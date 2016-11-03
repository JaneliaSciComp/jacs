package org.janelia.it.jacs.compute.access;

import com.google.common.collect.ImmutableMap;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;

public class TaskDAO  extends AbstractBaseDAO {
    private static Logger LOG = LoggerFactory.getLogger(TaskDAO.class);

    private static final int DEFAULT_MAX_LENGTH = 100;

    public TaskDAO(EntityManager entityManager) {
        super(entityManager);
    }


    public List<Task> getAllTasksByOwner(String owner, int offset, int length) {
        int nResults = length > 0 ? length : DEFAULT_MAX_LENGTH;
        return findByQueryParamsWithPaging("select t from Task t where t.owner = :taskOwner",
                ImmutableMap.<String, Object>of("taskOwner", owner),
                offset, nResults,
                Task.class);
    }

    public Task getTaskById(Long taskId) {
        return findByNumericId(taskId, Task.class);
    }

    public List<Task> getChildrenTasksByParentTaskId(Long parentTaskId) {
        return findByQueryParams("select t from Task t where t.parentTaskId = :parentTaskId",
                ImmutableMap.<String, Object>of("parentTaskId", parentTaskId),
                Task.class);
    }

    public List<Task> getMostRecentTasksWithName(String owner, String taskName) {
        LOG.trace("getMostRecentTasksWithName owner={}, taskName={}", owner, taskName);

        String ownerName = DomainUtils.getNameFromSubjectKey(owner);
        String hql = "select t from Task t where t.owner = :owner and t.taskName = :name order by t.objectId desc";
        return findByQueryParams(hql,
                ImmutableMap.<String, Object>of(
                    "owner", ownerName,
                    "name", taskName
                ),
                Task.class);
    }

    public void updateTaskStatus(long taskId, String status, String comment) throws DaoException {
        LOG.trace("updateTaskStatus(taskId={}, status={}, comment={})", taskId, status, comment);
        try {
            Task task = findByNumericId(taskId, Task.class);
            if (task.isDone() && !Event.ERROR_EVENT.equals(status)) {
                LOG.warn("Cannot update task "+task.getObjectId()+" to status \"" + status + "\" as it is already in DONE status.");
                return;
            }

            if (task.getLastEvent().getEventType().equals(status) && task.getLastEvent().getDescription().equals(comment)) {
                // Compensate for bad error handling in the process framework. Some errors can be logged multiple times,
                // so this attempts to dedup them so that we keep the database clean.
                LOG.debug("Cannot update task {} to status '{}' as it is already in that status with the same message.", task.getObjectId(), status);
                return;
            }

            Event event = new Event(comment, new Date(), status);
            task.addEvent(event);
            LOG.info("Update task {} to status {}", task.getObjectId(), status);
            update(task);
        } catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public void saveTaskMessages(long taskId, Set<TaskMessage> messages) throws DaoException {
        LOG.trace("saveTaskMessages(taskId=" + taskId + ", messages.size=" + messages + ")");

        try {
            Task task = findByNumericId(taskId, Task.class);
            LOG.debug("Retrieved task {} from the db.", task.getObjectId());
            LOG.info("Updating task {} with {} messages", task.getObjectId(), messages.size());
            task.getMessages().addAll(messages);
            update(task);
        } catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public int cancelIncompleteTasksWithName(String owner, String taskName) throws DaoException {
        LOG.trace("getIncompleteTasks(owner={}, taskName={})", owner, taskName);

        String ownerName = DomainUtils.getNameFromSubjectKey(owner);
        StringBuilder hqlBuffer = new StringBuilder("select t from Task t inner join fetch t.events where t.owner = :owner ");
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("owner", ownerName);
        if (null != taskName) {
            hqlBuffer.append("and t.taskName = :name ");
            queryParams.put("name", taskName);
        }
        hqlBuffer.append("order by t.objectId desc");

        List<Task> tasks = findByQueryParams(hqlBuffer.toString(), queryParams, Task.class);
        int c = 0;
        for(Task task : tasks) {
            if (!task.isDone()) {
                c++;
                cancelTaskById(task);
            }
            for(Task subtask : getChildrenTasksByParentTaskId(task.getObjectId())) {
                if (!subtask.isDone()) {
                    cancelTaskById(task);
                }
            }
        }

        return c;
    }

    public int cancelIncompleteTasksForUser(String owner) throws DaoException {
        LOG.trace("cancelIncompleteTasks(owner={})",owner);

        String ownerName = owner.contains(":") ? owner.split(":")[1] : owner;
        String hql = "select task.task_id from task join task_event on task.task_id = task_event.task_id " +
                "where task.task_owner = :ownerName and event_no in ( " +
                "select max(event_no) as event_no " +
                "from task_event task_event1 where  task_event1.task_id=task_event.task_id " +
                "order by task.task_id asc) and event_type != 'completed' and task_event.event_type != 'error' and task_event.event_type != 'canceled'";

        List<Task> tasks = findByQueryParams(hql, ImmutableMap.<String, Object>of("ownerName", ownerName), Task.class);
        int c = 0;
        for(Object taskId : tasks) {
            Task task = getTaskById(((BigInteger)taskId).longValue());
            if (!task.isDone()) {
                c++;
                cancelTaskById(task);
            }
        }
        return c;
    }

    public void cancelTaskById(Task task) throws DaoException {
        LOG.trace("cancelTaskById(task.objectId={})", task.getObjectId());
        if (task.getLastEvent() == null || task.getLastEvent().getEventType()==null || !task.getLastEvent().getEventType().equals(Event.CANCELED_EVENT)) {
            Event event = new Event();
            event.setEventType(Event.CANCELED_EVENT);
            event.setTimestamp(new Date());
            task.addEvent(event);
            save(event);
        }
    }

    public Task getTaskWithEventsById(long taskId) {
        LOG.trace("getTaskWithEventsById(taskId={})", taskId);

        return getAtMostOneResult(prepareNamedQuery("findTaskWithEvents", ImmutableMap.<String, Object>of("taskId", taskId), Task.class));
    }

    public Task getTaskWithMessages(long taskId) {
        LOG.trace("getTaskWithMessages(taskId={})", taskId);

        return getAtMostOneResult(prepareNamedQuery("findTaskWithMessages", ImmutableMap.<String, Object>of("taskId", taskId), Task.class));
    }

    public Task getTaskWithMessagesAndParameters(long taskId) {
        LOG.trace("getTaskWithMessagesAndParameters(taskId={})", taskId);

        return getAtMostOneResult(prepareNamedQuery("findTaskWithMessagesAndParameters", ImmutableMap.<String, Object>of("taskId", taskId), Task.class));
    }

    public Task getTaskWithResultsById(long taskId) {
        LOG.trace("getTaskWithResultsById(taskId={})", taskId);

        return getAtMostOneResult(prepareNamedQuery("findTaskWithResults", ImmutableMap.<String, Object>of("taskId", taskId), Task.class));
    }

}
