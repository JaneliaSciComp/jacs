
package org.janelia.it.jacs.server.access.hibernate.utils;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.MultiSelectVO;
import org.janelia.it.jacs.server.access.TaskDAO;
import org.janelia.it.jacs.server.access.hibernate.DaoException;
import org.janelia.it.jacs.shared.tasks.BlastJobInfo;

import java.text.DecimalFormat;
import java.util.*;

/**
 * User: aresnick
 * Date: Jul 8, 2009
 * Time: 11:37:27 AM
 * <p/>
 * <p/>
 * Description:
 */
abstract public class BlastJobInfoGenerator {
    protected Logger logger = Logger.getLogger(BlastJobInfoGenerator.class);
    protected TaskDAO taskDAO;

    BlastJobInfoGenerator(TaskDAO taskDAO) {
        this.taskDAO = taskDAO;
    }

    public List<BlastJobInfo> getBlastJobInfo(List<Task> tasks) throws DaoException {
        List<BlastJobInfo> blastJobInfo = new ArrayList<BlastJobInfo>(tasks.size());
        for (Task task : tasks) {
            blastJobInfo.add(getBlastJobInfo(task));
        }
        return blastJobInfo;
    }

    public BlastJobInfo getBlastJobInfo(Task task) throws DaoException {
        logger.info("Starting createBlastStatusInfo for taskId=" + task.getObjectId());
        BlastJobInfo info = new BlastJobInfo();
        // Get the default information
        info.setJobId(String.valueOf(task.getObjectId()));
        info.setUsername(task.getOwner());

        // Event-based attributes
        logger.info("Getting event info");
        Event lastEvent = task.getLastEvent();
        if (lastEvent != null) {
            info.setStatusDescription(lastEvent.getDescription());
            info.setStatus(lastEvent.getEventType());
        }
        Event firstEvent = task.getFirstEvent();
        if (firstEvent != null) {
            info.setSubmitted(new Date(firstEvent.getTimestamp().getTime()));
        }

        // Parameters
        HashMap<String, String> paramMap = new HashMap<String, String>();
        for (String s : task.getParameterKeySet()) {
            String value = task.getParameter(s);
            paramMap.put(s, value);
        }
        info.setParamMap(paramMap);

        // Set the total number of hits
        ResultsNodeInfo resultsNodeInfo = getResultsNodeHitCount(task);
        Long numHitsResults = resultsNodeInfo.getHitCount();
        info.setNumHits(numHitsResults);
        if (null != numHitsResults) {
            info.setNumHitsFormatted(new DecimalFormat("###,###").format(numHitsResults.longValue()));
            if (numHitsResults > SystemConfigurationProperties.getInt("BlastServer.HitThresholdForFileNode")) {
                info.setBlastResultFileNodeId(resultsNodeInfo.getResultsNodeID().toString());
            }
        }

        // Job name
        logger.info("Getting jobName info");
        info.setJobname(task.getJobName());

        info.setProgram(task.getTaskName());
        if (task.getMessages() == null) {
            info.setNumTaskMessages(0);
        }
        else {
            info.setNumTaskMessages(task.getMessages().size());
        }
        Map<String, String> nodeNames = new HashMap<String, String>();
        // Query name
        String queryNodeId = getQueryNodeId(task);
        logger.info("Calling getNodeNames() with queryNodeId=" + queryNodeId);
        taskDAO.getNodeNames(Arrays.asList(queryNodeId), nodeNames);
        info.setQueryName(nodeNames.get(queryNodeId));
        logger.info("after getNodeNames, info.getQueryName()=" + info.getQueryName());

        // Get the number of hits from the Tasks's BlastResultNode
        logger.info("Beginning cycle through nodes to get hit stats");
        setQueryDeflineAndSubjectSampleInfo(task, info);
        logger.info("Finished getting hit stats from nodes, starting to get subject database info");

        //  populate the name of the searched databases
        List<String> subjIds = new ArrayList<String>();
        MultiSelectVO searchedDatabases = getSubjectDatabases(task);
        if (null != searchedDatabases) {
            subjIds = searchedDatabases.getActualUserChoices();
            logger.info("Adding subject node names for searchedDatabases=" + searchedDatabases.getStringValue());
        }
        if (subjIds != null && subjIds.size() > 0) {
            List<String> validSubjIds = new ArrayList<String>();
            for (String id : subjIds) {
                if (id != null && id.length() > 0) {
                    validSubjIds.add(id);
                }
            }
            taskDAO.getNodeNames(validSubjIds, nodeNames);
            for (String id : validSubjIds) {
                info.addSubject(id, nodeNames.get(id));
            }
        }
        else {
            logger.info("subjIds are null or empty");
        }

        logger.info("Returning");
        return info;
    }

    abstract protected ResultsNodeInfo getResultsNodeHitCount(Task task);

    abstract protected String getQueryNodeId(Task task);

    abstract protected MultiSelectVO getSubjectDatabases(Task task);

    abstract protected void setQueryDeflineAndSubjectSampleInfo(Task task, BlastJobInfo info);

    class ResultsNodeInfo {
        private Long resultsNodeID;
        private Long hitCount;

        ResultsNodeInfo() {
        }

        public Long getHitCount() {
            return hitCount;
        }

        public void setHitCount(Long hitCount) {
            this.hitCount = hitCount;
        }

        public Long getResultsNodeID() {
            return resultsNodeID;
        }

        public void setResultsNodeID(Long resultsNodeID) {
            this.resultsNodeID = resultsNodeID;
        }
    }
}
