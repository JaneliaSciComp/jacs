
package org.janelia.it.jacs.web.gwt.blast.client;

import com.google.gwt.user.client.rpc.RemoteService;
import org.janelia.it.jacs.model.common.BlastTaskVO;
import org.janelia.it.jacs.model.common.BlastableNodeVO;
import org.janelia.it.jacs.model.tasks.Task;

import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: Lfoster
 * Date: Nov 20, 2006
 * Time: 9:39:33 AM
 * <p/>
 * Implement this to support client/server transfer for the BLAST entry point.
 */
public interface BlastService extends RemoteService {

    /**
     * Return all site location strings that are associated with the project given.
     *
     * @param project DB key corresponding to a project.
     * @return list of site location strings.
     */
    public List<String> getSiteLocations(String project);

    /**
     * Return mapping of node id vs its site location.
     *
     * @param project string of project in question
     * @return map of long-for-id vs string-for-location
     */
    public Map<String, String> getNodeIdVsSiteLocation(String project);

    /**
     * Populates a BlastTask with info to repopulate job
     *
     * @param taskId string of task id in question
     * @return Re-populated BlastTask object
     */
    public BlastTaskVO getPrepopulatedBlastTask(String taskId);

    public String runBlastJob(Task targetTask);

    public Task[] getBlastPrograms(String querySequenceType, String subjectSequenceType);

    public BlastableNodeVO[] getBlastableSubjectSets(String sequenceType);

    public BlastableNodeVO getBlastableSubjectSetByNodeId(String nodeId);
}
