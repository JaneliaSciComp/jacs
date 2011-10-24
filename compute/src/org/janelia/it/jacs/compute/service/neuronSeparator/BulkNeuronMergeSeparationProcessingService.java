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

package org.janelia.it.jacs.compute.service.neuronSeparator;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.neuronSeparator.BulkNeuronSeparatorTask;
import org.janelia.it.jacs.model.tasks.neuronSeparator.NeuronSeparatorPipelineTask;
import org.janelia.it.jacs.model.tasks.v3d.V3DPipelineTask;
import org.janelia.it.jacs.model.user_data.Node;

import java.io.File;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jan 20, 2009
 * Time: 11:44:08 PM
 */
public class BulkNeuronMergeSeparationProcessingService implements IService {
    private Logger logger;
    private List<String> directoryPathList = new ArrayList<String>();
    private HashSet<String> v3dTaskIdSet = new HashSet<String>();
    private HashSet<String> v3dCompletionSet = new HashSet<String>();
    private HashSet<String> neusepTaskIdSet = new HashSet<String>();
    private HashSet<String> neusepTaskCompletionSet = new HashSet<String>();
    private HashMap<String, String> v3dTaskResultNodeMap = new HashMap<String, String>();
    private BulkNeuronSeparatorTask task;

    public void execute(IProcessData processData) throws ServiceException {
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            task = (BulkNeuronSeparatorTask)ProcessDataHelper.getTask(processData);
            task.getParameter(BulkNeuronSeparatorTask.PARAM_inputDirectoryList);
            String taskInputDirectoryList = task.getParameter(BulkNeuronSeparatorTask.PARAM_inputDirectoryList);
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

            waitAndVerifyV3DCompletion();

            // Now run all the V3dData into the Neuron Separation pipeline
            for (String tmpV3dTaskId : v3dCompletionSet) {
                Task tmpTask = EJBFactory.getLocalComputeBean().getTaskById(Long.valueOf(tmpV3dTaskId));
                NeuronSeparatorPipelineTask neuTask = new NeuronSeparatorPipelineTask(new HashSet<Node>(),
                        task.getOwner(), new ArrayList<Event>(), new HashSet<TaskParameter>());
                neuTask.setParameter(NeuronSeparatorPipelineTask.PARAM_inputFilePath, tmpTask.getParameter(V3DPipelineTask.PARAM_INPUT_FILE_PATHS));
                neuTask.setJobName("Remote Neuron Separator Task");
                neuTask.setParentTaskId(tmpTask.getObjectId());
                neuTask = (NeuronSeparatorPipelineTask)EJBFactory.getLocalComputeBean().saveOrUpdateTask(neuTask);
                neusepTaskIdSet.add(neuTask.getObjectId().toString());
                EJBFactory.getLocalComputeBean().submitJob("NeuronSeparationPipelineRemote", neuTask.getObjectId());
            }

            waitAndVerifyNeuSepCompletion();
        }
        catch (Exception e) {
            throw new ServiceException("There was an error bulk processing files for the V3D pipeline", e);
        }
    }

    private void checkForDataAndStartV3D(File dir) throws DaoException {
        ArrayList<File> childFiles = new ArrayList<File>();
        File[] childItems = dir.listFiles();
        for (File childItem : childItems) {
            if (childItem.isDirectory()) {
                checkForDataAndStartV3D(childItem);
            }
            else if (childItem.getName().toLowerCase().endsWith(".lsm")){
                childFiles.add(childItem);
            }
        }

        // Check the db to see if this input has already been run


        if (childFiles.size()==2){
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

    private void waitAndVerifyV3DCompletion() throws Exception {
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

    // todo This needs to be a generic pipeline method that all services can use
    private void waitAndVerifyNeuSepCompletion() throws Exception {
        boolean allComplete = false;
        if (neusepTaskIdSet.size()!=0) {
            logger.debug("\n\nWaiting for processing of "+neusepTaskIdSet.size()+" Neuron Separation pipelines.");
            EJBFactory.getLocalComputeBean().saveEvent(task.getObjectId(), "Waiting for Neuron Separation Processing",
                    "Waiting for Neuron Separation Processing", new Date());
        }
        else {
//            logger.debug("No Neuron Separation pipelines processing."); // Would be too verbose every 5 seconds
            return;
        }
        while (!allComplete && neusepTaskIdSet.size()>0) {
            for (String tmpTaskId : neusepTaskIdSet) {
                if (!neusepTaskCompletionSet.contains(tmpTaskId)) {
                    String[] statusTypeAndValue = EJBFactory.getLocalComputeBean().getTaskStatus(Long.valueOf(tmpTaskId));
                    if (Task.isDone(statusTypeAndValue[0])) {
                        neusepTaskCompletionSet.add(tmpTaskId);
                    }
                }
                if (neusepTaskIdSet.size()==neusepTaskCompletionSet.size()) {
                    allComplete = true;
                }
                else {
                    Thread.sleep(5000);
                }
            }
        }
        logger.debug("\n\nBulk Neuron Separation pipeline processing complete.");
        EJBFactory.getLocalComputeBean().saveEvent(task.getObjectId(), "Bulk Neuron Separation Processing Complete",
                "Bulk Neuron Separation Processing Complete", new Date());
    }
}