package org.janelia.it.jacs.compute.access;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.janelia.it.jacs.model.jobs.DispatcherJob;
import org.janelia.it.jacs.shared.utils.StringUtils;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Iterator;
import java.util.List;

/**
 * Created by goinac on 1/12/16.
 */
public class DispatcherDAO {

    private SessionFactory sessionFactory;

    public Iterator<DispatcherJob> getPendingJobsIterator(String hostName, int maxRetries) {
        Query query = getCurrentSession().createQuery("select dj from DispatcherJob dj " +
                "where dj.status = :status " +
                "and dj.retries < :maxretries " +
                "and dj.dispatchHost = :hostname " +
                "order by dj.creationDate");
        query.setString("status", DispatcherJob.Status.PENDING.name());
        query.setInteger("maxretries", maxRetries);
        query.setString("hostname", hostName);
        return query.iterate();
    }

    /**
     * Looks up a rule for the owner and the discriminator value. If it finds such entry it returns the value of the
     * processing host otherwise it returns the defaultHost.
     * @param job
     * @param defaultHost
     * @return
     */
    public String getProcessingHost(DispatcherJob job, String defaultHost) {
        Query query = getCurrentSession().createSQLQuery("select processing_host from dispatcher_rule " +
                "where (job_owner = :job_owner or job_owner is null) " +
                (StringUtils.isBlank(job.getDispatchDiscriminatorValue())
                        ? ""
                        : "and (discriminator_value = :discriminator_value or discriminator_value is null) ") +
                "order by discriminator_value is null, discriminator_value, job_owner is null, job_owner;");
        query.setString("job_owner", job.getDispatchedTaskOwner());
        if (!StringUtils.isBlank(job.getDispatchDiscriminatorValue())) {
            query.setString("discriminator_value", job.getDispatchDiscriminatorValue());
        }
        List<String> hostCandidates = query.list();
        if (hostCandidates.size() > 0) {
            return hostCandidates.get(0);
        } else {
            return defaultHost;
        }
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
