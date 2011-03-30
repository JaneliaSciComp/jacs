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
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.SubmitJobException;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Nov 3, 2008
 * Time: 3:59:29 PM
 */
public class CreateOutputDirsService implements IService {

    protected Logger logger;

    protected List<File> outputDirs = new ArrayList<File>();
    protected List<File> queryFiles = new ArrayList<File>();
    protected FileNode resultFileNode;
    protected IProcessData processData;
    protected Task task;
    protected HashMap<File, File> inputOutputDirMap = new HashMap<File, File>();

    public void execute(IProcessData processData) throws SubmitJobException {
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            init(processData);
            createOutputDirs();
            processData.putItem(FileServiceConstants.OUTPUT_DIR_LIST, outputDirs);
            // THIS SHOULD ALREADY BE IN processData - so commenting this out
            // processData.putItem(FileServiceConstants.POST_SPLIT_INPUT_FILE_LIST, queryFiles);
            processData.putItem(FileServiceConstants.INPUT_OUTPUT_DIR_MAP, inputOutputDirMap);
        }
        catch (Exception e) {
            throw new SubmitJobException(e);
        }
    }

    protected void createOutputDirs() throws Exception {
        queryFiles = getQueryFiles();
        if (queryFiles == null || queryFiles.size() == 0)
            logger.error("CreateOutputDirsService createOutputDirs queryFile is null or empty");
        for (File queryFile : queryFiles) {
            File outputDir = createOutputDir(queryFile);
            outputDirs.add(outputDir);
            inputOutputDirMap.put(queryFile, outputDir);
            doAdditionalIntegrationPerInputOutput(queryFile, outputDir);
        }
    }

    protected void doAdditionalIntegrationPerInputOutput(File queryFile, File outputDir) throws Exception {
        // can be overridden
    }

    protected void init(IProcessData processData) throws MissingDataException {
        this.processData = processData;
        this.task = ProcessDataHelper.getTask(processData);
        // Needs to run in separate transaction
        this.resultFileNode = ProcessDataHelper.getResultFileNode(processData);
        if (resultFileNode == null) {
            throw new MissingDataException("ResultFileNode for createtask " + task.getObjectId() +
                    " must exist before a grid job is submitted");
        }
    }

    private List<File> getQueryFiles() throws MissingDataException {
        List<File> queryFiles = new ArrayList<File>();
        Object obj = processData.getMandatoryItem(FileServiceConstants.POST_SPLIT_INPUT_FILE_LIST);
        if (obj instanceof List) {
            queryFiles.addAll((List) obj);
        }
        else {
            queryFiles.add((File) obj);
        }
        return queryFiles;
    }

    private File createOutputDir(File queryFile) throws MissingDataException, IOException {
        String outputDirPath = FileUtil.checkFilePath(resultFileNode.getDirectoryPath()) + File.separator + "r_" + queryFile.getName();
        logger.debug("createOutputDir creating directory=" + outputDirPath);
        return FileUtil.ensureDirExists(outputDirPath);
    }


}
