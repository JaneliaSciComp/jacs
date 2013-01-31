
package org.janelia.it.jacs.web.gwt.blast.server;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.common.BlastTaskVO;
import org.janelia.it.jacs.model.common.BlastableNodeVO;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.server.access.BlastDAO;
import org.janelia.it.jacs.server.access.hibernate.DaoException;
import org.janelia.it.jacs.server.api.BlastAPI;
import org.janelia.it.jacs.server.utils.SystemException;
import org.janelia.it.jacs.web.gwt.blast.client.BlastService;
import org.janelia.it.jacs.web.gwt.common.server.JcviGWTSpringController;

import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: Lfoster
 * Date: Nov 20, 2006
 * Time: 9:36:17 AM
 * <p/>
 * GWT-service side, for serving up Molecule of Interest data to the client.
 */
public class BlastServiceImpl extends JcviGWTSpringController implements BlastService {
    private BlastDAO _blastDAO;
    private BlastAPI blastAPI = new BlastAPI();
    private static Logger _log = Logger.getLogger(BlastServiceImpl.class);

    public void setBlastDAO(BlastDAO blastDAO) {
        _blastDAO = blastDAO;
    }

    public void setBlastAPI(BlastAPI blastAPI) {
        this.blastAPI = blastAPI;
    }

    public Task[] getBlastPrograms(String querySequenceType, String subjectSequenceType) {
        _log.debug("DataServiceImpl: calling blastAPI.getBlastPrograms(" + querySequenceType + "," + subjectSequenceType + ")");
        return blastAPI.getBlastPrograms(querySequenceType, subjectSequenceType);
    }

    public BlastableNodeVO[] getBlastableSubjectSets(String sequenceType) {
        _log.debug("DataServiceImpl.getBlastableSubjectSets()");
        BlastableNodeVO[] subjectList = null;
        try {
            subjectList = blastAPI.getPublicDatabaseList(sequenceType, getSessionUser().getUserLogin());
            _log.debug("Found " + subjectList.length + " subject sequences");
        }
        catch (SystemException e) {
            _log.error("Acquisition of the subject list failed", e);
            // todo Need to actually return something here
        }
        if (null == subjectList || 0 == subjectList.length) {
            return new BlastableNodeVO[0];
        }
        return subjectList;
    }

    public BlastableNodeVO getBlastableSubjectSetByNodeId(String nodeId) {
        _log.debug("DataServiceImpl.getBlastableSubjectSetByNodeId()");
        BlastableNodeVO node = null;
        try {
            node = blastAPI.getPublicDatabaseByNodeId(getSessionUser().getUserLogin(), nodeId);
            _log.debug("Found " + (node == null ? 0 : 1) + " subject sequences");
        }
        catch (SystemException e) {
            _log.error("Acquisition of the subject database failed", e);
            // todo Need to actually return something here
        }
        return node;
    }

    /**
     * Get mapping of node id vs its site location.
     *
     * @param project
     * @return map of long-for-id vs string-for-location
     */
    public Map<String, String> getNodeIdVsSiteLocation(String project) {
        try {
            return _blastDAO.getNodeIdVsSiteLocation(project);
        }
        catch (DaoException daoe) {
            _log.error(daoe);
            throw new RuntimeException(daoe);
        }
    }

    /**
     * Get all locations of sites relevant to the project whose key
     * is given.
     *
     * @param project
     * @return list of strings representing sites.
     */
    public List<String> getSiteLocations(String project) {
        try {
            return _blastDAO.getSiteLocations(project);
        }
        catch (DaoException daoe) {
            _log.error(daoe);
            throw new RuntimeException(daoe);
        }
    }

    /**
     * Populates the BlastTask session context with state from prior blast task.
     *
     * @param taskId
     * @return Re-populated BlastTask object
     */
    public BlastTaskVO getPrepopulatedBlastTask(String taskId) {
        try {
            return _blastDAO.getPrepopulatedBlastTask(taskId);
        }
        catch (DaoException daoe) {
            _log.error("There was an exception calling blastDAO.populateBlastDataFromTaskId with taskId=" + taskId);
            _log.error(daoe);
            throw new RuntimeException(daoe);
        }
    }

    public String runBlastJob(Task targetTask) {
        _log.info("org.janelia.it.jacs.web.gwt.common.server.DataServiceImpl.runBlastJob()");
        if (null == targetTask) {
            _log.error("The BlastTask sent to the DataService is null");
        }
        else {
            _log.debug("Got a non-null BlastTask");
            _log.info("TargetTask = " + targetTask.getTaskName());
        }
        _log.info("User id = " + targetTask.getOwner());

        String jobId = "";
        try {
            jobId = blastAPI.runBlast(getSessionUser(), targetTask);
        }
        catch (SystemException e) {
            // todo do something useful with this system exception
            _log.error("Blast job failed: " + e.getMessage(), e);
            jobId = "Blast job failed.";
        }
        catch (Throwable e) {
            _log.error("Blast job failed: " + e.getMessage(), e);
            jobId = "Blast job failed.";
        }

        return (jobId);
    }

}
