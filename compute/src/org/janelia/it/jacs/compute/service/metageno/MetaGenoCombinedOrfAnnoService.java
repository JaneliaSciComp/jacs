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

package org.janelia.it.jacs.compute.service.metageno;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.ComputeBaseDAO;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.metageno.MetaGenoAnnotationTask;
import org.janelia.it.jacs.model.tasks.metageno.MetaGenoCombinedOrfAnnoTask;
import org.janelia.it.jacs.model.tasks.metageno.MetaGenoOrfCallerTask;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.metageno.MetaGenoOrfCallerResultNode;
import org.janelia.it.jacs.shared.node.FastaUtil;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Jul 20, 2009
 * Time: 12:30:24 PM
 */
public class MetaGenoCombinedOrfAnnoService implements IService {
    Logger logger;
    boolean parentTaskErrorFlag = false;

    Task parentTask;
    MetaGenoCombinedOrfAnnoTask combinedTask;
    ComputeBeanRemote computeBean;
    String sessionName;

    public void execute(IProcessData processData) throws ServiceException {
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            logger.info(getClass().getName() + " execute() start");
            init(processData);

            String queryNodeId = combinedTask.getParameter(MetaGenoCombinedOrfAnnoTask.PARAM_input_node_id);
            String useClearRange = combinedTask.getParameter(MetaGenoCombinedOrfAnnoTask.PARAM_useClearRange);
            String project = combinedTask.getParameter(MetaGenoCombinedOrfAnnoTask.PARAM_project);

            // Step 1: Run Orf Caller
            MetaGenoOrfCallerTask orfTask = new MetaGenoOrfCallerTask();
            orfTask.setOwner(combinedTask.getOwner());
            orfTask.setParameter(MetaGenoOrfCallerTask.PARAM_input_node_id, queryNodeId);
            orfTask.setParameter(MetaGenoOrfCallerTask.PARAM_useClearRange, useClearRange);
            orfTask.setParameter(MetaGenoOrfCallerTask.PARAM_project, project);
            orfTask.setParentTaskId(combinedTask.getObjectId());
            orfTask = (MetaGenoOrfCallerTask) computeBean.saveOrUpdateTask(orfTask);
            computeBean.submitJob("MetaGenoORFCaller", orfTask.getObjectId());
            waitForTask(orfTask.getObjectId());

            // Step 2: Create input node from orf result
            MetaGenoOrfCallerResultNode orfResultNode = (MetaGenoOrfCallerResultNode) computeBean.getResultNodeByTaskId(orfTask.getObjectId());
            String orfFilepath = orfResultNode.getFilePathByTag(MetaGenoOrfCallerResultNode.TAG_ORF_OUTPUT);
            File orfResultFile = new File(orfFilepath);
            if (!orfResultFile.exists()) {
                throw new Exception("Could not locate ORF result file=" + orfResultFile.getAbsolutePath());
            }
            String orfFastaInfo = "Orf Fasta File Node for MetaGenoCombinedOrfAnnoService task=" + combinedTask.getObjectId();
            Long orfFastaFileNodeId = createPeptideFastaFileNode(combinedTask.getOwner(), orfFastaInfo, orfFastaInfo, orfResultFile.getAbsolutePath());

            // Step 3: Run Annotation Pipeline
            MetaGenoAnnotationTask annoTask = new MetaGenoAnnotationTask();
            annoTask.setOwner(combinedTask.getOwner());
            annoTask.setParameter(MetaGenoAnnotationTask.PARAM_input_node_id, orfFastaFileNodeId.toString());
            annoTask.setParameter(MetaGenoAnnotationTask.PARAM_project, project);
            annoTask.setParentTaskId(combinedTask.getObjectId());
            annoTask = (MetaGenoAnnotationTask) computeBean.saveOrUpdateTask(annoTask);
            computeBean.submitJob("MetaGenoAnnotation", annoTask.getObjectId());
            waitForTask(annoTask.getObjectId());

            logger.info(getClass().getName() + " execute() finish");
        }
        catch (Exception e) {
            if (parentTaskErrorFlag) {
                logger.info("Parent task has error -returning");
            }
            else {
                this.setParentTaskToErrorStatus(parentTask, this.getClass().getName() + " : " + e.getMessage());
                throw new ServiceException(e);
            }
        }
    }

    protected void init(IProcessData processData) throws MissingDataException, IOException {
        combinedTask = (MetaGenoCombinedOrfAnnoTask) ProcessDataHelper.getTask(processData);
        computeBean = EJBFactory.getRemoteComputeBean();
        try {
            Long parentTaskId = combinedTask.getParentTaskId();
            if (parentTaskId != null && parentTaskId != 0L) {
                parentTask = computeBean.getTaskById(parentTaskId);
            }
            if (parentTask != null && checkParentTaskForError(parentTask)) {
                parentTaskErrorFlag = true;
                throw new MissingDataException("Parent task has ERROR event");
            }
        }
        catch (Exception e) {
            throw new MissingDataException(e.getMessage());
        }
        sessionName = ProcessDataHelper.getSessionRelativePath(processData);
    }

    protected void waitForTask(Long taskId) throws Exception {
        String[] taskStatus = null;
        while (taskStatus == null || !Task.isDone(taskStatus[0])) {
            taskStatus = computeBean.getTaskStatus(taskId);
            Thread.sleep(5000);
        }
        if (!taskStatus[0].equals(Event.COMPLETED_EVENT)) {
            throw new Exception("Task " + taskId + " finished with non-complete status=" + taskStatus[0]);
        }
    }

    protected void setParentTaskToErrorStatus(Task parentTask, String message) {
        try {
            computeBean.saveEvent(parentTask.getObjectId(), Event.ERROR_EVENT, message, new Date());
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    protected boolean checkParentTaskForError(Task parentTask) {
        try {
            String[] status = computeBean.getTaskStatus(parentTask.getObjectId());
            return status[ComputeBaseDAO.STATUS_TYPE].equals(Event.ERROR_EVENT);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return true;
        }
    }

    protected Long createPeptideFastaFileNode(String user, String name, String description, String sourcePath)
            throws Exception {
        if (logger.isInfoEnabled()) logger.info("Starting createFastaFileNode() with source path: " + sourcePath);
        File sourceFile = new File(sourcePath);
        long[] sequenceCountAndTotalLength = FastaUtil.findSequenceCountAndTotalLength(sourceFile);
        FastaFileNode ffn = new FastaFileNode(user, null/*Task*/, name, description,
                Node.VISIBILITY_PUBLIC, FastaFileNode.PEPTIDE, (int) sequenceCountAndTotalLength[0], sessionName);
        ffn.setLength(sequenceCountAndTotalLength[1]);
        ffn = (FastaFileNode) EJBFactory.getRemoteComputeBean().saveOrUpdateNode(ffn);
        File ffnDir = new File(ffn.getDirectoryPath());
        ffnDir.mkdirs();
        String copyCmd = "cp " + sourcePath + " " + ffn.getFastaFilePath();
        if (logger.isInfoEnabled()) logger.info("Executing: " + copyCmd);
        SystemCall call = new SystemCall(logger);
        int exitVal = call.emulateCommandLine(copyCmd, true);
        if (logger.isInfoEnabled()) logger.info("Exit value: " + exitVal);
        return ffn.getObjectId();
    }


}
