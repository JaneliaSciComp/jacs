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

package org.janelia.it.jacs.compute.service.common.file;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.utility.UploadSamFileTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.SamFileNode;
import org.janelia.it.jacs.shared.node.SamUtil;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.File;
import java.io.IOException;


/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 2, 2010
 * Time: 3:28:24 PM
 */
public class UploadSamFileService implements IService {

    private File sourceFile;
    UploadSamFileTask task;
    String sessionName;
    File scratchDir = new File(SystemConfigurationProperties.getString("SystemCall.ScratchDir"));

    public UploadSamFileService() {
    }

    public void execute(IProcessData processData) throws ServiceException {
        try {
            Logger logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            logger.info("UploadSamFileService execute() start");
            init(processData);
            long sequenceCount = SamUtil.countSequencesInSamFile(sourceFile, scratchDir, logger);
            logger.info("Found " + sequenceCount + " sequences in file=" + sourceFile.getAbsolutePath());
            SamFileNode samFileNode = createSamFileNode(sequenceCount);
            File targetFile = new File(samFileNode.getSamFilePath());
            logger.info("Copying file to SamFileNode source=" + sourceFile.getAbsolutePath() + " target=" + targetFile.getAbsolutePath());
            FileUtil.copyFileUsingSystemCall(sourceFile, targetFile);
            logger.info("UploadSamFileService execute() end");
        }
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    private SamFileNode createSamFileNode(long totalSequenceCount) throws Exception {
        SamFileNode node = new SamFileNode(task.getOwner(), task, "Upload node for task=" + task.getObjectId(),
                "Upload of dir=" + sourceFile.getAbsolutePath(), Node.VISIBILITY_PUBLIC, new Integer(totalSequenceCount + ""), sessionName);
        ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
        node = (SamFileNode) computeBean.saveOrUpdateNode(node);
        File nodeFile = new File(node.getDirectoryPath());
        if (!nodeFile.mkdirs()) {
            throw new Exception("Could not create node directory=" + nodeFile.getAbsolutePath());
        }
        return node;
    }

    /**
     * Initialize the input parameters
     *
     * @param processData params of the task
     * @throws org.janelia.it.jacs.compute.engine.data.MissingDataException
     *                             cannot find data required to process
     * @throws java.io.IOException problem accessing file data
     */
    protected void init(IProcessData processData) throws MissingDataException, IOException {
        task = (UploadSamFileTask) ProcessDataHelper.getTask(processData);
        sessionName = ProcessDataHelper.getSessionRelativePath(processData);
        String tmpSourceFile = task.getParameter(UploadSamFileTask.PARAM_SOURCE_FILE);
        // If the task has the source dir, use it
        if (null == tmpSourceFile || "".equals(tmpSourceFile)) {
            setSourceFile(processData);
        }
        // else look in the *.process file for it
        else {
            sourceFile = FileUtil.checkFileExists(tmpSourceFile);
        }
    }

    private void setSourceFile(IProcessData processData) throws MissingDataException, IOException {
        Object sourceFileObj = processData.getMandatoryItem(UploadSamFileTask.PARAM_SOURCE_FILE);
        if (sourceFileObj instanceof String) {
            sourceFile = (FileUtil.checkFileExists((String) sourceFileObj));
        }
        else if (sourceFileObj instanceof File && ((File) sourceFileObj).isFile()) {
            sourceFile = (File) sourceFileObj;
        }
        else {
            throw new MissingDataException("Could not process sourceFileObj - expected it to be a String or File");
        }
    }

}
