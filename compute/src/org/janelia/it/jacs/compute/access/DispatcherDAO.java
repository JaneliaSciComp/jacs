package org.janelia.it.jacs.compute.access;

import com.google.common.collect.ImmutableMap;
import org.janelia.it.jacs.model.jobs.ArchivedJob;
import org.janelia.it.jacs.model.jobs.DispatcherJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DispatcherDAO extends AbstractBaseDAO {

    private static final Logger LOG = LoggerFactory.getLogger(DispatcherDAO.class);

    @Resource(mappedName = "java://ComputeServer_DataSource")
    private DataSource dataSource;

    public DispatcherDAO(EntityManager entityManager) {
        super(entityManager);
    }

    public List<DispatcherJob> nextPendingJobs(String hostName, boolean fetchUnassignedJobsFlag, int maxRetries, int maxLength) {
        List<DispatcherJob> nextJobs = new ArrayList<>();

        Connection conn = null;
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
            conn = dataSource.getConnection();
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

    public void archive(DispatcherJob dispatcherJob) {
        ArchivedJob archivedJob = new ArchivedJob();
        dispatcherJob.copyTo(archivedJob);
        save(archivedJob);
        executeNativeStmt("DELETE FROM dispatcher_job WHERE dispatch_id = :dispatchId",
                ImmutableMap.<String, Object>of("dispatchId", dispatcherJob.getDispatchId()));
    }

}
