
package org.janelia.it.jacs.server.access.hibernate.utils;

import org.janelia.it.jacs.model.genomics.BaseSequenceEntity;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.blast.BlastTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.blast.BlastResultFileNode;
import org.janelia.it.jacs.model.user_data.blast.BlastResultNode;
import org.janelia.it.jacs.model.vo.MultiSelectVO;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.server.access.TaskDAO;
import org.janelia.it.jacs.shared.tasks.BlastJobInfo;

import java.util.List;
import java.util.Set;

/**
 * User: aresnick
 * Date: Jul 8, 2009
 * Time: 12:10:22 PM
 * <p/>
 * <p/>
 * Description:
 */
public class BlastTaskBlastJobInfoGenerator extends BlastJobInfoGenerator {

    public BlastTaskBlastJobInfoGenerator(TaskDAO taskDAO) {
        super(taskDAO);
    }

    protected ResultsNodeInfo getResultsNodeHitCount(Task task) {
        ResultsNodeInfo resultsNodeInfo = new ResultsNodeInfo();

        BlastResultFileNode resultNode = null;
        Set outputNodes = task.getOutputNodes();
        for (Object outputNode : outputNodes) {
            if (outputNode instanceof BlastResultFileNode) {
                resultNode = (BlastResultFileNode) outputNode;
                break;
            }
        }

        if (resultNode != null) {
            resultsNodeInfo.setResultsNodeID(resultNode.getObjectId());
            resultsNodeInfo.setHitCount(resultNode.getBlastHitCount());
        }

        return resultsNodeInfo;
    }

    protected String getQueryNodeId(Task task) {
        String queryNodeId = null;
        try {
            ParameterVO queryIdVO = task.getParameterVO(BlastTask.PARAM_query);
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
        Set<Node> nodes = task.getOutputNodes();
        for (Node node : nodes) {
            logger.info("Checking nodeId=" + node.getObjectId());
            if (node instanceof BlastResultNode) {
                logger.info("Node id=" + node.getObjectId() + " is a BlastResultNode");
                BlastResultNode resultNode = (BlastResultNode) node;
                // Get the first hit to determine the type
                List<BaseSequenceEntity> results = taskDAO.getBlastResultNodeBaseSequenceEntities(resultNode);
                if (results.size() > 0) {
                    logger.info("Found at least one hit");
                    BaseSequenceEntity firstHitSubj = results.get(0);
                    if (firstHitSubj != null) {
                        if (firstHitSubj.getSample() != null) {
                            info.setSubjectWithSample(true);
                        }
                        else {
                            info.setSubjectWithSample(false);
                        }
                    }
                }
                else {
                    logger.info("Did not find any hits");
                }
                if (resultNode.getDeflineMap() != null) {
                    logger.info("Setting queryDefline");
                    String queryNodeId = getQueryNodeId(task);
                    info.setQueryDefline(resultNode.getDeflineMap().get(queryNodeId));
                }
            }
        }
    }

    protected MultiSelectVO getSubjectDatabases(Task task) {
        MultiSelectVO subjectDatabases = null;
        try {
            subjectDatabases = (MultiSelectVO) task.getParameterVO(BlastTask.PARAM_subjectDatabases);
        }
        catch (ParameterException e) {
            logger.error(e, e);
        }
        return subjectDatabases;
    }
}
