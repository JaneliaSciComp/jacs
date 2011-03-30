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

package org.janelia.it.jacs.compute.service.hmmer3;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.file.FileServiceConstants;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.hmmer3.HMMER3ResultFileNode;
import org.janelia.it.jacs.shared.utils.FileUtil;

public class HMMER3CreateResultNodeService
        implements IService {

    public HMMER3CreateResultNodeService() {
    }

    public void execute(IProcessData processData)
            throws ServiceException {
        try {
            Logger _logger = ProcessDataHelper.getLoggerForTask(processData, getClass());
            Task task = ProcessDataHelper.getTask(processData);
            ComputeDAO computeDAO = new ComputeDAO(_logger);
            String sessionName = ProcessDataHelper.getSessionRelativePath(processData);
            HMMER3ResultFileNode resultFileNode = new HMMER3ResultFileNode(task.getOwner(), task, "HMMER3ResultFileNode",
                    "HMMER3ResultFileNode for createtask "+task.getObjectId(), Node.VISIBILITY_PRIVATE, sessionName);
            computeDAO.saveOrUpdate(resultFileNode);
            processData.putItem(FileServiceConstants.RESULT_FILE_NODE_ID, resultFileNode.getObjectId());
            processData.putItem(FileServiceConstants.RESULT_FILE_NODE_DIR, resultFileNode.getDirectoryPath());
            FileUtil.ensureDirExists(resultFileNode.getDirectoryPath());
            FileUtil.cleanDirectory(resultFileNode.getDirectoryPath());
            _logger.debug((new StringBuilder()).append("Created HMMER3ResultFileNode and placed in processData id=").append(resultFileNode.getObjectId()).toString());
        }
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }
}
