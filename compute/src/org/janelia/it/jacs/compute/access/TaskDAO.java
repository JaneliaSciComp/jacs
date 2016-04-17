package org.janelia.it.jacs.compute.access;

import org.hibernate.Query;
import org.hibernate.Session;
import org.janelia.it.jacs.compute.util.HibernateSessionUtils;
import org.janelia.it.jacs.model.tasks.Task;

import java.util.List;

/**
 * Created by goinac on 4/12/16.
 */
public class TaskDAO {
    private static final int DEFAULT_MAX_LENGTH = 100;

    public List<Task> getAllTasksByOwner(String owner, int offset, int length, Session session) {
        Query q = session.createQuery("select t " +
                "from Task t " +
                "where t.owner = :task_owner");
        q.setParameter("task_owner", owner);
        if (offset > 0) {
            q.setFirstResult(offset);
        }
        if (length > 0) {
            q.setMaxResults(length);
        } else {
            q.setMaxResults(DEFAULT_MAX_LENGTH);
        }
        return q.list();
    }

    public Task getTaskById(Long taskId, Session session) {
        Query q = session.createQuery("select t from Task t " +
                "where t.objectId = :id");
        q.setParameter("id", taskId);
        return (Task) q.uniqueResult();
    }

    public List<Task> getChildrenTasksByParentTaskId(Long parentTaskId, Session session) {
        Query q = session.createQuery("select t from Task t " +
                "where t.parentTaskId = :parentTaskId ");
        q.setParameter("parentTaskId", parentTaskId);
        return q.list();
    }
}
