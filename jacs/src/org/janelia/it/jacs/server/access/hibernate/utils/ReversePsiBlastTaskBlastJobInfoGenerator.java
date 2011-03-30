/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

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