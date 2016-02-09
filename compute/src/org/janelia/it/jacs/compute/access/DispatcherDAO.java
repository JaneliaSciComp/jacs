package org.janelia.it.jacs.compute.access;

import org.hibernate.*;
import org.janelia.it.jacs.model.jobs.DispatcherJob;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DispatcherDAO {

    private SessionFactory sessionFactory;

    public List<DispatcherJob> nextPendingJobs(String hostName, int maxRetries, int maxLength) {
        List<DispatcherJob> nextJobs = new ArrayList<>();
        Session session = getCurrentSession();
        Transaction tx = session.beginTransaction();
        Query query = session.createQuery("select dj from DispatcherJob dj " +
                "where dj.status = :status " +
                "and dj.retries < :maxretries " +
                "and (dj.dispatchHost = :hostname or dj.dispatchHost is null) " +
                "order by dj.creationDate");
        query.setString("status", DispatcherJob.Status.PENDING.name());
        query.setInteger("maxretries", maxRetries);
        query.setString("hostname", hostName);
        query.setMaxResults(maxLength);
        query.setLockMode("dj", LockMode.FORCE);
        ScrollableResults jobs = query.scroll();
        while (jobs.next()) {
            final DispatcherJob job = (DispatcherJob) jobs.get(0);
            job.incRetries();
            job.setDispatchedDate(new Date());
            job.setDispatchHost(hostName);
            job.setDispatchStatus(DispatcherJob.Status.IN_PROGRESS);
            session.update(job);
            nextJobs.add(job);
        }
        tx.commit();
        return nextJobs;
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
