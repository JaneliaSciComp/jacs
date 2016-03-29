package org.janelia.it.jacs.compute.access;

import org.hibernate.*;
import org.janelia.it.jacs.model.jobs.DispatcherJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DispatcherDAO {

    private static final Logger LOG = LoggerFactory.getLogger(DispatcherDAO.class);

    private SessionFactory sessionFactory;

    public List<DispatcherJob> nextPendingJobs(String hostName, boolean fetchUnassignedJobsFlag, int maxRetries, int maxLength) {
        List<DispatcherJob> nextJobs = new ArrayList<>();
        Session session = getCurrentSession();
        Transaction tx = session.beginTransaction();
        StringBuffer pendingJobsQueryBuffer = new StringBuffer();
        pendingJobsQueryBuffer.append("select dj from dispatcher_job dj where dj.status = :status and dj.retries < :maxretries ");
        if (fetchUnassignedJobsFlag) {
            pendingJobsQueryBuffer.append("and (dj.dispatchHost = :hostname or dj.dispatchHost is null) ");
        } else {
            pendingJobsQueryBuffer.append("and dj.dispatchHost = :hostname ");
        }
        pendingJobsQueryBuffer.append("order by dj.creationDate");
        Query query = session.createQuery(pendingJobsQueryBuffer.toString());
        query.setString("status", DispatcherJob.Status.PENDING.name());
        query.setInteger("maxretries", maxRetries);
        query.setString("hostname", hostName);
        query.setMaxResults(maxLength);
        query.setLockMode("dj", LockMode.FORCE);
        ScrollableResults jobrs = query.scroll();
        while (jobrs.next()) {
            DispatcherJob job = (DispatcherJob) jobrs.get(0);
            Session updateSession = sessionFactory.openSession();
            Transaction updateTx = updateSession.beginTransaction();
            try {
                Query updateQuery = updateSession.createSQLQuery("update dispatcher_job " +
                        "set dispatch_status = :status, " +
                        "retries = :retries, " +
                        "dispatch_host =  :hostname, " +
                        "dispatched_date = :dispatchedDate " +
                        "where dispatch_id = :dispatchId " +
                        "and retries = :currentRetries ")
                        .setString("status", DispatcherJob.Status.IN_PROGRESS.name())
                        .setInteger("retries", job.getRetries() + 1)
                        .setString("hostname", hostName)
                        .setDate("dispatchedDate", new Date())
                        .setLong("dispatchId", job.getDispatchId())
                        .setInteger("currentRetries", job.getRetries());
                int nUpdates = updateQuery.executeUpdate();
                if (nUpdates == 1) {
                    nextJobs.add(job);
                }
                updateTx.commit();
            } catch (StaleObjectStateException e) {
                LOG.warn("Conflict while updating {} potentially caused by another node updating the job", job, e);
            } finally {
                updateSession.close();
            }
        }
        tx.commit();
        return nextJobs;
    }

    public void save(DispatcherJob dispatcherJob) {
        getCurrentSession().saveOrUpdate("dispatcher_job", dispatcherJob);
    }

    public void archive(DispatcherJob dispatcherJob) {
        Session session = getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            getCurrentSession().saveOrUpdate("archived_dispatcher_job", dispatcherJob);
            Query deleteJobQuery = session.createSQLQuery("DELETE FROM dispatcher_job WHERE dispatch_id = :dispatchId ")
                    .setLong("dispatchId", dispatcherJob.getDispatchId());
            deleteJobQuery.executeUpdate();
            tx.commit();
        } catch (Exception e) {
            LOG.warn("Error while archiving job {}", dispatcherJob, e);
            tx.rollback();
        }
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
