package org.janelia.it.jacs.compute.access;

import org.hibernate.LockMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.janelia.it.jacs.model.jobs.DispatcherJob;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Iterator;

public class DispatcherDAO {

    private SessionFactory sessionFactory;

    public Iterator<DispatcherJob> getPendingJobsIterator(String hostName, int maxRetries, int maxLength) {
        Query query = getCurrentSession().createQuery("select dj from DispatcherJob dj " +
                "where dj.status = :status " +
                "and dj.retries < :maxretries " +
                "and (dj.dispatchHost = :hostname or dj.dispatchHost is null) " +
                "order by dj.creationDate");
        query.setString("status", DispatcherJob.Status.PENDING.name());
        query.setInteger("maxretries", maxRetries);
        query.setString("hostname", hostName);
        query.setMaxResults(maxLength);
        query.setLockMode("dj", LockMode.UPGRADE);
        return query.iterate();
    }

    public void save(DispatcherJob dispatcherJob) {
        getCurrentSession().saveOrUpdate(dispatcherJob);
    }

    private SessionFactory getSessionFactory() {
        try {
            if (sessionFactory==null) {
                sessionFactory = (SessionFactory) createInitialContext().lookup("java:/hibernate/ComputeSessionFactory");
            }
            return sessionFactory;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static InitialContext createInitialContext() throws NamingException {
        return new InitialContext();
    }

    public Session getCurrentSession() {
        return getSessionFactory().getCurrentSession();
    }

}
