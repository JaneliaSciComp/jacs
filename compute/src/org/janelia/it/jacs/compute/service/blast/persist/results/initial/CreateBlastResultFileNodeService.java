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

package org.janelia.it.jacs.compute.service.blast.persist.results.initial;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.service.blast.BlastProcessDataConstants;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.blast.BlastResultFileNode;
import org.janelia.it.jacs.shared.utils.FileUtil;

/**
 * This service creates a blast result file node.  It's entirely extracted from work done by Sean Murphy and Todd Safford.
 *
 * @author Tareq Nabeel
 */
public class CreateBlastResultFileNodeService implements IService {

    public void execute(IProcessData processData) throws CreateBlastFileNodeException {
        try {
            Logger logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            Task task = ProcessDataHelper.getTask(processData);
            String sessionName = ProcessDataHelper.getSessionRelativePath(processData);
            // if we get this far then we assume that no result persist exists and create one
            Task loadedTask = EJBFactory.getLocalComputeBean().getTaskById(task.getObjectId());
            BlastResultFileNode blastResultFileNode = new BlastResultFileNode(loadedTask.getOwner(), loadedTask, "BlastResultFileNode",
                    "BlastResultFileNode for createtask " + loadedTask.getObjectId(), Node.VISIBILITY_PRIVATE, sessionName);
            EJBFactory.getLocalComputeBean().saveOrUpdateNode(blastResultFileNode);

            FileUtil.ensureDirExists(blastResultFileNode.getDirectoryPath());
            FileUtil.cleanDirectory(blastResultFileNode.getDirectoryPath());

            processData.putItem(BlastProcessDataConstants.RESULT_FILE_NODE_ID, blastResultFileNode.getObjectId());
            processData.putItem(BlastProcessDataConstants.RESULT_FILE_NODE_DIR, blastResultFileNode.getDirectoryPath());
            logger.debug("Created blastResultNode and placed in processData id=" + blastResultFileNode.getObjectId());
        }
        catch (Exception e) {
            throw new CreateBlastFileNodeException(e);
        }
    }
}
