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

package org.janelia.it.jacs.compute.service.colorSeparator;

import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.service.common.ProcessDataConstants;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.recruitment.CreateRecruitmentFileNodeException;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.colorSeparator.ColorSeparatorResultNode;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.IOException;

/**
 * @author Todd Safford
 */
public class CreateColorSeparatorResultFileNodeService implements IService {

    private Task task;
    private ColorSeparatorResultNode resultFileNode;
    private String sessionName;
    Long resultNodeId;

    public void execute(IProcessData processData) throws CreateRecruitmentFileNodeException {
        try {
            this.task = ProcessDataHelper.getTask(processData);
            sessionName = ProcessDataHelper.getSessionRelativePath(processData);
            resultNodeId = createResultFileNode();
            processData.putItem(ProcessDataConstants.RESULT_FILE_NODE_ID, resultNodeId);
            processData.putItem(ProcessDataConstants.RESULT_FILE_NODE, resultFileNode);
        }
        catch (Exception e) {
            throw new CreateRecruitmentFileNodeException(e);
        }
    }

    private Long createResultFileNode() throws DaoException, IOException {
        // if we get this far then we assume that no result persist exists and create one
        String visibility = Node.VISIBILITY_PRIVATE;

        if (User.SYSTEM_USER_LOGIN.equalsIgnoreCase(task.getOwner())) {
            visibility = Node.VISIBILITY_PUBLIC;
        }
        resultFileNode = new ColorSeparatorResultNode(task.getOwner(), task,
                "ColorSeparatorResultNode",
                "ColorSeparatorResultNode for createtask " + task.getObjectId(), visibility, sessionName);
        EJBFactory.getLocalComputeBean().saveOrUpdateNode(resultFileNode);

        FileUtil.ensureDirExists(resultFileNode.getDirectoryPath());
        FileUtil.cleanDirectory(resultFileNode.getDirectoryPath());
        return resultFileNode.getObjectId();
    }

}