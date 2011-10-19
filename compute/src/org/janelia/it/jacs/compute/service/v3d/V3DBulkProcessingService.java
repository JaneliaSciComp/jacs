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

package org.janelia.it.jacs.compute.service.v3d;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.fileDiscovery.MCFOUnifiedFileDiscoveryTask;
import org.janelia.it.jacs.model.tasks.v3d.V3DPipelineTask;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jan 20, 2009
 * Time: 11:44:08 PM
 */
public class V3DBulkProcessingService implements IService {
    private Logger logger;
    private List<String> directoryPathList = new ArrayList<String>();
    private HashSet<String> v3dTaskIdSet = new HashSet<String>();
    private HashSet<String> v3dCompletionSet = new HashSet<String>();
    private MCFOUnifiedFileDiscoveryTask task;

    public void execute(IProcessData processData) throws ServiceException {
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            task = (MCFOUnifiedFileDiscoveryTask)ProcessDataHelper.getTask(processData);
            task.getParameter(MCFOUnifiedFileDiscoveryTask.PARAM_inputDirectoryList);
            String taskInputDirectoryList = task.getParameter(MCFOUnifiedFileDiscoveryTask.PARAM_inputDirectoryList);
            if (taskInputDirectoryList != null) {
                String[] directoryArray = taskInputDirectoryList.split(",");
                for (String d : directoryArray) {
                    String trimmedPath=d.trim();
                    if (trimmedPath.length()>0) {
                        directoryPathList.add(trimmedPath);
                    }
                }
            }

            if (directoryPathList.isEmpty()) {
            	throw new Exception("No input directories provided");
            }

            for (String directoryPath : directoryPathList) {
                logger.info("MCFOUnifiedFileDiscoveryService including directory = "+directoryPath);
            }

            for (String directoryPath : directoryPathList) {
                logger.info("Processing dir="+directoryPath);
                File dir = new File(directoryPath);
                if (!dir.exists()) {
                    logger.error("Directory "+dir.getAbsolutePath()+" does not exist - skipping");
                }
                else if (!dir.isDirectory()) {
                    logger.error(("File " + dir.getAbsolutePath()+ " is not a directory - skipping"));
                }
                else {
                    checkForDataAndStartV3D(dir);
                }
            }

            waitAndVerifyCompletion();
        }
        catch (Exception e) {
            throw new ServiceException("There was an error bulk processing files for the V3D pipeline", e);
        }
    }

    private void checkForDataAndStartV3D(File dir) throws DaoException {
        File[] childFiles = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.toLowerCase().endsWith(".lsm");
            }
        });
        if (childFiles.length==2){
            V3DPipelineTask newV3dTask = new V3DPipelineTask(null, task.getOwner(), null, null, true, false, false,
                    dir.getAbsolutePath());
            newV3dTask.setParentTaskId(task.getObjectId());
            newV3dTask.setJobName("V3D Pipeline for "+dir.getName());
            newV3dTask = (V3DPipelineTask) EJBFactory.getLocalComputeBean().saveOrUpdateTask(newV3dTask);
            v3dTaskIdSet.add(newV3dTask.getObjectId().toString());
            logger.debug("Starting V3DPipeline for dir="+dir.getAbsolutePath());
            EJBFactory.getLocalComputeBean().submitJob("V3DPipeline", newV3dTask.getObjectId());
        }
    }

    private void waitAndVerifyCompletion() throws Exception {
        boolean allComplete = false;
        if (v3dTaskIdSet.size()!=0) {
            logger.debug("\n\nWaiting for processing of "+v3dTaskIdSet.size()+" V3D pipelines.");
            EJBFactory.getLocalComputeBean().saveEvent(task.getObjectId(), "Waiting for V3D Processing",
                    "Waiting for V3D Processing", new Date());
        }
        else {
//            logger.debug("No V3D pipelines processing."); // Would be too verbose every 5 seconds
            return;
        }
        while (!allComplete && v3dTaskIdSet.size()>0) {
            for (String tmpTaskId : v3dTaskIdSet) {
                if (!v3dCompletionSet.contains(tmpTaskId)) {
                    String[] statusTypeAndValue = EJBFactory.getLocalComputeBean().getTaskStatus(Long.valueOf(tmpTaskId));
                    if (Task.isDone(statusTypeAndValue[0])) {
                        v3dCompletionSet.add(tmpTaskId);
                    }
                }
                if (v3dTaskIdSet.size()==v3dCompletionSet.size()) {
                    allComplete = true;
                }
                else {
                    Thread.sleep(5000);
                }
            }
        }
        logger.debug("\n\nV3D pipeline processing complete.");
        EJBFactory.getLocalComputeBean().saveEvent(task.getObjectId(), "V3D Processing Complete",
                "V3D Processing Complete", new Date());
    }

}