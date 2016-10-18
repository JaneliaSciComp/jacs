package org.janelia.it.jacs.compute.access;

import org.hibernate.*;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.jobs.DispatcherJob;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import javax.rmi.PortableRemoteObject;
import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DispatcherDAO {

    private static final Logger LOG = LoggerFactory.getLogger(DispatcherDAO.class);
    private final String jndiPath = SystemConfigurationProperties.getString("jdbc.jndiName", null);
    private final String jdbcDriver = SystemConfigurationProperties.getString("jdbc.driverClassName", null);
    private final String jdbcUrl = SystemConfigurationProperties.getString("jdbc.url", null);
    private final String jdbcUser = SystemConfigurationProperties.getString("jdbc.username", null);
    private final String jdbcPw = SystemConfigurationProperties.getString("jdbc.password", null);

    private SessionFactory sessionFactory;

    public List<DispatcherJob> nextPendingJobs(String hostName, boolean fetchUnassignedJobsFlag, int maxRetries, int maxLength) {
        List<DispatcherJob> nextJobs = new ArrayList<>();

        Connection conn = null;
//        if (fetchUnassignedJobsFlag) {
//            String updateQuery = "update dispatcher_job set dispatch_host = ? where dispatch_status = ? and retries < ? and dispatch_host is null limit ?";
//            try {
//                conn = getJdbcConnection();
//                pstmt = conn.prepareStatement(pendingJobsQueryBuffer.toString(),
//                        ResultSet.TYPE_SCROLL_SENSITIVE,
//                        ResultSet.CONCUR_UPDATABLE);
//                pstmt.setString(fieldIndex++, DispatcherJob.Status.PENDING.name());
//                pstmt.setInt(fieldIndex++, maxRetries);
//                pstmt.setString(fieldIndex++, hostName);
//                pstmt.setInt(fieldIndex++, maxLength);
//                rs = pstmt.executeQuery();
//
//            }
//        }
        StringBuffer pendingJobsQueryBuffer = new StringBuffer();
        pendingJobsQueryBuffer.append("select ")
                .append("dispatch_id, dispatched_task_id, dispatched_task_owner, process_defn_name, dispatch_status, dispatch_host, retries, dispatched_date, creation_date ")
                .append("from dispatcher_job ")
                .append("where dispatch_status = ? and retries < ? ");
        if (fetchUnassignedJobsFlag) {
            pendingJobsQueryBuffer.append("and (dispatch_host = ? or dispatch_host is null) ");
        } else {
            pendingJobsQueryBuffer.append("and dispatch_host = ? ");
        }
        pendingJobsQueryBuffer.append("order by creation_date limit ? FOR UPDATE");
        Date currentDate = new Date();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        int fieldIndex = 1;
        try {
            conn = getJdbcConnection();
            pstmt = conn.prepareStatement(pendingJobsQueryBuffer.toString(),
                    ResultSet.TYPE_SCROLL_SENSITIVE,
                    ResultSet.CONCUR_UPDATABLE);
            pstmt.setString(fieldIndex++, DispatcherJob.Status.PENDING.name());
            pstmt.setInt(fieldIndex++, maxRetries);
            pstmt.setString(fieldIndex++, hostName);
            pstmt.setInt(fieldIndex++, maxLength);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                DispatcherJob job = new DispatcherJob();
                job.setDispatchId(rs.getLong("dispatch_id"));
                job.setStatus(rs.getString("dispatch_status"));
                job.setDispatchHost(rs.getString("dispatch_host"));
                job.setDispatchedTaskId(rs.getLong("dispatched_task_id"));
                job.setDispatchedTaskOwner(rs.getString("dispatched_task_owner"));
                job.setProcessDefnName(rs.getString("process_defn_name"));
                job.setRetries(rs.getInt("retries"));
                job.setCreationDate(rs.getTimestamp("creation_date"));
                job.incRetries();
                job.setDispatchedDate(currentDate);
                job.setDispatchHost(hostName);
                job.setDispatchStatus(DispatcherJob.Status.IN_PROGRESS);
                rs.updateString("dispatch_status", DispatcherJob.Status.IN_PROGRESS.name());
                rs.updateString("dispatch_host", hostName);
                rs.updateInt("retries", job.getRetries());
                rs.updateTimestamp("dispatched_date", new Timestamp(currentDate.getTime()));
                rs.updateRow();
                nextJobs.add(job);
            }
        } catch (Exception e) {
            LOG.warn("Error retrieving pending jobs", e);
        } finally {
            try {
                rs.close();
            } catch (Exception ignore) {
            }
            try {
                pstmt.close();
            } catch (Exception ignore) {
            }
            try {
                conn.close();
            } catch (Exception ignore) {
            }
        }
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

    private Connection getJdbcConnection() throws DaoException {
        try {
            Connection connection = null;
            if (!StringUtils.isEmpty(jndiPath)) {
                LOG.debug("getJdbcConnection() using these parameters: jndiPath={}", jndiPath);
                Context ctx = new InitialContext();
                DataSource ds = (DataSource) PortableRemoteObject.narrow(ctx.lookup(jndiPath), DataSource.class);
                connection = ds.getConnection();
            } else {
                LOG.debug("getJdbcConnection() using these parameters: driverClassName={} url={} user={}", jdbcDriver, jdbcUrl, jdbcUser);
                Class.forName(jdbcDriver);
                connection = DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPw);
            }
            connection.setAutoCommit(false);
            return connection;
        } catch (Exception e) {
            throw new DaoException(e);
        }
    }

    private SessionFactory getSessionFactory() {
        try {
            if (sessionFactory==null) {
                EntityManager em = Persistence.createEntityManagerFactory("primary").createEntityManager();
                Session session = (Session)em.getDelegate();
                sessionFactory = session.getSessionFactory();
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
