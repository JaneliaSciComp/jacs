
package org.janelia.it.jacs.server.access.hibernate.utils;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.psiBlast.ReversePsiBlastTask;
import org.janelia.it.jacs.model.user_data.reversePsiBlast.ReversePsiBlastResultNode;
import org.janelia.it.jacs.model.vo.MultiSelectVO;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.server.access.TaskDAO;
import org.janelia.it.jacs.shared.tasks.BlastJobInfo;

import java.util.Set;

/**
 * User: aresnick
 * Date: Jul 8, 2009
 * Time: 12:10:22 PM
 * <p/>
 * <p/>
 * Description:
 */
public class ReversePsiBlastTaskBlastJobInfoGenerator extends BlastJobInfoGenerator {

    public ReversePsiBlastTaskBlastJobInfoGenerator(TaskDAO taskDAO) {
        super(taskDAO);
    }

    protected ResultsNodeInfo getResultsNodeHitCount(Task task) {
        ResultsNodeInfo resultsNodeInfo = new ResultsNodeInfo();

        ReversePsiBlastResultNode resultNode = null;
        Set outputNodes = task.getOutputNodes();
        for (Object outputNode : outputNodes) {
            if (outputNode instanceof ReversePsiBlastResultNode) {
                resultNode = (ReversePsiBlastResultNode) outputNode;
                break;
            }
        }

        if (resultNode != null) {
            resultsNodeInfo.setResultsNodeID(resultNode.getObjectId());
            resultsNodeInfo.setHitCount(resultNode.getHitCount());
        }

        return resultsNodeInfo;

    }

    protected String getQueryNodeId(Task task) {
        String queryNodeId = null;
        try {
            ParameterVO queryIdVO = task.getParameterVO(ReversePsiBlastTask.PARAM_query_node_id);
            if (null != queryIdVO) {
                queryNodeId = queryIdVO.getStringValue();
            }
        }
        catch (ParameterException e) {
            logger.error(e, e);
        }
        return queryNodeId;
    }

    protected void setQueryDeflineAndSubjectSampleInfo(Task task, BlastJobInfo info) {
        // does not seem to be an option for ReversePsiBlastTasks, so do nothing
    }

    protected MultiSelectVO getSubjectDatabases(Task task) {
        MultiSelectVO subjectDatabases = null;
        try {
            subjectDatabases = (MultiSelectVO) task.getParameterVO(ReversePsiBlastTask.PARAM_subjectDatabases);
        }
        catch (ParameterException e) {
            logger.error(e, e);
        }
        return subjectDatabases;
    }

}