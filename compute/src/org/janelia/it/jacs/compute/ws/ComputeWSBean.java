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

package org.janelia.it.jacs.compute.ws;

import org.jboss.annotation.ejb.PoolClass;
import org.jboss.annotation.ejb.TransactionTimeout;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.TextFileIO;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.SessionTask;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.ap16s.AnalysisPipeline16sTask;
import org.janelia.it.jacs.model.tasks.blast.*;
import org.janelia.it.jacs.model.tasks.eukAnnotation.EAPTask;
import org.janelia.it.jacs.model.tasks.genomeProject.GenomeProjectUpdateTask;
import org.janelia.it.jacs.model.tasks.hmmer.HmmpfamTask;
import org.janelia.it.jacs.model.tasks.hmmer3.HMMER3Task;
import org.janelia.it.jacs.model.tasks.inspect.InspectTask;
import org.janelia.it.jacs.model.tasks.metageno.*;
import org.janelia.it.jacs.model.tasks.psiBlast.ReversePsiBlastTask;
import org.janelia.it.jacs.model.tasks.rnaSeq.*;
import org.janelia.it.jacs.model.tasks.sift.SiftProteinSubstitutionTask;
import org.janelia.it.jacs.model.tasks.tools.*;
import org.janelia.it.jacs.model.tasks.utility.*;
import org.janelia.it.jacs.model.user_data.*;
import org.janelia.it.jacs.model.user_data.blast.BlastDatabaseFileNode;
import org.janelia.it.jacs.model.user_data.blast.BlastResultFileNode;
import org.janelia.it.jacs.model.user_data.hmmer.HmmerPfamDatabaseNode;
import org.janelia.it.jacs.model.user_data.hmmer.HmmerPfamResultNode;
import org.janelia.it.jacs.model.user_data.metageno.*;
import org.janelia.it.jacs.model.user_data.reversePsiBlast.ReversePsiBlastDatabaseNode;
import org.janelia.it.jacs.model.user_data.rnaSeq.CufflinksResultNode;
import org.janelia.it.jacs.model.user_data.rnaSeq.RnaSeqReferenceGenomeNode;
import org.janelia.it.jacs.model.user_data.rnaSeq.TophatResultNode;
import org.janelia.it.jacs.model.user_data.tools.GenericServiceDefinitionNode;
import org.janelia.it.jacs.model.user_data.tools.GenezillaIsoFileNode;
import org.janelia.it.jacs.model.vo.MultiSelectVO;
import org.janelia.it.jacs.shared.node.FastaUtil;
import org.janelia.it.jacs.shared.utils.FileUtil;

import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Sep 11, 2008
 * Time: 11:42:11 AM
 * // todo Validate username and security token - HTTP web service authentication?
 * <p/>
 * $Id: ComputeWSBean.java 1 2011-02-16 21:07:19Z tprindle $
 */
@SOAPBinding(style = SOAPBinding.Style.RPC)
@WebService(endpointInterface = "org.janelia.it.jacs.compute.ws.ComputeWS")
@Stateless(name = "ComputeWS")
@Remote(ComputeWS.class)
@TransactionTimeout(432000)
@PoolClass(value = org.jboss.ejb3.StrictMaxPool.class, maxSize = 100, timeout = 10000)
public class ComputeWSBean extends BaseWSBean {

    // These methods allow someone running an external pipeline to bin their work into a common area

    public String createWorkSession(@WebParam(name = "username") String username,
                                    @WebParam(name = "token") String token,
                                    @WebParam(name = "sessionName") String sessionName) {
        logger.debug("Web Services - createWorkSession() acknowledged");
        SessionTask tmpSessionTask = new SessionTask(null, username, null, null);
        tmpSessionTask.setJobName(sessionName);
        StringBuffer sbuf = new StringBuffer("");
        ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
        try {
            // Save the job before running
            tmpSessionTask = (SessionTask) computeBean.saveOrUpdateTask(tmpSessionTask);
            // Fire off the process
            computeBean.submitJob("CreateSession", tmpSessionTask.getObjectId());
            sbuf.append("Session Id: ").append(tmpSessionTask.getObjectId().toString()).append("\n");
        }
        catch (Exception e) {
            e.printStackTrace();
            String error = "There was a problem creating a session via the web service.\nContact an administrator.";
            sbuf = new StringBuffer(error);
            logTaskError(computeBean, e, tmpSessionTask.getObjectId(), error);
        }
        logger.debug("Web Services - createWorkSession() complete");
        return sbuf.toString();
    }

    // This method copies all data from the local session FileNode to the user's directory

    public String exportWorkFromSession(@WebParam(name = "username") String username,
                                        @WebParam(name = "token") String token,
                                        @WebParam(name = "workSessionId") String workSessionId,
                                        @WebParam(name = "finalOutputDirectory") String finalOutputDirectory) {
        logger.debug("Web Services - exportWorkFromSession() acknowledged");
        StringBuffer sbuf = new StringBuffer("");
        ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
        CopyFileTask copyTask = null;
        try {
            FileNode tmpNode = (FileNode) computeBean.getResultNodeByTaskId(Long.valueOf(workSessionId));
            copyTask = new CopyFileTask(null, username, null, null, tmpNode.getDirectoryPath(), finalOutputDirectory);
            // Save the job before running
            copyTask = (CopyFileTask) computeBean.saveOrUpdateTask(copyTask);
            // Fire off the process
            computeBean.submitJob("CopyDirectory", copyTask.getObjectId());
            sbuf.append("Job Id: ").append(copyTask.getObjectId().toString()).append("\n");
        }
        catch (Exception e) {
            e.printStackTrace();
            String error = "There was a problem running the job via the web service.\nContact an administrator.";
            sbuf = new StringBuffer(error);
            if (null != copyTask) {
                logTaskError(computeBean, e, copyTask.getObjectId(), error);
            }
        }
        return sbuf.toString();
    }

    // This removes all files under the FileNode associated with the session (workflow task)

    public String deleteAllWorkForSession(@WebParam(name = "username") String username,
                                          @WebParam(name = "token") String token,
                                          @WebParam(name = "workSessionId") String workSessionId) {
        logger.debug("Web Services - deleteAllWorkForSession() acknowledged");
        String returnValue = deleteTaskById(username, token, workSessionId);
        logger.debug("Web Services - deleteAllWorkForSession() complete");
        return returnValue;
    }

    /**
     * Service to execute a blast job.
     * We can return the blast task id or the path to the results (or both)
     *
     * @return the task identifier so the user can check status
     */
    public String runBlastN(@WebParam(name = "username") String username,
                            @WebParam(name = "token") String token,
                            @WebParam(name = "project") String project,
                            @WebParam(name = "workSessionId") String workSessionId,
                            @WebParam(name = "jobName") String jobName,
                            @WebParam(name = "subjectDBIdentifier") String subjectDBIdentifier,
                            @WebParam(name = "queryFastaFileNodeId") String queryFastaFileNodeId,
                            @WebParam(name = "databaseAlignmentsPerQuery") int databaseAlignmentsPerQuery,
                            @WebParam(name = "filter") String filter,
                            @WebParam(name = "eValueExponent") int eValueExponent,
                            @WebParam(name = "lowercaseFiltering") boolean lowercaseFiltering,
                            @WebParam(name = "believeDefline") boolean believeDefline,
                            @WebParam(name = "databaseSize") long databaseSize,
                            @WebParam(name = "gapExtendCost") int gapExtendCost,
                            @WebParam(name = "gappedAlignment") boolean gappedAlignment,
                            @WebParam(name = "hitExtensionThreshold") int hitExtensionThreshold,
                            @WebParam(name = "matrix") String matrix,
                            @WebParam(name = "multihitWindowSize") int multihitWindowSize,
                            @WebParam(name = "searchStrand") String searchStrand,
                            @WebParam(name = "ungappedExtensionDropoff") int ungappedExtensionDropoff,
                            @WebParam(name = "bestHitsToKeep") int bestHitsToKeep,
                            @WebParam(name = "finalGappedDropoff") int finalGappedDropoff,
                            @WebParam(name = "gapOpenCost") int gapOpenCost,
                            @WebParam(name = "gappedAlignmentDropoff") int gappedAlignmentDropoff,
                            @WebParam(name = "matchReward") int matchReward,
                            @WebParam(name = "mismatchPenalty") int mismatchPenalty,
                            @WebParam(name = "searchSize") int searchSize,
                            @WebParam(name = "showGIs") boolean showGIs,
                            @WebParam(name = "wordsize") int wordsize,
                            @WebParam(name = "formatTypesCsv") String formatTypesCsv) {
        logger.debug("Web Services - runBlastN() acknowledged");
        BlastNTask blastnTask = new BlastNTask();
        blastnTask.setOwner(username);
        blastnTask.setJobName(jobName);
        blastnTask.setParameter(BlastNTask.PARAM_query, queryFastaFileNodeId);
        MultiSelectVO ms = new MultiSelectVO();
        ArrayList<String> dbList = new ArrayList<String>();
        dbList.add(subjectDBIdentifier);
        ms.setPotentialChoices(dbList);
        ms.setActualUserChoices(dbList);
        blastnTask.setParameter(BlastTask.PARAM_project, project);
        if (null != workSessionId && !"".equals(workSessionId)) {
            blastnTask.setParentTaskId(Long.valueOf(workSessionId));
        }
        blastnTask.setParameter(BlastTask.PARAM_subjectDatabases, Task.csvStringFromCollection(ms.getActualUserChoices()));
        blastnTask.setParameter(BlastTask.PARAM_databaseAlignments, Integer.toString(databaseAlignmentsPerQuery));
        blastnTask.setParameter(BlastTask.PARAM_filter, filter);
        blastnTask.setParameter(BlastTask.PARAM_evalue, Integer.toString(eValueExponent));
        blastnTask.setParameter(BlastTask.PARAM_lowerCaseFiltering, Boolean.toString(lowercaseFiltering));

        blastnTask.setParameter(BlastTask.PARAM_believeDefline, Boolean.toString(believeDefline));
        blastnTask.setParameter(BlastTask.PARAM_databaseSize, Long.toString(databaseSize));
        blastnTask.setParameter(BlastTask.PARAM_gapExtendCost, Integer.toString(gapExtendCost));
        blastnTask.setParameter(BlastNTask.PARAM_gappedAlignment, Boolean.toString(gappedAlignment));
        blastnTask.setParameter(BlastTask.PARAM_hitExtensionThreshold, Integer.toString(hitExtensionThreshold));
        blastnTask.setParameter(BlastTask.PARAM_matrix, matrix);
        blastnTask.setParameter(BlastTask.PARAM_multiHitWindowSize, Integer.toString(multihitWindowSize));
        blastnTask.setParameter(BlastNTask.PARAM_searchStrand, searchStrand);
        blastnTask.setParameter(BlastTask.PARAM_ungappedExtensionDropoff, Integer.toString(ungappedExtensionDropoff));
        blastnTask.setParameter(BlastTask.PARAM_bestHitsToKeep, Integer.toString(bestHitsToKeep));
        blastnTask.setParameter(BlastTask.PARAM_finalGappedDropoff, Integer.toString(finalGappedDropoff));
        blastnTask.setParameter(BlastTask.PARAM_gapOpenCost, Integer.toString(gapOpenCost));
        blastnTask.setParameter(BlastTask.PARAM_gappedAlignmentDropoff, Integer.toString(gappedAlignmentDropoff));
        blastnTask.setParameter(BlastNTask.PARAM_matchReward, Integer.toString(matchReward));
        blastnTask.setParameter(BlastNTask.PARAM_mismatchPenalty, Integer.toString(mismatchPenalty));
        blastnTask.setParameter(BlastTask.PARAM_searchSize, Integer.toString(searchSize));
        blastnTask.setParameter(BlastTask.PARAM_showGIs, Boolean.toString(showGIs));
        blastnTask.setParameter(BlastTask.PARAM_wordsize, Integer.toString(wordsize));
        blastnTask.setParameter(BlastTask.PARAM_formatTypesCsv, formatTypesCsv);
        try {
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            computeBean.validateBlastTaskQueryDatabaseMatch(blastnTask);
        }
        catch (Exception e) {
            logger.error(e.getMessage());
            return e.getMessage() + "\n";
        }
        logger.debug("Web Services - runBlastN() complete");
        return saveAndSubmitJob(blastnTask, "BlastWithGridMerge");
    }


    public String runVTeraBlastN(@WebParam(name = "username") String username,
                                 @WebParam(name = "token") String token,
                                 @WebParam(name = "project") String project,
                                 @WebParam(name = "workSessionId") String workSessionId,
                                 @WebParam(name = "jobName") String jobName,
                                 @WebParam(name = "subjectDBIdentifier") String subjectDBIdentifier,
                                 @WebParam(name = "queryFastaFileNodeId") String queryFastaFileNodeId,
                                 @WebParam(name = "databaseAlignmentsPerQuery") int databaseAlignmentsPerQuery,
                                 @WebParam(name = "filter") String filter,
                                 @WebParam(name = "eValueExponent") int eValueExponent,
                                 @WebParam(name = "lowercaseFiltering") boolean lowercaseFiltering,
                                 @WebParam(name = "believeDefline") boolean believeDefline,
                                 @WebParam(name = "databaseSize") long databaseSize,
                                 @WebParam(name = "gapExtendCost") int gapExtendCost,
                                 @WebParam(name = "gappedAlignment") boolean gappedAlignment,
                                 @WebParam(name = "hitExtensionThreshold") int hitExtensionThreshold,
                                 @WebParam(name = "matrix") String matrix,
                                 @WebParam(name = "multihitWindowSize") int multihitWindowSize,
                                 @WebParam(name = "searchStrand") String searchStrand,
                                 @WebParam(name = "ungappedExtensionDropoff") int ungappedExtensionDropoff,
                                 @WebParam(name = "bestHitsToKeep") int bestHitsToKeep,
                                 @WebParam(name = "finalGappedDropoff") int finalGappedDropoff,
                                 @WebParam(name = "gapOpenCost") int gapOpenCost,
                                 @WebParam(name = "gappedAlignmentDropoff") int gappedAlignmentDropoff,
                                 @WebParam(name = "matchReward") int matchReward,
                                 @WebParam(name = "mismatchPenalty") int mismatchPenalty,
                                 @WebParam(name = "searchSize") int searchSize,
                                 @WebParam(name = "showGIs") boolean showGIs,
                                 @WebParam(name = "wordsize") int wordsize,
                                 @WebParam(name = "formatTypesCsv") String formatTypesCsv) throws RemoteException {
        logger.debug("Web Services - runVTeraBlastN() acknowledged");
        TeraBlastNTask blastnTask = new TeraBlastNTask();
        blastnTask.setOwner(username);
        blastnTask.setJobName(jobName);
        blastnTask.setParameter(BlastNTask.PARAM_query, queryFastaFileNodeId);
        MultiSelectVO ms = new MultiSelectVO();
        ArrayList<String> dbList = new ArrayList<String>();
        dbList.add(subjectDBIdentifier);
        ms.setPotentialChoices(dbList);
        ms.setActualUserChoices(dbList);
        blastnTask.setParameter(BlastTask.PARAM_project, project);
        if (null != workSessionId && !"".equals(workSessionId)) {
            blastnTask.setParentTaskId(Long.valueOf(workSessionId));
        }
        blastnTask.setParameter(BlastTask.PARAM_subjectDatabases, Task.csvStringFromCollection(ms.getActualUserChoices()));
        blastnTask.setParameter(BlastTask.PARAM_databaseAlignments, Integer.toString(databaseAlignmentsPerQuery));
        blastnTask.setParameter(BlastTask.PARAM_filter, filter);
        blastnTask.setParameter(BlastTask.PARAM_evalue, Integer.toString(eValueExponent));
        blastnTask.setParameter(BlastTask.PARAM_lowerCaseFiltering, Boolean.toString(lowercaseFiltering));

        blastnTask.setParameter(BlastTask.PARAM_believeDefline, Boolean.toString(believeDefline));
        blastnTask.setParameter(BlastTask.PARAM_databaseSize, Long.toString(databaseSize));
        blastnTask.setParameter(BlastTask.PARAM_gapExtendCost, Integer.toString(gapExtendCost));
        blastnTask.setParameter(BlastTask.PARAM_gappedAlignment, Boolean.toString(gappedAlignment));
        blastnTask.setParameter(BlastTask.PARAM_hitExtensionThreshold, Integer.toString(hitExtensionThreshold));
        blastnTask.setParameter(BlastTask.PARAM_matrix, matrix);
        blastnTask.setParameter(BlastTask.PARAM_multiHitWindowSize, Integer.toString(multihitWindowSize));
        blastnTask.setParameter(BlastTask.PARAM_searchStrand, searchStrand);
        blastnTask.setParameter(BlastTask.PARAM_ungappedExtensionDropoff, Integer.toString(ungappedExtensionDropoff));
        blastnTask.setParameter(BlastTask.PARAM_bestHitsToKeep, Integer.toString(bestHitsToKeep));
        blastnTask.setParameter(BlastTask.PARAM_finalGappedDropoff, Integer.toString(finalGappedDropoff));
        blastnTask.setParameter(BlastTask.PARAM_gapOpenCost, Integer.toString(gapOpenCost));
        blastnTask.setParameter(BlastTask.PARAM_gappedAlignmentDropoff, Integer.toString(gappedAlignmentDropoff));
        blastnTask.setParameter(BlastTask.PARAM_matchReward, Integer.toString(matchReward));
        blastnTask.setParameter(BlastTask.PARAM_mismatchPenalty, Integer.toString(mismatchPenalty));
        blastnTask.setParameter(BlastTask.PARAM_searchSize, Integer.toString(searchSize));
        blastnTask.setParameter(BlastTask.PARAM_showGIs, Boolean.toString(showGIs));
        blastnTask.setParameter(BlastTask.PARAM_wordsize, Integer.toString(wordsize));
        blastnTask.setParameter(BlastTask.PARAM_formatTypesCsv, formatTypesCsv);
        try {
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            computeBean.validateBlastTaskQueryDatabaseMatch(blastnTask);
        }
        catch (Exception e) {
            logger.error(e.getMessage());
            return e.getMessage() + "\n";
        }
        logger.debug("Web Services - runVTeraBlastN() complete");
        return saveAndSubmitJob(blastnTask, "TeraBlast");
    }

    public String runMegaBlast(@WebParam(name = "username") String username,
                               @WebParam(name = "token") String token,
                               @WebParam(name = "project") String project,
                               @WebParam(name = "workSessionId") String workSessionId,
                               @WebParam(name = "jobName") String jobName,
                               @WebParam(name = "subjectDBIdentifier") String subjectDBIdentifier,
                               @WebParam(name = "queryFastaFileNodeId") String queryFastaFileNodeId,
                               @WebParam(name = "databaseAlignmentsPerQuery") int databaseAlignmentsPerQuery,
                               @WebParam(name = "filter") String filter,
                               @WebParam(name = "eValueExponent") int eValueExponent,
                               @WebParam(name = "lowercaseFiltering") boolean lowercaseFiltering,
                               @WebParam(name = "believeDefline") boolean believeDefline,
                               @WebParam(name = "databaseSize") long databaseSize,
                               @WebParam(name = "gapExtendCost") int gapExtendCost,
                               @WebParam(name = "gappedAlignment") boolean gappedAlignment,
                               @WebParam(name = "hitExtensionThreshold") int hitExtensionThreshold,
                               @WebParam(name = "matrix") String matrix,
                               @WebParam(name = "multihitWindowSize") int multihitWindowSize,
                               @WebParam(name = "showGIs") boolean showGIs,
                               @WebParam(name = "wordsize") int wordsize,
                               @WebParam(name = "bestHitsToKeep") int bestHitsToKeep,
                               @WebParam(name = "finalGappedDropoff") int finalGappedDropoff,
                               @WebParam(name = "gapOpenCost") int gapOpenCost,
                               @WebParam(name = "gappedAlignmentDropoff") int gappedAlignmentDropoff,
                               @WebParam(name = "matchReward") int matchReward,
                               @WebParam(name = "mismatchPenalty") int mismatchPenalty,
                               @WebParam(name = "searchSize") int searchSize,
                               @WebParam(name = "ungappedExtensionDropoff") int ungappedExtensionDropoff,
                               @WebParam(name = "formatTypesCsv") String formatTypesCsv) {
        logger.debug("Web Services - runMegaBlast() acknowledged");
        MegablastTask megaBlastTask = new MegablastTask();
        megaBlastTask.setOwner(username);
        megaBlastTask.setJobName(jobName);
        megaBlastTask.setParameter(BlastTask.PARAM_query, queryFastaFileNodeId);
        MultiSelectVO ms = new MultiSelectVO();
        ArrayList<String> dbList = new ArrayList<String>();
        dbList.add(subjectDBIdentifier);
        ms.setPotentialChoices(dbList);
        ms.setActualUserChoices(dbList);
        megaBlastTask.setParameter(BlastTask.PARAM_project, project);
        if (null != workSessionId && !"".equals(workSessionId)) {
            megaBlastTask.setParentTaskId(Long.valueOf(workSessionId));
        }
        megaBlastTask.setParameter(BlastTask.PARAM_subjectDatabases, Task.csvStringFromCollection(ms.getActualUserChoices()));
        megaBlastTask.setParameter(BlastTask.PARAM_databaseAlignments, Integer.toString(databaseAlignmentsPerQuery));
        megaBlastTask.setParameter(BlastTask.PARAM_filter, filter);
        megaBlastTask.setParameter(BlastTask.PARAM_evalue, Integer.toString(eValueExponent));
        megaBlastTask.setParameter(BlastTask.PARAM_lowerCaseFiltering, Boolean.toString(lowercaseFiltering));

        megaBlastTask.setParameter(BlastTask.PARAM_believeDefline, Boolean.toString(believeDefline));
        megaBlastTask.setParameter(BlastTask.PARAM_databaseSize, Long.toString(databaseSize));
        megaBlastTask.setParameter(BlastTask.PARAM_gapExtendCost, Integer.toString(gapExtendCost));
        megaBlastTask.setParameter(MegablastTask.PARAM_gappedAlignment, Boolean.toString(gappedAlignment));
        megaBlastTask.setParameter(BlastTask.PARAM_hitExtensionThreshold, Integer.toString(hitExtensionThreshold));
        megaBlastTask.setParameter(BlastTask.PARAM_matrix, matrix);
        megaBlastTask.setParameter(BlastTask.PARAM_multiHitWindowSize, Integer.toString(multihitWindowSize));
        megaBlastTask.setParameter(BlastTask.PARAM_ungappedExtensionDropoff, Integer.toString(ungappedExtensionDropoff));
        megaBlastTask.setParameter(BlastTask.PARAM_bestHitsToKeep, Integer.toString(bestHitsToKeep));
        megaBlastTask.setParameter(BlastTask.PARAM_finalGappedDropoff, Integer.toString(finalGappedDropoff));
        megaBlastTask.setParameter(BlastTask.PARAM_gapOpenCost, Integer.toString(gapOpenCost));
        megaBlastTask.setParameter(BlastTask.PARAM_gappedAlignmentDropoff, Integer.toString(gappedAlignmentDropoff));
        megaBlastTask.setParameter(MegablastTask.PARAM_matchReward, Integer.toString(matchReward));
        megaBlastTask.setParameter(MegablastTask.PARAM_mismatchPenalty, Integer.toString(mismatchPenalty));
        megaBlastTask.setParameter(BlastTask.PARAM_searchSize, Integer.toString(searchSize));
        megaBlastTask.setParameter(BlastTask.PARAM_showGIs, Boolean.toString(showGIs));
        megaBlastTask.setParameter(BlastTask.PARAM_wordsize, Integer.toString(wordsize));
        megaBlastTask.setParameter(BlastTask.PARAM_formatTypesCsv, formatTypesCsv);
        try {
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            computeBean.validateBlastTaskQueryDatabaseMatch(megaBlastTask);
        }
        catch (Exception e) {
            logger.error(e.getMessage());
            return e.getMessage() + "\n";
        }
        logger.debug("Web Services - runMegaBlast() complete");
        return saveAndSubmitJob(megaBlastTask, "BlastWithGridMerge");
    }

    public String runTBlastX(@WebParam(name = "username") String username,
                             @WebParam(name = "token") String token,
                             @WebParam(name = "project") String project,
                             @WebParam(name = "workSessionId") String workSessionId,
                             @WebParam(name = "jobName") String jobName,
                             @WebParam(name = "subjectDBIdentifier") String subjectDBIdentifier,
                             @WebParam(name = "queryFastaFileNodeId") String queryFastaFileNodeId,
                             @WebParam(name = "databaseAlignmentsPerQuery") int databaseAlignmentsPerQuery,
                             @WebParam(name = "filter") String filter,
                             @WebParam(name = "eValueExponent") int eValueExponent,
                             @WebParam(name = "lowercaseFiltering") boolean lowercaseFiltering,
                             @WebParam(name = "believeDefline") boolean believeDefline,
                             @WebParam(name = "databaseSize") long databaseSize,
                             @WebParam(name = "gapExtendCost") int gapExtendCost,
                             @WebParam(name = "gappedAlignmentDropoff") int gappedAlignmentDropoff,
                             @WebParam(name = "matrix") String matrix,
                             @WebParam(name = "searchSize") int searchSize,
                             @WebParam(name = "showGIs") boolean showGIs,
                             @WebParam(name = "wordsize") int wordsize,
                             @WebParam(name = "bestHitsToKeep") int bestHitsToKeep,
                             @WebParam(name = "finalGappedDropoff") int finalGappedDropoff,
                             @WebParam(name = "gapOpenCost") int gapOpenCost,
                             @WebParam(name = "hitExtensionThreshold") int hitExtensionThreshold,
                             @WebParam(name = "multihitWindowSize") int multihitWindowSize,
                             @WebParam(name = "searchStrand") String searchStrand,
                             @WebParam(name = "ungappedExtensionDropoff") int ungappedExtensionDropoff,
                             @WebParam(name = "formatTypesCsv") String formatTypesCsv) {
        logger.debug("Web Services - runTBlastX() acknowledged");
        TBlastXTask tBlastXTask = new TBlastXTask();
        tBlastXTask.setOwner(username);
        tBlastXTask.setJobName(jobName);
        tBlastXTask.setParameter(BlastNTask.PARAM_query, queryFastaFileNodeId);
        MultiSelectVO ms = new MultiSelectVO();
        ArrayList<String> dbList = new ArrayList<String>();
        dbList.add(subjectDBIdentifier);
        ms.setPotentialChoices(dbList);
        ms.setActualUserChoices(dbList);
        tBlastXTask.setParameter(BlastTask.PARAM_project, project);
        if (null != workSessionId && !"".equals(workSessionId)) {
            tBlastXTask.setParentTaskId(Long.valueOf(workSessionId));
        }
        tBlastXTask.setParameter(BlastTask.PARAM_subjectDatabases, Task.csvStringFromCollection(ms.getActualUserChoices()));
        tBlastXTask.setParameter(BlastTask.PARAM_databaseAlignments, Integer.toString(databaseAlignmentsPerQuery));
        tBlastXTask.setParameter(BlastTask.PARAM_filter, filter);
        tBlastXTask.setParameter(BlastTask.PARAM_evalue, Integer.toString(eValueExponent));
        tBlastXTask.setParameter(BlastTask.PARAM_lowerCaseFiltering, Boolean.toString(lowercaseFiltering));

        tBlastXTask.setParameter(BlastTask.PARAM_believeDefline, Boolean.toString(believeDefline));
        tBlastXTask.setParameter(BlastTask.PARAM_databaseSize, Long.toString(databaseSize));
        tBlastXTask.setParameter(BlastTask.PARAM_gapExtendCost, Integer.toString(gapExtendCost));
        tBlastXTask.setParameter(BlastTask.PARAM_hitExtensionThreshold, Integer.toString(hitExtensionThreshold));
        tBlastXTask.setParameter(BlastTask.PARAM_matrix, matrix);
        tBlastXTask.setParameter(BlastTask.PARAM_multiHitWindowSize, Integer.toString(multihitWindowSize));
        tBlastXTask.setParameter(BlastNTask.PARAM_searchStrand, searchStrand);
        tBlastXTask.setParameter(BlastTask.PARAM_ungappedExtensionDropoff, Integer.toString(ungappedExtensionDropoff));
        tBlastXTask.setParameter(BlastTask.PARAM_bestHitsToKeep, Integer.toString(bestHitsToKeep));
        tBlastXTask.setParameter(BlastTask.PARAM_finalGappedDropoff, Integer.toString(finalGappedDropoff));
        tBlastXTask.setParameter(BlastTask.PARAM_gapOpenCost, Integer.toString(gapOpenCost));
        tBlastXTask.setParameter(BlastTask.PARAM_gappedAlignmentDropoff, Integer.toString(gappedAlignmentDropoff));
        tBlastXTask.setParameter(BlastTask.PARAM_searchSize, Integer.toString(searchSize));
        tBlastXTask.setParameter(BlastTask.PARAM_showGIs, Boolean.toString(showGIs));
        tBlastXTask.setParameter(BlastTask.PARAM_wordsize, Integer.toString(wordsize));
        tBlastXTask.setParameter(BlastTask.PARAM_formatTypesCsv, formatTypesCsv);
        try {
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            computeBean.validateBlastTaskQueryDatabaseMatch(tBlastXTask);
        }
        catch (Exception e) {
            logger.error(e.getMessage());
            return e.getMessage() + "\n";
        }
        logger.debug("Web Services - runTBlastX() complete");
        return saveAndSubmitJob(tBlastXTask, "BlastWithGridMerge");
    }

    public String runVTeraTBlastX(@WebParam(name = "username") String username,
                                  @WebParam(name = "token") String token,
                                  @WebParam(name = "project") String project,
                                  @WebParam(name = "workSessionId") String workSessionId,
                                  @WebParam(name = "jobName") String jobName,
                                  @WebParam(name = "subjectDBIdentifier") String subjectDBIdentifier,
                                  @WebParam(name = "queryFastaFileNodeId") String queryFastaFileNodeId,
                                  @WebParam(name = "databaseAlignmentsPerQuery") int databaseAlignmentsPerQuery,
                                  @WebParam(name = "filter") String filter,
                                  @WebParam(name = "eValueExponent") int eValueExponent,
                                  @WebParam(name = "lowercaseFiltering") boolean lowercaseFiltering,
                                  @WebParam(name = "believeDefline") boolean believeDefline,
                                  @WebParam(name = "databaseSize") long databaseSize,
                                  @WebParam(name = "gapExtendCost") int gapExtendCost,
                                  @WebParam(name = "gappedAlignmentDropoff") int gappedAlignmentDropoff,
                                  @WebParam(name = "matrix") String matrix,
                                  @WebParam(name = "searchSize") int searchSize,
                                  @WebParam(name = "showGIs") boolean showGIs,
                                  @WebParam(name = "wordsize") int wordsize,
                                  @WebParam(name = "bestHitsToKeep") int bestHitsToKeep,
                                  @WebParam(name = "finalGappedDropoff") int finalGappedDropoff,
                                  @WebParam(name = "gapOpenCost") int gapOpenCost,
                                  @WebParam(name = "hitExtensionThreshold") int hitExtensionThreshold,
                                  @WebParam(name = "multihitWindowSize") int multihitWindowSize,
                                  @WebParam(name = "searchStrand") String searchStrand,
                                  @WebParam(name = "ungappedExtensionDropoff") int ungappedExtensionDropoff,
                                  @WebParam(name = "formatTypesCsv") String formatTypesCsv) {
        logger.debug("Web Services - runVTeraTBlastX() acknowledged");
        TeraTBlastXTask tBlastXTask = new TeraTBlastXTask();
        tBlastXTask.setOwner(username);
        tBlastXTask.setJobName(jobName);
        tBlastXTask.setParameter(BlastTask.PARAM_query, queryFastaFileNodeId);
        MultiSelectVO ms = new MultiSelectVO();
        ArrayList<String> dbList = new ArrayList<String>();
        dbList.add(subjectDBIdentifier);
        ms.setPotentialChoices(dbList);
        ms.setActualUserChoices(dbList);
        tBlastXTask.setParameter(BlastTask.PARAM_project, project);
        if (null != workSessionId && !"".equals(workSessionId)) {
            tBlastXTask.setParentTaskId(Long.valueOf(workSessionId));
        }
        tBlastXTask.setParameter(BlastTask.PARAM_subjectDatabases, Task.csvStringFromCollection(ms.getActualUserChoices()));
        tBlastXTask.setParameter(BlastTask.PARAM_databaseAlignments, Integer.toString(databaseAlignmentsPerQuery));
        tBlastXTask.setParameter(BlastTask.PARAM_filter, filter);
        tBlastXTask.setParameter(BlastTask.PARAM_evalue, Integer.toString(eValueExponent));
        tBlastXTask.setParameter(BlastTask.PARAM_lowerCaseFiltering, Boolean.toString(lowercaseFiltering));

        tBlastXTask.setParameter(BlastTask.PARAM_believeDefline, Boolean.toString(believeDefline));
        tBlastXTask.setParameter(BlastTask.PARAM_databaseSize, Long.toString(databaseSize));
        tBlastXTask.setParameter(BlastTask.PARAM_gapExtendCost, Integer.toString(gapExtendCost));
        tBlastXTask.setParameter(BlastTask.PARAM_hitExtensionThreshold, Integer.toString(hitExtensionThreshold));
        tBlastXTask.setParameter(BlastTask.PARAM_matrix, matrix);
        tBlastXTask.setParameter(BlastTask.PARAM_multiHitWindowSize, Integer.toString(multihitWindowSize));
        tBlastXTask.setParameter(BlastTask.PARAM_searchStrand, searchStrand);
        tBlastXTask.setParameter(BlastTask.PARAM_ungappedExtensionDropoff, Integer.toString(ungappedExtensionDropoff));
        tBlastXTask.setParameter(BlastTask.PARAM_bestHitsToKeep, Integer.toString(bestHitsToKeep));
        tBlastXTask.setParameter(BlastTask.PARAM_finalGappedDropoff, Integer.toString(finalGappedDropoff));
        tBlastXTask.setParameter(BlastTask.PARAM_gapOpenCost, Integer.toString(gapOpenCost));
        tBlastXTask.setParameter(BlastTask.PARAM_gappedAlignment, Boolean.TRUE.toString());
        tBlastXTask.setParameter(BlastTask.PARAM_gappedAlignmentDropoff, Integer.toString(gappedAlignmentDropoff));
        tBlastXTask.setParameter(BlastTask.PARAM_searchSize, Integer.toString(searchSize));
        tBlastXTask.setParameter(BlastTask.PARAM_showGIs, Boolean.toString(showGIs));
        tBlastXTask.setParameter(BlastTask.PARAM_wordsize, Integer.toString(wordsize));
        tBlastXTask.setParameter(BlastTask.PARAM_formatTypesCsv, formatTypesCsv);
        try {
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            computeBean.validateBlastTaskQueryDatabaseMatch(tBlastXTask);
        }
        catch (Exception e) {
            logger.error(e.getMessage());
            return e.getMessage() + "\n";
        }
        logger.debug("Web Services - runVTeraTBlastX() complete");
        return saveAndSubmitJob(tBlastXTask, "TeraBlast");
    }

    public String runTBlastN(@WebParam(name = "username") String username,
                             @WebParam(name = "token") String token,
                             @WebParam(name = "project") String project,
                             @WebParam(name = "workSessionId") String workSessionId,
                             @WebParam(name = "jobName") String jobName,
                             @WebParam(name = "subjectDBIdentifier") String subjectDBIdentifier,
                             @WebParam(name = "queryFastaFileNodeId") String queryFastaFileNodeId,
                             @WebParam(name = "databaseAlignmentsPerQuery") int databaseAlignmentsPerQuery,
                             @WebParam(name = "filter") String filter,
                             @WebParam(name = "eValueExponent") int eValueExponent,
                             @WebParam(name = "lowercaseFiltering") boolean lowercaseFiltering,
                             @WebParam(name = "believeDefline") boolean believeDefline,
                             @WebParam(name = "databaseSize") long databaseSize,
                             @WebParam(name = "gapExtendCost") int gapExtendCost,
                             @WebParam(name = "gappedAlignment") boolean gappedAlignment,
                             @WebParam(name = "hitExtensionThreshold") int hitExtensionThreshold,
                             @WebParam(name = "multihitWindowSize") int multihitWindowSize,
                             @WebParam(name = "showGIs") boolean showGIs,
                             @WebParam(name = "wordsize") int wordsize,
                             @WebParam(name = "bestHitsToKeep") int bestHitsToKeep,
                             @WebParam(name = "finalGappedDropoff") int finalGappedDropoff,
                             @WebParam(name = "gapOpenCost") int gapOpenCost,
                             @WebParam(name = "gappedAlignmentDropoff") int gappedAlignmentDropoff,
                             @WebParam(name = "matrix") String matrix,
                             @WebParam(name = "searchSize") int searchSize,
                             @WebParam(name = "ungappedExtensionDropoff") int ungappedExtensionDropoff,
                             @WebParam(name = "formatTypesCsv") String formatTypesCsv) {
        logger.debug("Web Services - runTBlastN() acknowledged");
        TBlastNTask tBlastNTask = new TBlastNTask();
        tBlastNTask.setOwner(username);
        tBlastNTask.setJobName(jobName);
        tBlastNTask.setParameter(BlastNTask.PARAM_query, queryFastaFileNodeId);
        MultiSelectVO ms = new MultiSelectVO();
        ArrayList<String> dbList = new ArrayList<String>();
        dbList.add(subjectDBIdentifier);
        ms.setPotentialChoices(dbList);
        ms.setActualUserChoices(dbList);
        tBlastNTask.setParameter(BlastTask.PARAM_project, project);
        if (null != workSessionId && !"".equals(workSessionId)) {
            tBlastNTask.setParentTaskId(Long.valueOf(workSessionId));
        }
        tBlastNTask.setParameter(BlastTask.PARAM_subjectDatabases, Task.csvStringFromCollection(ms.getActualUserChoices()));
        tBlastNTask.setParameter(BlastTask.PARAM_databaseAlignments, Integer.toString(databaseAlignmentsPerQuery));
        tBlastNTask.setParameter(BlastTask.PARAM_filter, filter);
        tBlastNTask.setParameter(BlastTask.PARAM_evalue, Integer.toString(eValueExponent));
        tBlastNTask.setParameter(BlastTask.PARAM_lowerCaseFiltering, Boolean.toString(lowercaseFiltering));

        tBlastNTask.setParameter(BlastTask.PARAM_believeDefline, Boolean.toString(believeDefline));
        tBlastNTask.setParameter(BlastTask.PARAM_databaseSize, Long.toString(databaseSize));
        tBlastNTask.setParameter(BlastTask.PARAM_gapExtendCost, Integer.toString(gapExtendCost));
        tBlastNTask.setParameter(BlastNTask.PARAM_gappedAlignment, Boolean.toString(gappedAlignment));
        tBlastNTask.setParameter(BlastTask.PARAM_hitExtensionThreshold, Integer.toString(hitExtensionThreshold));
        tBlastNTask.setParameter(BlastTask.PARAM_matrix, matrix);
        tBlastNTask.setParameter(BlastTask.PARAM_multiHitWindowSize, Integer.toString(multihitWindowSize));
        tBlastNTask.setParameter(BlastTask.PARAM_ungappedExtensionDropoff, Integer.toString(ungappedExtensionDropoff));
        tBlastNTask.setParameter(BlastTask.PARAM_bestHitsToKeep, Integer.toString(bestHitsToKeep));
        tBlastNTask.setParameter(BlastTask.PARAM_finalGappedDropoff, Integer.toString(finalGappedDropoff));
        tBlastNTask.setParameter(BlastTask.PARAM_gapOpenCost, Integer.toString(gapOpenCost));
        tBlastNTask.setParameter(BlastTask.PARAM_gappedAlignmentDropoff, Integer.toString(gappedAlignmentDropoff));
        tBlastNTask.setParameter(BlastTask.PARAM_searchSize, Integer.toString(searchSize));
        tBlastNTask.setParameter(BlastTask.PARAM_showGIs, Boolean.toString(showGIs));
        tBlastNTask.setParameter(BlastTask.PARAM_wordsize, Integer.toString(wordsize));
        tBlastNTask.setParameter(BlastTask.PARAM_formatTypesCsv, formatTypesCsv);
        try {
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            computeBean.validateBlastTaskQueryDatabaseMatch(tBlastNTask);
        }
        catch (Exception e) {
            logger.error(e.getMessage());
            return e.getMessage() + "\n";
        }
        logger.debug("Web Services - runTBlastN() complete");
        return saveAndSubmitJob(tBlastNTask, "BlastWithGridMerge");
    }

    public String runVTeraTBlastN(@WebParam(name = "username") String username,
                                  @WebParam(name = "token") String token,
                                  @WebParam(name = "project") String project,
                                  @WebParam(name = "workSessionId") String workSessionId,
                                  @WebParam(name = "jobName") String jobName,
                                  @WebParam(name = "subjectDBIdentifier") String subjectDBIdentifier,
                                  @WebParam(name = "queryFastaFileNodeId") String queryFastaFileNodeId,
                                  @WebParam(name = "databaseAlignmentsPerQuery") int databaseAlignmentsPerQuery,
                                  @WebParam(name = "filter") String filter,
                                  @WebParam(name = "eValueExponent") int eValueExponent,
                                  @WebParam(name = "lowercaseFiltering") boolean lowercaseFiltering,
                                  @WebParam(name = "believeDefline") boolean believeDefline,
                                  @WebParam(name = "databaseSize") long databaseSize,
                                  @WebParam(name = "gapExtendCost") int gapExtendCost,
                                  @WebParam(name = "gappedAlignment") boolean gappedAlignment,
                                  @WebParam(name = "hitExtensionThreshold") int hitExtensionThreshold,
                                  @WebParam(name = "multihitWindowSize") int multihitWindowSize,
                                  @WebParam(name = "showGIs") boolean showGIs,
                                  @WebParam(name = "wordsize") int wordsize,
                                  @WebParam(name = "bestHitsToKeep") int bestHitsToKeep,
                                  @WebParam(name = "finalGappedDropoff") int finalGappedDropoff,
                                  @WebParam(name = "gapOpenCost") int gapOpenCost,
                                  @WebParam(name = "gappedAlignmentDropoff") int gappedAlignmentDropoff,
                                  @WebParam(name = "matrix") String matrix,
                                  @WebParam(name = "searchSize") int searchSize,
                                  @WebParam(name = "ungappedExtensionDropoff") int ungappedExtensionDropoff,
                                  @WebParam(name = "formatTypesCsv") String formatTypesCsv) {
        logger.debug("Web Services - runVTeraTBlastN() acknowledged");
        TeraTBlastNTask tBlastNTask = new TeraTBlastNTask();
        tBlastNTask.setOwner(username);
        tBlastNTask.setJobName(jobName);
        tBlastNTask.setParameter(BlastTask.PARAM_query, queryFastaFileNodeId);
        MultiSelectVO ms = new MultiSelectVO();
        ArrayList<String> dbList = new ArrayList<String>();
        dbList.add(subjectDBIdentifier);
        ms.setPotentialChoices(dbList);
        ms.setActualUserChoices(dbList);
        tBlastNTask.setParameter(BlastTask.PARAM_project, project);
        if (null != workSessionId && !"".equals(workSessionId)) {
            tBlastNTask.setParentTaskId(Long.valueOf(workSessionId));
        }
        tBlastNTask.setParameter(BlastTask.PARAM_subjectDatabases, Task.csvStringFromCollection(ms.getActualUserChoices()));
        tBlastNTask.setParameter(BlastTask.PARAM_databaseAlignments, Integer.toString(databaseAlignmentsPerQuery));
        tBlastNTask.setParameter(BlastTask.PARAM_filter, filter);
        tBlastNTask.setParameter(BlastTask.PARAM_evalue, Integer.toString(eValueExponent));
        tBlastNTask.setParameter(BlastTask.PARAM_lowerCaseFiltering, Boolean.toString(lowercaseFiltering));

        tBlastNTask.setParameter(BlastTask.PARAM_believeDefline, Boolean.toString(believeDefline));
        tBlastNTask.setParameter(BlastTask.PARAM_databaseSize, Long.toString(databaseSize));
        tBlastNTask.setParameter(BlastTask.PARAM_gapExtendCost, Integer.toString(gapExtendCost));
        tBlastNTask.setParameter(BlastTask.PARAM_gappedAlignment, Boolean.toString(gappedAlignment));
        tBlastNTask.setParameter(BlastTask.PARAM_hitExtensionThreshold, Integer.toString(hitExtensionThreshold));
        tBlastNTask.setParameter(BlastTask.PARAM_matrix, matrix);
        tBlastNTask.setParameter(BlastTask.PARAM_multiHitWindowSize, Integer.toString(multihitWindowSize));
        tBlastNTask.setParameter(BlastTask.PARAM_ungappedExtensionDropoff, Integer.toString(ungappedExtensionDropoff));
        tBlastNTask.setParameter(BlastTask.PARAM_bestHitsToKeep, Integer.toString(bestHitsToKeep));
        tBlastNTask.setParameter(BlastTask.PARAM_finalGappedDropoff, Integer.toString(finalGappedDropoff));
        tBlastNTask.setParameter(BlastTask.PARAM_gapOpenCost, Integer.toString(gapOpenCost));
        tBlastNTask.setParameter(BlastTask.PARAM_gappedAlignmentDropoff, Integer.toString(gappedAlignmentDropoff));
        tBlastNTask.setParameter(BlastTask.PARAM_searchSize, Integer.toString(searchSize));
        tBlastNTask.setParameter(BlastTask.PARAM_showGIs, Boolean.toString(showGIs));
        tBlastNTask.setParameter(BlastTask.PARAM_wordsize, Integer.toString(wordsize));
        tBlastNTask.setParameter(BlastTask.PARAM_formatTypesCsv, formatTypesCsv);
        try {
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            computeBean.validateBlastTaskQueryDatabaseMatch(tBlastNTask);
        }
        catch (Exception e) {
            logger.error(e.getMessage());
            return e.getMessage() + "\n";
        }
        logger.debug("Web Services - runVTeraTBlastN() complete");
        return saveAndSubmitJob(tBlastNTask, "TeraBlast");
    }

    public String runBlastP(@WebParam(name = "username") String username,
                            @WebParam(name = "token") String token,
                            @WebParam(name = "project") String project,
                            @WebParam(name = "workSessionId") String workSessionId,
                            @WebParam(name = "jobName") String jobName,
                            @WebParam(name = "subjectDBIdentifier") String subjectDBIdentifier,
                            @WebParam(name = "queryFastaFileNodeId") String queryFastaFileNodeId,
                            @WebParam(name = "databaseAlignmentsPerQuery") int databaseAlignmentsPerQuery,
                            @WebParam(name = "filter") String filter,
                            @WebParam(name = "eValueExponent") int eValueExponent,
                            @WebParam(name = "lowercaseFiltering") boolean lowercaseFiltering,
                            @WebParam(name = "believeDefline") boolean believeDefline,
                            @WebParam(name = "databaseSize") long databaseSize,
                            @WebParam(name = "gapExtendCost") int gapExtendCost,
                            @WebParam(name = "gappedAlignment") boolean gappedAlignment,
                            @WebParam(name = "hitExtensionThreshold") int hitExtensionThreshold,
                            @WebParam(name = "multihitWindowSize") int multihitWindowSize,
                            @WebParam(name = "showGIs") boolean showGIs,
                            @WebParam(name = "wordsize") int wordsize,
                            @WebParam(name = "bestHitsToKeep") int bestHitsToKeep,
                            @WebParam(name = "finalGappedDropoff") int finalGappedDropoff,
                            @WebParam(name = "gapOpenCost") int gapOpenCost,
                            @WebParam(name = "gappedAlignmentDropoff") int gappedAlignmentDropoff,
                            @WebParam(name = "matrix") String matrix,
                            @WebParam(name = "searchSize") int searchSize,
                            @WebParam(name = "ungappedExtensionDropoff") int ungappedExtensionDropoff,
                            @WebParam(name = "formatTypesCsv") String formatTypesCsv) {
        logger.debug("Web Services - runBlastP() acknowledged");
        BlastPTask blastpTask = new BlastPTask();
        blastpTask.setOwner(username);
        blastpTask.setJobName(jobName);
        blastpTask.setParameter(BlastNTask.PARAM_query, queryFastaFileNodeId);
        MultiSelectVO ms = new MultiSelectVO();
        ArrayList<String> dbList = new ArrayList<String>();
        dbList.add(subjectDBIdentifier);
        ms.setPotentialChoices(dbList);
        ms.setActualUserChoices(dbList);
        blastpTask.setParameter(BlastTask.PARAM_project, project);
        if (null != workSessionId && !"".equals(workSessionId)) {
            blastpTask.setParentTaskId(Long.valueOf(workSessionId));
        }
        blastpTask.setParameter(BlastTask.PARAM_subjectDatabases, Task.csvStringFromCollection(ms.getActualUserChoices()));
        blastpTask.setParameter(BlastTask.PARAM_databaseAlignments, Integer.toString(databaseAlignmentsPerQuery));
        blastpTask.setParameter(BlastTask.PARAM_filter, filter);
        blastpTask.setParameter(BlastTask.PARAM_evalue, Integer.toString(eValueExponent));
        blastpTask.setParameter(BlastTask.PARAM_lowerCaseFiltering, Boolean.toString(lowercaseFiltering));
        blastpTask.setParameter(BlastTask.PARAM_believeDefline, Boolean.toString(believeDefline));
        blastpTask.setParameter(BlastTask.PARAM_databaseSize, Long.toString(databaseSize));
        blastpTask.setParameter(BlastTask.PARAM_gapExtendCost, Integer.toString(gapExtendCost));
        blastpTask.setParameter(BlastPTask.PARAM_gappedAlignment, Boolean.toString(gappedAlignment));
        blastpTask.setParameter(BlastTask.PARAM_hitExtensionThreshold, Integer.toString(hitExtensionThreshold));
        blastpTask.setParameter(BlastTask.PARAM_matrix, matrix);
        blastpTask.setParameter(BlastTask.PARAM_multiHitWindowSize, Integer.toString(multihitWindowSize));
        blastpTask.setParameter(BlastTask.PARAM_ungappedExtensionDropoff, Integer.toString(ungappedExtensionDropoff));
        blastpTask.setParameter(BlastTask.PARAM_bestHitsToKeep, Integer.toString(bestHitsToKeep));
        blastpTask.setParameter(BlastTask.PARAM_finalGappedDropoff, Integer.toString(finalGappedDropoff));
        blastpTask.setParameter(BlastTask.PARAM_gapOpenCost, Integer.toString(gapOpenCost));
        blastpTask.setParameter(BlastTask.PARAM_gappedAlignmentDropoff, Integer.toString(gappedAlignmentDropoff));
        blastpTask.setParameter(BlastTask.PARAM_searchSize, Integer.toString(searchSize));
        blastpTask.setParameter(BlastTask.PARAM_showGIs, Boolean.toString(showGIs));
        blastpTask.setParameter(BlastTask.PARAM_wordsize, Integer.toString(wordsize));
        blastpTask.setParameter(BlastTask.PARAM_formatTypesCsv, formatTypesCsv);
        try {
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            computeBean.validateBlastTaskQueryDatabaseMatch(blastpTask);
        }
        catch (Exception e) {
            logger.error(e.getMessage());
            return e.getMessage() + "\n";
        }
        logger.debug("Web Services - runBlastP() complete");
        return saveAndSubmitJob(blastpTask, "BlastWithGridMerge");
    }

    public String runVTeraBlastP(@WebParam(name = "username") String username,
                                 @WebParam(name = "token") String token,
                                 @WebParam(name = "project") String project,
                                 @WebParam(name = "workSessionId") String workSessionId,
                                 @WebParam(name = "jobName") String jobName,
                                 @WebParam(name = "subjectDBIdentifier") String subjectDBIdentifier,
                                 @WebParam(name = "queryFastaFileNodeId") String queryFastaFileNodeId,
                                 @WebParam(name = "databaseAlignmentsPerQuery") int databaseAlignmentsPerQuery,
                                 @WebParam(name = "filter") String filter,
                                 @WebParam(name = "eValueExponent") int eValueExponent,
                                 @WebParam(name = "lowercaseFiltering") boolean lowercaseFiltering,
                                 @WebParam(name = "believeDefline") boolean believeDefline,
                                 @WebParam(name = "databaseSize") long databaseSize,
                                 @WebParam(name = "gapExtendCost") int gapExtendCost,
                                 @WebParam(name = "gappedAlignment") boolean gappedAlignment,
                                 @WebParam(name = "hitExtensionThreshold") int hitExtensionThreshold,
                                 @WebParam(name = "multihitWindowSize") int multihitWindowSize,
                                 @WebParam(name = "showGIs") boolean showGIs,
                                 @WebParam(name = "wordsize") int wordsize,
                                 @WebParam(name = "bestHitsToKeep") int bestHitsToKeep,
                                 @WebParam(name = "finalGappedDropoff") int finalGappedDropoff,
                                 @WebParam(name = "gapOpenCost") int gapOpenCost,
                                 @WebParam(name = "gappedAlignmentDropoff") int gappedAlignmentDropoff,
                                 @WebParam(name = "matrix") String matrix,
                                 @WebParam(name = "searchSize") int searchSize,
                                 @WebParam(name = "ungappedExtensionDropoff") int ungappedExtensionDropoff,
                                 @WebParam(name = "formatTypesCsv") String formatTypesCsv) {
        logger.debug("Web Services - runVTeraBlastP() acknowledged");
        TeraBlastPTask blastpTask = new TeraBlastPTask();
        blastpTask.setOwner(username);
        blastpTask.setJobName(jobName);
        blastpTask.setParameter(BlastTask.PARAM_query, queryFastaFileNodeId);
        MultiSelectVO ms = new MultiSelectVO();
        ArrayList<String> dbList = new ArrayList<String>();
        dbList.add(subjectDBIdentifier);
        ms.setPotentialChoices(dbList);
        ms.setActualUserChoices(dbList);
        blastpTask.setParameter(BlastTask.PARAM_project, project);
        if (null != workSessionId && !"".equals(workSessionId)) {
            blastpTask.setParentTaskId(Long.valueOf(workSessionId));
        }
        blastpTask.setParameter(BlastTask.PARAM_subjectDatabases, Task.csvStringFromCollection(ms.getActualUserChoices()));
        blastpTask.setParameter(BlastTask.PARAM_databaseAlignments, Integer.toString(databaseAlignmentsPerQuery));
        blastpTask.setParameter(BlastTask.PARAM_filter, filter);
        blastpTask.setParameter(BlastTask.PARAM_evalue, Integer.toString(eValueExponent));
        blastpTask.setParameter(BlastTask.PARAM_lowerCaseFiltering, Boolean.toString(lowercaseFiltering));
        blastpTask.setParameter(BlastTask.PARAM_believeDefline, Boolean.toString(believeDefline));
        blastpTask.setParameter(BlastTask.PARAM_databaseSize, Long.toString(databaseSize));
        blastpTask.setParameter(BlastTask.PARAM_gapExtendCost, Integer.toString(gapExtendCost));
        blastpTask.setParameter(BlastTask.PARAM_gappedAlignment, Boolean.toString(gappedAlignment));
        blastpTask.setParameter(BlastTask.PARAM_hitExtensionThreshold, Integer.toString(hitExtensionThreshold));
        blastpTask.setParameter(BlastTask.PARAM_matrix, matrix);
        blastpTask.setParameter(BlastTask.PARAM_multiHitWindowSize, Integer.toString(multihitWindowSize));
        blastpTask.setParameter(BlastTask.PARAM_ungappedExtensionDropoff, Integer.toString(ungappedExtensionDropoff));
        blastpTask.setParameter(BlastTask.PARAM_bestHitsToKeep, Integer.toString(bestHitsToKeep));
        blastpTask.setParameter(BlastTask.PARAM_finalGappedDropoff, Integer.toString(finalGappedDropoff));
        blastpTask.setParameter(BlastTask.PARAM_gapOpenCost, Integer.toString(gapOpenCost));
        blastpTask.setParameter(BlastTask.PARAM_gappedAlignmentDropoff, Integer.toString(gappedAlignmentDropoff));
        blastpTask.setParameter(BlastTask.PARAM_searchSize, Integer.toString(searchSize));
        blastpTask.setParameter(BlastTask.PARAM_showGIs, Boolean.toString(showGIs));
        blastpTask.setParameter(BlastTask.PARAM_wordsize, Integer.toString(wordsize));
        blastpTask.setParameter(BlastTask.PARAM_formatTypesCsv, formatTypesCsv);
        try {
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            computeBean.validateBlastTaskQueryDatabaseMatch(blastpTask);
        }
        catch (Exception e) {
            logger.error(e.getMessage());
            return e.getMessage() + "\n";
        }
        logger.debug("Web Services - runVTeraBlastP() complete");
        return saveAndSubmitJob(blastpTask, "TeraBlast");
    }

    public String runBlastX(@WebParam(name = "username") String username,
                            @WebParam(name = "token") String token,
                            @WebParam(name = "project") String project,
                            @WebParam(name = "workSessionId") String workSessionId,
                            @WebParam(name = "jobName") String jobName,
                            @WebParam(name = "subjectDBIdentifier") String subjectDBIdentifier,
                            @WebParam(name = "queryFastaFileNodeId") String queryFastaFileNodeId,
                            @WebParam(name = "databaseAlignmentsPerQuery") int databaseAlignmentsPerQuery,
                            @WebParam(name = "filter") String filter,
                            @WebParam(name = "eValueExponent") int eValueExponent,
                            @WebParam(name = "lowercaseFiltering") boolean lowercaseFiltering,
                            @WebParam(name = "believeDefline") boolean believeDefline,
                            @WebParam(name = "databaseSize") long databaseSize,
                            @WebParam(name = "frameshiftPenalty") String frameshiftPenalty,
                            @WebParam(name = "gapOpenCost") int gapOpenCost,
                            @WebParam(name = "gappedAlignmentDropoff") int gappedAlignmentDropoff,
                            @WebParam(name = "matrix") String matrix,
                            @WebParam(name = "searchSize") int searchSize,
                            @WebParam(name = "showGIs") boolean showGIs,
                            @WebParam(name = "wordsize") int wordsize,
                            @WebParam(name = "bestHitsToKeep") int bestHitsToKeep,
                            @WebParam(name = "finalGappedDropoff") int finalGappedDropoff,
                            @WebParam(name = "gapExtendCost") int gapExtendCost,
                            @WebParam(name = "gappedAlignment") boolean gappedAlignment,
                            @WebParam(name = "hitExtensionThreshold") int hitExtensionThreshold,
                            @WebParam(name = "multihitWindowSize") int multihitWindowSize,
                            @WebParam(name = "searchStrand") String searchStrand,
                            @WebParam(name = "ungappedExtensionDropoff") int ungappedExtensionDropoff,
                            @WebParam(name = "formatTypesCsv") String formatTypesCsv) {
        logger.debug("Web Services - runBlastX() acknowledged");
        BlastXTask blastxTask = new BlastXTask();
        blastxTask.setOwner(username);
        blastxTask.setJobName(jobName);
        blastxTask.setParameter(BlastNTask.PARAM_query, queryFastaFileNodeId);
        MultiSelectVO ms = new MultiSelectVO();
        ArrayList<String> dbList = new ArrayList<String>();
        dbList.add(subjectDBIdentifier);
        ms.setPotentialChoices(dbList);
        ms.setActualUserChoices(dbList);
        blastxTask.setParameter(BlastTask.PARAM_project, project);
        if (null != workSessionId && !"".equals(workSessionId)) {
            blastxTask.setParentTaskId(Long.valueOf(workSessionId));
        }
        blastxTask.setParameter(BlastTask.PARAM_subjectDatabases, Task.csvStringFromCollection(ms.getActualUserChoices()));
        blastxTask.setParameter(BlastTask.PARAM_databaseAlignments, Integer.toString(databaseAlignmentsPerQuery));
        blastxTask.setParameter(BlastTask.PARAM_filter, filter);
        blastxTask.setParameter(BlastTask.PARAM_evalue, Integer.toString(eValueExponent));
        blastxTask.setParameter(BlastTask.PARAM_lowerCaseFiltering, Boolean.toString(lowercaseFiltering));
        blastxTask.setParameter(BlastTask.PARAM_believeDefline, Boolean.toString(believeDefline));
        blastxTask.setParameter(BlastTask.PARAM_databaseSize, Long.toString(databaseSize));
        blastxTask.setParameter(BlastXTask.PARAM_frameshiftPenalty, frameshiftPenalty);
        blastxTask.setParameter(BlastTask.PARAM_gapOpenCost, Integer.toString(gapOpenCost));
        blastxTask.setParameter(BlastTask.PARAM_gappedAlignmentDropoff, Integer.toString(gappedAlignmentDropoff));
        blastxTask.setParameter(BlastTask.PARAM_matrix, matrix);
        blastxTask.setParameter(BlastTask.PARAM_searchSize, Integer.toString(searchSize));
        blastxTask.setParameter(BlastTask.PARAM_showGIs, Boolean.toString(showGIs));
        blastxTask.setParameter(BlastTask.PARAM_wordsize, Integer.toString(wordsize));
        blastxTask.setParameter(BlastTask.PARAM_bestHitsToKeep, Integer.toString(bestHitsToKeep));
        blastxTask.setParameter(BlastTask.PARAM_finalGappedDropoff, Integer.toString(finalGappedDropoff));
        blastxTask.setParameter(BlastTask.PARAM_gapExtendCost, Integer.toString(gapExtendCost));
        blastxTask.setParameter(BlastNTask.PARAM_gappedAlignment, Boolean.toString(gappedAlignment));
        blastxTask.setParameter(BlastTask.PARAM_hitExtensionThreshold, Integer.toString(hitExtensionThreshold));
        blastxTask.setParameter(BlastTask.PARAM_multiHitWindowSize, Integer.toString(multihitWindowSize));
        blastxTask.setParameter(BlastNTask.PARAM_searchStrand, searchStrand);
        blastxTask.setParameter(BlastTask.PARAM_ungappedExtensionDropoff, Integer.toString(ungappedExtensionDropoff));
        blastxTask.setParameter(BlastTask.PARAM_formatTypesCsv, formatTypesCsv);
        try {
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            computeBean.validateBlastTaskQueryDatabaseMatch(blastxTask);
        }
        catch (Exception e) {
            logger.error(e.getMessage());
            return e.getMessage() + "\n";
        }
        logger.debug("Web Services - runBlastX() complete");
        return saveAndSubmitJob(blastxTask, "BlastWithGridMerge");
    }

    public String runVTeraBlastX(@WebParam(name = "username") String username,
                                 @WebParam(name = "token") String token,
                                 @WebParam(name = "project") String project,
                                 @WebParam(name = "workSessionId") String workSessionId,
                                 @WebParam(name = "jobName") String jobName,
                                 @WebParam(name = "subjectDBIdentifier") String subjectDBIdentifier,
                                 @WebParam(name = "queryFastaFileNodeId") String queryFastaFileNodeId,
                                 @WebParam(name = "databaseAlignmentsPerQuery") int databaseAlignmentsPerQuery,
                                 @WebParam(name = "filter") String filter,
                                 @WebParam(name = "eValueExponent") int eValueExponent,
                                 @WebParam(name = "lowercaseFiltering") boolean lowercaseFiltering,
                                 @WebParam(name = "believeDefline") boolean believeDefline,
                                 @WebParam(name = "databaseSize") long databaseSize,
                                 @WebParam(name = "frameshiftPenalty") String frameshiftPenalty,
                                 @WebParam(name = "gapOpenCost") int gapOpenCost,
                                 @WebParam(name = "gappedAlignmentDropoff") int gappedAlignmentDropoff,
                                 @WebParam(name = "matrix") String matrix,
                                 @WebParam(name = "searchSize") int searchSize,
                                 @WebParam(name = "showGIs") boolean showGIs,
                                 @WebParam(name = "wordsize") int wordsize,
                                 @WebParam(name = "bestHitsToKeep") int bestHitsToKeep,
                                 @WebParam(name = "finalGappedDropoff") int finalGappedDropoff,
                                 @WebParam(name = "gapExtendCost") int gapExtendCost,
                                 @WebParam(name = "gappedAlignment") boolean gappedAlignment,
                                 @WebParam(name = "hitExtensionThreshold") int hitExtensionThreshold,
                                 @WebParam(name = "multihitWindowSize") int multihitWindowSize,
                                 @WebParam(name = "searchStrand") String searchStrand,
                                 @WebParam(name = "ungappedExtensionDropoff") int ungappedExtensionDropoff,
                                 @WebParam(name = "formatTypesCsv") String formatTypesCsv) {
        logger.debug("Web Services - runVTeraBlastX() acknowledged");
        TeraBlastXTask blastxTask = new TeraBlastXTask();
        blastxTask.setOwner(username);
        blastxTask.setJobName(jobName);
        blastxTask.setParameter(BlastTask.PARAM_query, queryFastaFileNodeId);
        MultiSelectVO ms = new MultiSelectVO();
        ArrayList<String> dbList = new ArrayList<String>();
        dbList.add(subjectDBIdentifier);
        ms.setPotentialChoices(dbList);
        ms.setActualUserChoices(dbList);
        blastxTask.setParameter(BlastTask.PARAM_project, project);
        if (null != workSessionId && !"".equals(workSessionId)) {
            blastxTask.setParentTaskId(Long.valueOf(workSessionId));
        }
        blastxTask.setParameter(BlastTask.PARAM_subjectDatabases, Task.csvStringFromCollection(ms.getActualUserChoices()));
        blastxTask.setParameter(BlastTask.PARAM_databaseAlignments, Integer.toString(databaseAlignmentsPerQuery));
        blastxTask.setParameter(BlastTask.PARAM_filter, filter);
        blastxTask.setParameter(BlastTask.PARAM_evalue, Integer.toString(eValueExponent));
        blastxTask.setParameter(BlastTask.PARAM_lowerCaseFiltering, Boolean.toString(lowercaseFiltering));
        blastxTask.setParameter(BlastTask.PARAM_believeDefline, Boolean.toString(believeDefline));
        blastxTask.setParameter(BlastTask.PARAM_databaseSize, Long.toString(databaseSize));
        blastxTask.setParameter(BlastTask.PARAM_frameshiftPenalty, frameshiftPenalty);
        blastxTask.setParameter(BlastTask.PARAM_gapOpenCost, Integer.toString(gapOpenCost));
        blastxTask.setParameter(BlastTask.PARAM_gappedAlignmentDropoff, Integer.toString(gappedAlignmentDropoff));
        blastxTask.setParameter(BlastTask.PARAM_matrix, matrix);
        blastxTask.setParameter(BlastTask.PARAM_searchSize, Integer.toString(searchSize));
        blastxTask.setParameter(BlastTask.PARAM_showGIs, Boolean.toString(showGIs));
        blastxTask.setParameter(BlastTask.PARAM_wordsize, Integer.toString(wordsize));
        blastxTask.setParameter(BlastTask.PARAM_bestHitsToKeep, Integer.toString(bestHitsToKeep));
        blastxTask.setParameter(BlastTask.PARAM_finalGappedDropoff, Integer.toString(finalGappedDropoff));
        blastxTask.setParameter(BlastTask.PARAM_gapExtendCost, Integer.toString(gapExtendCost));
        blastxTask.setParameter(BlastTask.PARAM_gappedAlignment, Boolean.toString(gappedAlignment));
        blastxTask.setParameter(BlastTask.PARAM_hitExtensionThreshold, Integer.toString(hitExtensionThreshold));
        blastxTask.setParameter(BlastTask.PARAM_multiHitWindowSize, Integer.toString(multihitWindowSize));
        blastxTask.setParameter(BlastTask.PARAM_searchStrand, searchStrand);
        blastxTask.setParameter(BlastTask.PARAM_ungappedExtensionDropoff, Integer.toString(ungappedExtensionDropoff));
        blastxTask.setParameter(BlastTask.PARAM_formatTypesCsv, formatTypesCsv);
        try {
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            computeBean.validateBlastTaskQueryDatabaseMatch(blastxTask);
        }
        catch (Exception e) {
            logger.error(e.getMessage());
            return e.getMessage() + "\n";
        }
        logger.debug("Web Services - runVTeraBlastX() complete");
        return saveAndSubmitJob(blastxTask, "TeraBlast");
    }

    public String runTophat(@WebParam(name = "username") String username,
                            @WebParam(name = "token") String token,
                            @WebParam(name = "project") String project,
                            @WebParam(name = "workSessionId") String workSessionId,
                            @WebParam(name = "jobName") String jobName,
                            @WebParam(name = "inputFastqReadDirectoryNodeId") String inputFastqReadDirectoryNodeId,
                            @WebParam(name = "inputReferenceGenomeFastaFileId") String inputReferenceGenomeFastaFileId) {
        logger.debug("Web Services - runTophat() acknowledged");
        TophatTask tophatTask = new TophatTask();
        tophatTask.setOwner(username);
        tophatTask.setParameter(TophatTask.PARAM_project, project);
        if (null != workSessionId && !"".equals(workSessionId)) {
            tophatTask.setParentTaskId(Long.valueOf(workSessionId));
        }
        tophatTask.setJobName(jobName);
        tophatTask.setParameter(TophatTask.PARAM_reads_fastQ_node_id, inputFastqReadDirectoryNodeId);
        tophatTask.setParameter(TophatTask.PARAM_refgenome_fasta_node_id, inputReferenceGenomeFastaFileId);

        logger.info("Web Services - runTophat() config complete - now submitting task");
        return saveAndSubmitJob(tophatTask, "Tophat");
    }

    public String runCufflinks(@WebParam(name = "username") String username,
                               @WebParam(name = "token") String token,
                               @WebParam(name = "project") String project,
                               @WebParam(name = "workSessionId") String workSessionId,
                               @WebParam(name = "jobName") String jobName,
                               @WebParam(name = "inputSamFileNodeId") String inputSamFileNodeId,
                               @WebParam(name = "matePairInnerMeanDistance") String matePairInnerMeanDistance,
                               @WebParam(name = "matePairStdDevDistance") String matePairStdDevDistance,
                               @WebParam(name = "maxIntronLength") String maxIntronLength,
                               @WebParam(name = "minIsoformFraction") String minIsoformFraction,
                               @WebParam(name = "preMrnaFraction") String preMrnaFraction,
                               @WebParam(name = "minMapQual") String minMapQual,
                               @WebParam(name = "altLabel") String altLabel,
                               @WebParam(name = "optionalGtfFileNodeId") String optionalGtfFileNodeId) {

        logger.debug("Web Services - runCufflinks() acknowledged");
        CufflinksTask cufflinksTask = new CufflinksTask();
        cufflinksTask.setOwner(username);
        cufflinksTask.setParameter(CufflinksTask.PARAM_project, project);
        if (null != workSessionId && !"".equals(workSessionId)) {
            cufflinksTask.setParentTaskId(Long.valueOf(workSessionId));
        }
        cufflinksTask.setJobName(jobName);
        cufflinksTask.setParameter(CufflinksTask.PARAM_sam_file_input_node_id, inputSamFileNodeId);
        parameterHelper(cufflinksTask, CufflinksTask.PARAM_inner_dist_mean, matePairInnerMeanDistance);
        parameterHelper(cufflinksTask, CufflinksTask.PARAM_inner_dist_std_dev, matePairStdDevDistance);
        parameterHelper(cufflinksTask, CufflinksTask.PARAM_max_intron_length, maxIntronLength);
        parameterHelper(cufflinksTask, CufflinksTask.PARAM_min_isoform_fraction, minIsoformFraction);
        parameterHelper(cufflinksTask, CufflinksTask.PARAM_pre_mrna_fraction, preMrnaFraction);
        parameterHelper(cufflinksTask, CufflinksTask.PARAM_min_mapqual, minMapQual);
        parameterHelper(cufflinksTask, CufflinksTask.PARAM_alt_label, altLabel);
        parameterHelper(cufflinksTask, CufflinksTask.PARAM_gtf_node_id, optionalGtfFileNodeId);

        logger.info("Web Services - runCufflinks() config complete - now submitting task");
        return saveAndSubmitJob(cufflinksTask, "Cufflinks");
    }

    public String runJaccard(@WebParam(name = "username") String username,
                             @WebParam(name = "token") String token,
                             @WebParam(name = "project") String project,
                             @WebParam(name = "workSessionId") String workSessionId,
                             @WebParam(name = "jobName") String jobName,
                             @WebParam(name = "input_file_list") String input_file_list,
                             @WebParam(name = "bsml_search_list") String bsml_search_list,
                             @WebParam(name = "linkscore") String link_score,
                             @WebParam(name = "percent_identity") String percent_identity,
                             @WebParam(name = "percent_coverage") String percent_coverage,
                             @WebParam(name = "p_value") String p_value,
                             @WebParam(name = "maxCogSeqCount") String maxCogSeqCount) {

        logger.debug("Web Services - runJaccard() acknowledged");
        JaccardTask jaccardTask = new JaccardTask();
        jaccardTask.setOwner(username);
        jaccardTask.setParameter(JaccardTask.PARAM_project, project);
        if (null != workSessionId && !"".equals(workSessionId)) {
            jaccardTask.setParentTaskId(Long.valueOf(workSessionId));
        }
        jaccardTask.setJobName(jobName);
        parameterHelper(jaccardTask, JaccardTask.PARAM_input_file_list, input_file_list);
        parameterHelper(jaccardTask, JaccardTask.PARAM_bsmlSearchList, bsml_search_list);
        parameterHelper(jaccardTask, JaccardTask.PARAM_link_score, link_score);
        parameterHelper(jaccardTask, JaccardTask.PARAM_percent_identity, percent_identity);
        parameterHelper(jaccardTask, JaccardTask.PARAM_percent_coverage, percent_coverage);
        parameterHelper(jaccardTask, JaccardTask.PARAM_p_value, p_value);
        parameterHelper(jaccardTask, JaccardTask.PARAM_max_cog_seq_count, maxCogSeqCount);

        logger.info("Web Services - runJaccard() config complete - now submitting task");
        return saveAndSubmitJob(jaccardTask, "Jaccard");
    }

    public String runJocs(@WebParam(name = "username") String username,
                          @WebParam(name = "token") String token,
                          @WebParam(name = "project") String project,
                          @WebParam(name = "workSessionId") String workSessionId,
                          @WebParam(name = "jobName") String jobName,
                          @WebParam(name = "bsmlSearchList") String bsmlSearchList,
                          @WebParam(name = "bsmlModelList") String bsmlModelList,
                          @WebParam(name = "bsmlJaccardlList") String bsmlJaccardlList,
                          @WebParam(name = "pvalcut") String pvalcut,
                          @WebParam(name = "coverageCutoff") String coverageCutoff,
                          @WebParam(name = "jaccard_coefficient") String jaccardCoefficient,
                          @WebParam(name = "j_cutoff") String jCutoff,
                          @WebParam(name = "max_cog_seq_count") String maxCogSeqCount) {

        logger.debug("Web Services - runJocs() acknowledged");
        JocsTask jocsTask = new JocsTask();
        jocsTask.setOwner(username);
        jocsTask.setParameter(JocsTask.PARAM_project, project);
        if (null != workSessionId && !"".equals(workSessionId)) {
            jocsTask.setParentTaskId(Long.valueOf(workSessionId));
        }
        jocsTask.setJobName(jobName);
        parameterHelper(jocsTask, JocsTask.PARAM_bsmlSearchList, bsmlSearchList);
        parameterHelper(jocsTask, JocsTask.PARAM_bsmlModelList, bsmlModelList);
        parameterHelper(jocsTask, JocsTask.PARAM_bsmlJaccardList, bsmlJaccardlList);
        parameterHelper(jocsTask, JocsTask.PARAM_pvlaue, pvalcut);
        parameterHelper(jocsTask, JocsTask.PARAM_coverageCutoff, coverageCutoff);
        parameterHelper(jocsTask, JocsTask.PARAM_jaccard_coefficient, jaccardCoefficient);
        parameterHelper(jocsTask, JocsTask.PARAM_j_cutoff, jCutoff);
        parameterHelper(jocsTask, JocsTask.PARAM_max_cog_seq_count, maxCogSeqCount);

        logger.info("Web Services - runJocs() config complete - now submitting task");
        return saveAndSubmitJob(jocsTask, "Jocs");
    }

    public String runSignalp(@WebParam(name = "username") String username,
                             @WebParam(name = "token") String token,
                             @WebParam(name = "project") String project,
                             @WebParam(name = "workSessionId") String workSessionId,
                             @WebParam(name = "jobName") String jobName,
                             @WebParam(name = "fastaInputNodeId") String fastaInputNodeId,
                             @WebParam(name = "typeOfOrganism") String typeOfOrganism,
                             @WebParam(name = "format") String format,
                             @WebParam(name = "method") String method,
                             @WebParam(name = "truncateLength") String truncateLength) {
        logger.debug("Web Services - runSignalp() acknowledged");
        SignalpTask signalpTask = new SignalpTask();
        signalpTask.setOwner(username);
        signalpTask.setParameter(SignalpTask.PARAM_project, project);
        if (null != workSessionId && !"".equals(workSessionId)) {
            signalpTask.setParentTaskId(Long.valueOf(workSessionId));
        }
        signalpTask.setJobName(jobName);
        signalpTask.setParameter(SignalpTask.PARAM_fasta_input_node_id, fastaInputNodeId);
        parameterHelper(signalpTask, SignalpTask.PARAM_type_of_organism, typeOfOrganism);
        parameterHelper(signalpTask, SignalpTask.PARAM_format, format);
        parameterHelper(signalpTask, SignalpTask.PARAM_method, method);
        parameterHelper(signalpTask, SignalpTask.PARAM_truncate_length, truncateLength);

        logger.info("Web Services - runSignalp() config complete - now submitting task");
        return saveAndSubmitJob(signalpTask, "Signalp");
    }

    public String runEAP(@WebParam(name = "username") String username,
                         @WebParam(name = "token") String token,
                         @WebParam(name = "project") String project,
                         @WebParam(name = "workSessionId") String workSessionId,
                         @WebParam(name = "jobName") String jobName,
                         @WebParam(name = "input_fasta") String input_fasta,
                         @WebParam(name = "dataset_name") String dataset_name,
                         @WebParam(name = "dataset_description") String dataset_description,
                         @WebParam(name = "attribute_list") String attribute_list,
                         @WebParam(name = "configuration_db") String configuration_db,
                         @WebParam(name = "query_aliases") String query_aliases,
                         @WebParam(name = "max_retries") String max_retries,
                         @WebParam(name = "compute_list") String compute_list,
                         @WebParam(name = "jacs_wsdl_url") String jacs_wsdl_url,
                         @WebParam(name = "gzip") String gzip) {
        logger.debug("Web Services - runEAP() acknowledged");
        EAPTask eapTask = new EAPTask();
        eapTask.setOwner(username);
        eapTask.setParameter(EAPTask.PARAM_project, project);
        if (null != workSessionId && !"".equals(workSessionId)) {
            eapTask.setParentTaskId(Long.valueOf(workSessionId));
        }
        eapTask.setJobName(jobName);
        eapTask.setParameter(EAPTask.PARAM_dataset_name, dataset_name);
        parameterHelper(eapTask, EAPTask.PARAM_input_fasta, input_fasta);
        parameterHelper(eapTask, EAPTask.PARAM_dataset_description, dataset_description);
        parameterHelper(eapTask, EAPTask.PARAM_attribute_list, attribute_list);
        parameterHelper(eapTask, EAPTask.PARAM_configuration_db, configuration_db);
        parameterHelper(eapTask, EAPTask.PARAM_query_aliases, query_aliases);
        parameterHelper(eapTask, EAPTask.PARAM_max_retries, max_retries);
        parameterHelper(eapTask, EAPTask.PARAM_compute_list, compute_list);
        parameterHelper(eapTask, EAPTask.PARAM_jacs_wsdl_url, jacs_wsdl_url);
        parameterHelper(eapTask, EAPTask.PARAM_gzip, gzip);

        logger.info("Web Services - runEAP() config complete - now submitting task");
        return saveAndSubmitJob(eapTask, "EAP");
    }

    public String runSiftProteinSubstitution(@WebParam(name = "username") String username,
                                             @WebParam(name = "token") String token,
                                             @WebParam(name = "project") String project,
                                             @WebParam(name = "workSessionId") String workSessionId,
                                             @WebParam(name = "jobName") String jobName,
                                             @WebParam(name = "fastaInputNodeId") String fastaInputNodeId,
                                             @WebParam(name = "substitutionString") String substitutionString) {
        logger.debug("Web Services - runSiftProteinSubstitution() acknowledged");
        SiftProteinSubstitutionTask siftProteinSubstitutionTask = new SiftProteinSubstitutionTask();
        siftProteinSubstitutionTask.setOwner(username);
        siftProteinSubstitutionTask.setParameter(SiftProteinSubstitutionTask.PARAM_project, project);
        if (null != workSessionId && !"".equals(workSessionId)) {
            siftProteinSubstitutionTask.setParentTaskId(Long.valueOf(workSessionId));
        }
        siftProteinSubstitutionTask.setJobName(jobName);
        siftProteinSubstitutionTask.setParameter(SiftProteinSubstitutionTask.PARAM_fasta_input_node_id, fastaInputNodeId);
        parameterHelper(siftProteinSubstitutionTask, SiftProteinSubstitutionTask.PARAM_substitution_string, substitutionString);

        logger.info("Web Services - runsiftProteinSubstitution() config complete - now submitting task");
        return saveAndSubmitJob(siftProteinSubstitutionTask, "SiftProteinSubstitution");
    }

    public String runClustalw2(@WebParam(name = "username") String username,
                               @WebParam(name = "token") String token,
                               @WebParam(name = "project") String project,
                               @WebParam(name = "workSessionId") String workSessionId,
                               @WebParam(name = "jobName") String jobName,
                               @WebParam(name = "fastaInputNodeId") String fastaInputNodeId,
                               @WebParam(name = "fastaInputFileList") String fastaInputFileList,
                               @WebParam(name = "align") String align,
                               @WebParam(name = "tree") String tree,
                               @WebParam(name = "bootstrap") String bootstrap,
                               @WebParam(name = "conver") String convert,
                               @WebParam(name = "quicktree") String quicktree,
                               @WebParam(name = "type") String type,
                               @WebParam(name = "negative") String negative,
                               @WebParam(name = "output") String output,
                               @WebParam(name = "outorder") String outorder,
                               @WebParam(name = "clustal_case") String clustal_case,
                               @WebParam(name = "seqnos") String seqnos,
                               @WebParam(name = "seqno_range") String seqno_range,
                               @WebParam(name = "range") String range,
                               @WebParam(name = "maxseqlen") String maxseqlen,
                               @WebParam(name = "quiet") String quiet,
                               @WebParam(name = "stats") String stats,
                               @WebParam(name = "ktuple") String ktuple,
                               @WebParam(name = "topdiags") String topdiags,
                               @WebParam(name = "window") String window,
                               @WebParam(name = "pairgap") String pairgap,
                               @WebParam(name = "score") String score,
                               @WebParam(name = "pwmatrix") String pwmatrix,
                               @WebParam(name = "pwdnamatrix") String pwdnamatrix,
                               @WebParam(name = "pwgapopen") String pwgapopen,
                               @WebParam(name = "pwgapext") String pwgapext,
                               @WebParam(name = "newtree") String newtree,
                               @WebParam(name = "usetree") String usetree,
                               @WebParam(name = "matrix") String matrix,
                               @WebParam(name = "dnamatrix") String dnamatrix,
                               @WebParam(name = "gapopen") String gapopen,
                               @WebParam(name = "gapext") String gapext,
                               @WebParam(name = "endgaps") String endgaps,
                               @WebParam(name = "gapdist") String gapdist,
                               @WebParam(name = "nopgap") String nopgap,
                               @WebParam(name = "nohgap") String nohgap,
                               @WebParam(name = "hgapresidues") String hgapresidues,
                               @WebParam(name = "maxdiv") String maxdiv,
                               @WebParam(name = "transweight") String transweight,
                               @WebParam(name = "iteration") String iteration,
                               @WebParam(name = "numiter") String numiter,
                               @WebParam(name = "noweights") String noweights,
                               @WebParam(name = "profile") String profile,
                               @WebParam(name = "newtree1") String newtree1,
                               @WebParam(name = "usetree1") String usetree1,
                               @WebParam(name = "newtree2") String newtree2,
                               @WebParam(name = "usetree2") String usetree2,
                               @WebParam(name = "sequences") String sequences,
                               @WebParam(name = "nosecstr1") String nosecstr1,
                               @WebParam(name = "nosecstr2") String nosecstr2,
                               @WebParam(name = "secstrout") String secstrout,
                               @WebParam(name = "helixgap") String helixgap,
                               @WebParam(name = "strandgap") String strandgap,
                               @WebParam(name = "loopgap") String loopgap,
                               @WebParam(name = "terminalgap") String terminalgap,
                               @WebParam(name = "helixendin") String helixendin,
                               @WebParam(name = "helixendout") String helixendout,
                               @WebParam(name = "strandendin") String strandendin,
                               @WebParam(name = "strandendou") String strandendout,
                               @WebParam(name = "outputtree") String outputtree,
                               @WebParam(name = "seed") String seed,
                               @WebParam(name = "kimura") String kimura,
                               @WebParam(name = "tossgaps") String tossgaps,
                               @WebParam(name = "bootlabels") String bootlabels,
                               @WebParam(name = "clustering") String clustering,
                               @WebParam(name = "batch") String batch) {
        logger.debug("Web Services - runClustalw2() acknowledged");
        Clustalw2Task clustalw2Task = new Clustalw2Task();
        clustalw2Task.setOwner(username);
        clustalw2Task.setParameter(Clustalw2Task.PARAM_project, project);
        if (null != workSessionId && !"".equals(workSessionId)) {
            clustalw2Task.setParentTaskId(Long.valueOf(workSessionId));
        }
        clustalw2Task.setJobName(jobName);
        clustalw2Task.setParameter(Clustalw2Task.PARAM_fasta_input_node_id, fastaInputNodeId);
        clustalw2Task.setParameter(Clustalw2Task.PARAM_fasta_input_file_list, fastaInputFileList);

        // VERBS input
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_align, align);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_tree, tree);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_bootstrap, bootstrap);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_convert, convert);

        // General Settings Parameters
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_quicktree, quicktree);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_type, type);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_negative, negative);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_output, output);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_outorder, outorder);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_clustal_case, clustal_case);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_seqnos, seqnos);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_seqno_range, seqno_range);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_range, range);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_maxseqlen, maxseqlen);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_quiet, quiet);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_stats, stats);

        // Fast Pairwaise Alignments Parameters
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_ktuple, ktuple);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_topdiags, topdiags);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_window, window);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_pairgap, pairgap);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_score, score);

        // Slow Pairwise Alignments Parameters
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_pwmatrix, pwmatrix);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_pwdnamatrix, pwdnamatrix);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_pwgapopen, pwgapopen);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_pwgapext, pwgapext);

        // Multiple Alignments Parameters
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_newtree, newtree);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_usetree, usetree);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_matrix, matrix);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_dnamatrix, dnamatrix);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_gapopen, gapopen);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_gapext, gapext);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_endgaps, endgaps);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_gapdist, gapdist);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_nopgap, nopgap);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_nohgap, nohgap);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_hgapresidues, hgapresidues);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_maxdiv, maxdiv);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_transweight, transweight);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_iteration, iteration);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_numiter, numiter);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_noweights, noweights);

        // Profile Alignments Paramaters
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_profile, profile);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_newtree1, newtree1);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_usetree1, usetree1);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_newtree2, newtree2);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_usetree2, usetree2);

        // Sequence to Profile Alignments Parameters
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_sequences, sequences);

        // Structure Alignments Parameters
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_nosecstr1, nosecstr1);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_nosecstr2, nosecstr2);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_secstrout, secstrout);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_helixgap, helixgap);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_strandgap, strandgap);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_loopgap, loopgap);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_terminalgap, terminalgap);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_helixendin, helixendin);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_helixendout, helixendout);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_strandendin, strandendin);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_strandendout, strandendout);

        //TREES Parameters
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_outputtree, outputtree);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_seed, seed);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_kimura, kimura);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_tossgaps, tossgaps);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_bootlabels, bootlabels);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_clustering, clustering);
        parameterHelper(clustalw2Task, Clustalw2Task.PARAM_batch, batch);

        logger.info("Web Services - runClustalw2() config complete - now submitting task");
        return saveAndSubmitJob(clustalw2Task, "Clustalw2");
    }

    public String runLegacy2Bsml(@WebParam(name = "username") String username,
                                 @WebParam(name = "token") String token,
                                 @WebParam(name = "project") String project,
                                 @WebParam(name = "workSessionId") String workSessionId,
                                 @WebParam(name = "jobName") String jobName,
                                 @WebParam(name = "backup") String backup,
                                 @WebParam(name = "db_username") String db_username,
                                 @WebParam(name = "password") String password,
                                 @WebParam(name = "mode") String mode,
                                 @WebParam(name = "fastadir") String fastadir,
                                 @WebParam(name = "rdbms") String rdbms,
                                 @WebParam(name = "host") String host,
                                 @WebParam(name = "schema") String schema,
                                 @WebParam(name = "no_misc_features") String no_misc_features,
                                 @WebParam(name = "no_repeat_features") String no_repeat_features,
                                 @WebParam(name = "no_transposon_features") String no_transposon_features,
                                 @WebParam(name = "no_id_generator") String no_id_generator,
                                 @WebParam(name = "input_id_mapping_files") String input_id_mapping_files,
                                 @WebParam(name = "input_id_mapping_directories") String input_id_mapping_directories,
                                 @WebParam(name = "idgen_identifier_version") String idgen_identifier_version,
                                 @WebParam(name = "no_die_null_sequences") String no_die_null_sequences,
                                 @WebParam(name = "sourcename") String sourcename,
                                 @WebParam(name = "control_file") String control_file,
                                 @WebParam(name = "root_project") String root_project) {
        logger.debug("Web Services - runLegacy2Bsml() acknowledged");
        Legacy2BsmlTask legacy2bsmlTask = new Legacy2BsmlTask();
        legacy2bsmlTask.setOwner(username);
        legacy2bsmlTask.setParameter(Legacy2BsmlTask.PARAM_project, project);
        if (null != workSessionId && !"".equals(workSessionId)) {
            legacy2bsmlTask.setParentTaskId(Long.valueOf(workSessionId));
        }
        legacy2bsmlTask.setJobName(jobName);

        parameterHelper(legacy2bsmlTask, Legacy2BsmlTask.PARAM_backup, backup);
        parameterHelper(legacy2bsmlTask, Legacy2BsmlTask.PARAM_db_username, db_username);
        parameterHelper(legacy2bsmlTask, Legacy2BsmlTask.PARAM_password, password);
        parameterHelper(legacy2bsmlTask, Legacy2BsmlTask.PARAM_mode, mode);
        parameterHelper(legacy2bsmlTask, Legacy2BsmlTask.PARAM_fastadir, fastadir);
        parameterHelper(legacy2bsmlTask, Legacy2BsmlTask.PARAM_rdbms, rdbms);
        parameterHelper(legacy2bsmlTask, Legacy2BsmlTask.PARAM_host, host);
        parameterHelper(legacy2bsmlTask, Legacy2BsmlTask.PARAM_schema, schema);
        parameterHelper(legacy2bsmlTask, Legacy2BsmlTask.PARAM_no_misc_features, no_misc_features);
        parameterHelper(legacy2bsmlTask, Legacy2BsmlTask.PARAM_no_repeat_features, no_repeat_features);
        parameterHelper(legacy2bsmlTask, Legacy2BsmlTask.PARAM_no_transposon_features, no_transposon_features);
        parameterHelper(legacy2bsmlTask, Legacy2BsmlTask.PARAM_no_id_generator, no_id_generator);
        parameterHelper(legacy2bsmlTask, Legacy2BsmlTask.PARAM_input_id_mapping_files, input_id_mapping_files);
        parameterHelper(legacy2bsmlTask, Legacy2BsmlTask.PARAM_input_id_mapping_directories, input_id_mapping_directories);
        parameterHelper(legacy2bsmlTask, Legacy2BsmlTask.PARAM_idgen_identifier_version, idgen_identifier_version);
        parameterHelper(legacy2bsmlTask, Legacy2BsmlTask.PARAM_no_die_null_sequences, no_die_null_sequences);
        parameterHelper(legacy2bsmlTask, Legacy2BsmlTask.PARAM_sourcename, sourcename);
        parameterHelper(legacy2bsmlTask, Legacy2BsmlTask.PARAM_control_file, control_file);
        parameterHelper(legacy2bsmlTask, Legacy2BsmlTask.PARAM_root_project, root_project);

        logger.info("Web Services - runLegacy2Bsml() config complete - now submitting task");
        return saveAndSubmitJob(legacy2bsmlTask, "Legacy2Bsml");
    }


    public String runFasta2Bsml(@WebParam(name = "username") String username,
                                @WebParam(name = "token") String token,
                                @WebParam(name = "project") String project,
                                @WebParam(name = "workSessionId") String workSessionId,
                                @WebParam(name = "jobName") String jobName,
                                @WebParam(name = "fasta_input") String fasta_input,
                                @WebParam(name = "fasta_list") String fasta_list,
                                @WebParam(name = "format") String format,
                                @WebParam(name = "type_class") String type_class,
                                @WebParam(name = "organism") String organism,
                                @WebParam(name = "genus") String genus,
                                @WebParam(name = "species") String species) {
        logger.debug("Web Services - runFasta2Bsml() acknowledged");
        Fasta2BsmlTask fasta2bsmlTask = new Fasta2BsmlTask();
        fasta2bsmlTask.setOwner(username);
        fasta2bsmlTask.setParameter(Fasta2BsmlTask.PARAM_project, project);
        if (null != workSessionId && !"".equals(workSessionId)) {
            fasta2bsmlTask.setParentTaskId(Long.valueOf(workSessionId));
        }
        fasta2bsmlTask.setJobName(jobName);

        parameterHelper(fasta2bsmlTask, Fasta2BsmlTask.PARAM_fasta_input, fasta_input);
        parameterHelper(fasta2bsmlTask, Fasta2BsmlTask.PARAM_fasta_list, fasta_list);
        parameterHelper(fasta2bsmlTask, Fasta2BsmlTask.PARAM_format, format);
        parameterHelper(fasta2bsmlTask, Fasta2BsmlTask.PARAM_class, type_class);
        parameterHelper(fasta2bsmlTask, Fasta2BsmlTask.PARAM_organism, organism);
        parameterHelper(fasta2bsmlTask, Fasta2BsmlTask.PARAM_genus, genus);
        parameterHelper(fasta2bsmlTask, Fasta2BsmlTask.PARAM_species, species);

        logger.info("Web Services - runFasta2Bsml() config complete - now submitting task");
        return saveAndSubmitJob(fasta2bsmlTask, "Fasta2Bsml");
    }

    public String runEvidenceModeler(@WebParam(name = "username") String username,
                                     @WebParam(name = "token") String token,
                                     @WebParam(name = "project") String project,
                                     @WebParam(name = "workSessionId") String workSessionId,
                                     @WebParam(name = "jobName") String jobName,
                                     @WebParam(name = "fastaInputNodeId") String fastaInputNodeId,
                                     @WebParam(name = "weights") String weights,
                                     @WebParam(name = "gene_predictions") String gene_predictions,
                                     @WebParam(name = "protein_alignments") String protein_alignments,
                                     @WebParam(name = "transcript_alignments") String transcript_alignments,
                                     @WebParam(name = "repeats") String repeats,
                                     @WebParam(name = "terminalExons") String terminalExons,
                                     @WebParam(name = "stitch_ends") String stitch_ends,
                                     @WebParam(name = "extend_to_terminal") String extend_to_terminal,
                                     @WebParam(name = "stop_codons") String stop_codons,
                                     @WebParam(name = "min_intron_length") String min_intron_length,
                                     @WebParam(name = "INTERGENIC_SCORE_ADJUST_FACTOR") String INTERGENIC_SCORE_ADJUST_FACTOR,
                                     @WebParam(name = "exec_dir") String exec_dir,
                                     @WebParam(name = "forwardStrandOnly") String forwardStrandOnly,
                                     @WebParam(name = "reverseStrandOnly") String reverseStrandOnly,
                                     @WebParam(name = "verbose") String verbose,
                                     @WebParam(name = "debug") String debug,
                                     @WebParam(name = "report_ELM") String report_ELM,
                                     @WebParam(name = "RECURSE") String RECURSE,
                                     @WebParam(name = "limit_range_lend") String limit_range_lend,
                                     @WebParam(name = "limit_range_rend") String limit_range_rend,
                                     @WebParam(name = "segmentSize") String segmentSize,
                                     @WebParam(name = "overlapSize") String overlapSize) {
        logger.debug("Web Services - runEvidenceModeler() acknowledged");
        EvidenceModelerTask evidenceModelerTask = new EvidenceModelerTask();
        evidenceModelerTask.setOwner(username);
        evidenceModelerTask.setParameter(EvidenceModelerTask.PARAM_project, project);
        if (null != workSessionId && !"".equals(workSessionId)) {
            evidenceModelerTask.setParentTaskId(Long.valueOf(workSessionId));
        }
        evidenceModelerTask.setJobName(jobName);
        evidenceModelerTask.setParameter(EvidenceModelerTask.PARAM_fastaInputNodeId, fastaInputNodeId);
        parameterHelper(evidenceModelerTask, EvidenceModelerTask.PARAM_weights, weights);
        parameterHelper(evidenceModelerTask, EvidenceModelerTask.PARAM_gene_predictions, gene_predictions);
        parameterHelper(evidenceModelerTask, EvidenceModelerTask.PARAM_protein_alignments, protein_alignments);
        parameterHelper(evidenceModelerTask, EvidenceModelerTask.PARAM_transcript_alignments, transcript_alignments);
        parameterHelper(evidenceModelerTask, EvidenceModelerTask.PARAM_repeats, repeats);
        parameterHelper(evidenceModelerTask, EvidenceModelerTask.PARAM_terminalExons, terminalExons);
        parameterHelper(evidenceModelerTask, EvidenceModelerTask.PARAM_stitch_ends, stitch_ends);
        parameterHelper(evidenceModelerTask, EvidenceModelerTask.PARAM_extend_to_terminal, extend_to_terminal);
        parameterHelper(evidenceModelerTask, EvidenceModelerTask.PARAM_stop_codons, stop_codons);
        parameterHelper(evidenceModelerTask, EvidenceModelerTask.PARAM_min_intron_length, min_intron_length);
        parameterHelper(evidenceModelerTask, EvidenceModelerTask.PARAM_INTERGENIC_SCORE_ADJUST_FACTOR, INTERGENIC_SCORE_ADJUST_FACTOR);
        parameterHelper(evidenceModelerTask, EvidenceModelerTask.PARAM_exec_dir, exec_dir);
        parameterHelper(evidenceModelerTask, EvidenceModelerTask.PARAM_forwardStrandOnly, forwardStrandOnly);
        parameterHelper(evidenceModelerTask, EvidenceModelerTask.PARAM_reverseStrandOnly, reverseStrandOnly);
        parameterHelper(evidenceModelerTask, EvidenceModelerTask.PARAM_verbose, verbose);
        parameterHelper(evidenceModelerTask, EvidenceModelerTask.PARAM_debug, debug);
        parameterHelper(evidenceModelerTask, EvidenceModelerTask.PARAM_report_ELM, report_ELM);
        parameterHelper(evidenceModelerTask, EvidenceModelerTask.PARAM_RECURSE, RECURSE);
        parameterHelper(evidenceModelerTask, EvidenceModelerTask.PARAM_limit_range_lend, limit_range_lend);
        parameterHelper(evidenceModelerTask, EvidenceModelerTask.PARAM_limit_range_rend, limit_range_rend);
        parameterHelper(evidenceModelerTask, EvidenceModelerTask.PARAM_segmentSize, segmentSize);
        parameterHelper(evidenceModelerTask, EvidenceModelerTask.PARAM_overlapSize, overlapSize);

        logger.info("Web Services - runEvidenceModeler() config complete - now submitting task");
        return saveAndSubmitJob(evidenceModelerTask, "EvidenceModeler");
    }

    public String runExonerate(@WebParam(name = "username") String username,
                               @WebParam(name = "token") String token,
                               @WebParam(name = "project") String project,
                               @WebParam(name = "workSessionId") String workSessionId,
                               @WebParam(name = "jobName") String jobName,
                               // Sequence Input Options
                               @WebParam(name = "query_fasta_node_id") String query_fasta_node_id,
                               @WebParam(name = "target_fasta_node_id") String target_fasta_node_id,
                               @WebParam(name = "querytype") String querytype,
                               @WebParam(name = "targettype") String targettype,
                               @WebParam(name = "querychunkidtype") String querychunkidtype,
                               @WebParam(name = "targetchunkidtype") String targetchunkidtype,
                               @WebParam(name = "querychunktotaltype") String querychunktotaltype,
                               @WebParam(name = "targetchunktotaltype") String targetchunktotaltype,
                               @WebParam(name = "verbose") String verbose,
                               // Analysis Options
                               @WebParam(name = "exhaustive") String exhaustive,
                               @WebParam(name = "bigseq") String bigseq,
                               @WebParam(name = "forcescan") String forcescan,
                               @WebParam(name = "saturatethreshold") String saturatethreshold,
                               @WebParam(name = "customserver") String customserver,
                               // Fasta Database Options
                               @WebParam(name = "fastasuffix") String fastasuffix,
                               // Gapped Alignment Options
                               @WebParam(name = "model") String model,
                               @WebParam(name = "score") String score,
                               @WebParam(name = "percent") String percent,
                               @WebParam(name = "showalignment") String showalignment,
                               @WebParam(name = "showsugar") String showsugar,
                               @WebParam(name = "showcigar") String showcigar,
                               @WebParam(name = "showvulgar") String showvulgar,
                               @WebParam(name = "showquerygff") String showquerygff,
                               @WebParam(name = "showtargetgff") String showtargetgff,
                               @WebParam(name = "ryo") String ryo,
                               @WebParam(name = "bestn") String bestn,
                               @WebParam(name = "subopt") String subopt,
                               @WebParam(name = "gappedextension") String gappedextension,
                               @WebParam(name = "refine") String refine,
                               @WebParam(name = "refineboundary") String refineboundary,
                               // Viterbi algorithm options
                               @WebParam(name = "dpmemory") String dpmemory,
                               // Code generation options
                               @WebParam(name = "compiled") String compiled,
                               // Heuristic Options
                               @WebParam(name = "terminalrangeint") String terminalrangeint,
                               @WebParam(name = "terminalrangeext") String terminalrangeext,
                               @WebParam(name = "joinrangeint") String joinrangeint,
                               @WebParam(name = "joinrangeext") String joinrangeext,
                               @WebParam(name = "spanrangeint") String spanrangeint,
                               @WebParam(name = "spanrangeext") String spanrangeext,
                               // Seeded Dynamic Programming options
                               @WebParam(name = "extensionthreshold") String extensionthreshold,
                               @WebParam(name = "singlepass") String singlepass,
                               // BSDP algorithm options
                               @WebParam(name = "joinfilter") String joinfilter,
                               // Sequence Options
                               @WebParam(name = "annotation") String annotation,
                               // Symbol Comparison Options
                               @WebParam(name = "softmaskquery") String softmaskquery,
                               @WebParam(name = "softmasktarget") String softmasktarget,
                               @WebParam(name = "dnasubmat") String dnasubmat,
                               @WebParam(name = "proteinsubmat") String proteinsubmat,
                               // Alignment Seeding Options
                               @WebParam(name = "fsmmemory") String fsmmemory,
                               @WebParam(name = "forcefsm") String forcefsm,
                               @WebParam(name = "wordjump") String wordjump,
                               // Affine Model Options
                               @WebParam(name = "gapopen") String gapopen,
                               @WebParam(name = "gapextend") String gapextend,
                               @WebParam(name = "codongapopen") String codongapopen,
                               @WebParam(name = "codongapextend") String codongapextend,
                               // NER Model Options
                               @WebParam(name = "minner") String minner,
                               @WebParam(name = "maxner") String maxner,
                               @WebParam(name = "neropen") String neropen,
                               // Intron Modelling Options
                               @WebParam(name = "minintron") String minintron,
                               @WebParam(name = "maxintron") String maxintron,
                               @WebParam(name = "intronpenalty") String intronpenalty,
                               // Frameshift Options
                               @WebParam(name = "frameshift") String frameshift,
                               // Alphabet Options
                               @WebParam(name = "useaatla") String useaatla,
                               // Translation Options
                               @WebParam(name = "geneticcode") String geneticcode,
                               // HSP creation options
                               @WebParam(name = "hspfilter") String hspfilter,
                               @WebParam(name = "useworddropoff") String useworddropoff,
                               @WebParam(name = "seedrepeat") String seedrepeat,
                               @WebParam(name = "dnawordlen") String dnawordlen,
                               @WebParam(name = "proteinwordlen") String proteinwordlen,
                               @WebParam(name = "codonwordlen") String codonwordlen,
                               @WebParam(name = "dnahspdropoff") String dnahspdropoff,
                               @WebParam(name = "proteinhspdropoff") String proteinhspdropoff,
                               @WebParam(name = "codonhspdropoff") String codonhspdropoff,
                               @WebParam(name = "dnahspthreshold") String dnahspthreshold,
                               @WebParam(name = "proteinhspthreshold") String proteinhspthreshold,
                               @WebParam(name = "codonhspthreshold") String codonhspthreshold,
                               @WebParam(name = "dnawordlimit") String dnawordlimit,
                               @WebParam(name = "proteinwordlimit") String proteinwordlimit,
                               @WebParam(name = "codonwordlimit") String codonwordlimit,
                               @WebParam(name = "geneseed") String geneseed,
                               @WebParam(name = "geneseedrepeat") String geneseedrepeat,
                               // Alignment options
                               @WebParam(name = "alignmentwidth") String alignmentwidth,
                               @WebParam(name = "forwardcoordinates") String forwardcoordinates,
                               // SAR Options
                               @WebParam(name = "quality") String quality,
                               // Splice Site Prediction Options
                               @WebParam(name = "splice3") String splice3,
                               @WebParam(name = "splice5") String splice5,
                               @WebParam(name = "forcegtag") String forcegtag) {
        logger.debug("Web Services - runExonerate() acknowledged");
        ExonerateTask exonerateTask = new ExonerateTask();
        exonerateTask.setOwner(username);
        exonerateTask.setParameter(ExonerateTask.PARAM_project, project);
        if (null != workSessionId && !"".equals(workSessionId)) {
            exonerateTask.setParentTaskId(Long.valueOf(workSessionId));
        }
        exonerateTask.setJobName(jobName);
        // Sequence Input Options
        exonerateTask.setParameter(ExonerateTask.PARAM_query_fasta_node_id, query_fasta_node_id);
        exonerateTask.setParameter(ExonerateTask.PARAM_target_fasta_node_id, target_fasta_node_id);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_querytype, querytype);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_targettype, targettype);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_querychunkidtype, querychunkidtype);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_targetchunkidtype, targetchunkidtype);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_querychunktotaltype, querychunktotaltype);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_targetchunktotaltype, targetchunktotaltype);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_verbose, verbose);
        // Analysis Options
        parameterHelper(exonerateTask, ExonerateTask.PARAM_exhaustive, exhaustive);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_bigseq, bigseq);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_forcescan, forcescan);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_saturatethreshold, saturatethreshold);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_customserver, customserver);
        // Fasta Database Options
        parameterHelper(exonerateTask, ExonerateTask.PARAM_fastasuffix, fastasuffix);
        // Gapped Alignment Options
        parameterHelper(exonerateTask, ExonerateTask.PARAM_model, model);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_score, score);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_percent, percent);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_showalignment, showalignment);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_showsugar, showsugar);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_showcigar, showcigar);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_showvulgar, showvulgar);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_showquerygff, showquerygff);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_showtargetgff, showtargetgff);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_ryo, ryo);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_bestn, bestn);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_subopt, subopt);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_gappedextension, gappedextension);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_refine, refine);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_refineboundary, refineboundary);
        // Viterbi algorithm options
        parameterHelper(exonerateTask, ExonerateTask.PARAM_dpmemory, dpmemory);
        // Code generation options
        parameterHelper(exonerateTask, ExonerateTask.PARAM_compiled, compiled);
        // Heuristic Options
        parameterHelper(exonerateTask, ExonerateTask.PARAM_terminalrangeint, terminalrangeint);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_terminalrangeext, terminalrangeext);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_joinrangeint, joinrangeint);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_joinrangeext, joinrangeext);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_spanrangeint, spanrangeint);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_spanrangeext, spanrangeext);
        // Seeded Dynamic Programming options
        parameterHelper(exonerateTask, ExonerateTask.PARAM_extensionthreshold, extensionthreshold);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_singlepass, singlepass);
        // BSDP algorithm options
        parameterHelper(exonerateTask, ExonerateTask.PARAM_joinfilter, joinfilter);
        // Sequence Options
        parameterHelper(exonerateTask, ExonerateTask.PARAM_annotation, annotation);
        // Symbol Comparison Options
        parameterHelper(exonerateTask, ExonerateTask.PARAM_softmaskquery, softmaskquery);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_softmasktarget, softmasktarget);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_dnasubmat, dnasubmat);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_proteinsubmat, proteinsubmat);
        // Alignment Seeding Options
        parameterHelper(exonerateTask, ExonerateTask.PARAM_fsmmemory, fsmmemory);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_forcefsm, forcefsm);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_wordjump, wordjump);
        // Affine Model Options
        parameterHelper(exonerateTask, ExonerateTask.PARAM_gapopen, gapopen);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_gapextend, gapextend);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_codongapopen, codongapopen);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_codongapextend, codongapextend);
        // NER Model Options
        parameterHelper(exonerateTask, ExonerateTask.PARAM_minner, minner);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_maxner, maxner);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_neropen, neropen);
        // Intron Modelling Options
        parameterHelper(exonerateTask, ExonerateTask.PARAM_minintron, minintron);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_maxintron, maxintron);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_intronpenalty, intronpenalty);
        // Frameshift Options
        parameterHelper(exonerateTask, ExonerateTask.PARAM_frameshift, frameshift);
        // Alphabet Options
        parameterHelper(exonerateTask, ExonerateTask.PARAM_useaatla, useaatla);
        // Translation Options
        parameterHelper(exonerateTask, ExonerateTask.PARAM_geneticcode, geneticcode);
        // HSP creation options
        parameterHelper(exonerateTask, ExonerateTask.PARAM_hspfilter, hspfilter);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_useworddropoff, useworddropoff);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_seedrepeat, seedrepeat);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_dnawordlen, dnawordlen);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_proteinwordlen, proteinwordlen);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_codonwordlen, codonwordlen);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_dnahspdropoff, dnahspdropoff);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_proteinhspdropoff, proteinhspdropoff);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_codonhspdropoff, codonhspdropoff);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_dnahspthreshold, dnahspthreshold);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_proteinhspthreshold, proteinhspthreshold);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_codonhspthreshold, codonhspthreshold);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_dnawordlimit, dnawordlimit);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_proteinwordlimit, proteinwordlimit);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_codonwordlimit, codonwordlimit);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_geneseed, geneseed);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_geneseedrepeat, geneseedrepeat);
        // Alignment options
        parameterHelper(exonerateTask, ExonerateTask.PARAM_alignmentwidth, alignmentwidth);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_forwardcoordinates, forwardcoordinates);
        // SAR Options
        parameterHelper(exonerateTask, ExonerateTask.PARAM_quality, quality);
        // Splice Site Prediction Options
        parameterHelper(exonerateTask, ExonerateTask.PARAM_splice3, splice3);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_splice5, splice5);
        parameterHelper(exonerateTask, ExonerateTask.PARAM_forcegtag, forcegtag);
        logger.info("Web Services - runExonerate() config complete - now submitting task");
        return saveAndSubmitJob(exonerateTask, "Exonerate");
    }

    public String runAugustus(@WebParam(name = "username") String username,
                              @WebParam(name = "token") String token,
                              @WebParam(name = "project") String project,
                              @WebParam(name = "workSessionId") String workSessionId,
                              @WebParam(name = "jobName") String jobName,
                              @WebParam(name = "fastaInputNodeId") String fastaInputNodeId,
                              @WebParam(name = "species") String species,
                              @WebParam(name = "strand") String strand,
                              @WebParam(name = "geneModel") String geneModel,
                              @WebParam(name = "singleStrand") String singleStrand,
                              @WebParam(name = "hintsFile") String hintsFile,
                              @WebParam(name = "augustusConfigPath") String augustusConfigPath,
                              @WebParam(name = "alternativesFromEvidence") String alternativesFromEvidence,
                              @WebParam(name = "alternativesFromSampling") String alternativesFromSampling,
                              @WebParam(name = "sample") String sample,
                              @WebParam(name = "minExonIntronProb") String minExonIntronProb,
                              @WebParam(name = "minMeanExonIntronProb") String minMeanExonIntronProb,
                              @WebParam(name = "maxTracks") String maxTracks,
                              @WebParam(name = "progress") String progress,
                              @WebParam(name = "gff3") String gff3,
                              @WebParam(name = "predictionStart") String predictionStart,
                              @WebParam(name = "predictionEnd") String predictionEnd,
                              @WebParam(name = "UTR") String UTR,
                              @WebParam(name = "noInFrameStop") String noInFrameStop,
                              @WebParam(name = "noPrediction") String noPrediction,
                              @WebParam(name = "uniqueGeneId") String uniqueGeneId) {
        logger.debug("Web Services - runAugustus() acknowledged");
        AugustusTask augustusTask = new AugustusTask();
        augustusTask.setOwner(username);
        augustusTask.setParameter(AugustusTask.PARAM_project, project);
        if (null != workSessionId && !"".equals(workSessionId)) {
            augustusTask.setParentTaskId(Long.valueOf(workSessionId));
        }
        augustusTask.setJobName(jobName);
        augustusTask.setParameter(AugustusTask.PARAM_fasta_input_node_id, fastaInputNodeId);
        parameterHelper(augustusTask, AugustusTask.PARAM_species, species);
        parameterHelper(augustusTask, AugustusTask.PARAM_strand, strand);
        parameterHelper(augustusTask, AugustusTask.PARAM_geneModel, geneModel);
        parameterHelper(augustusTask, AugustusTask.PARAM_singleStrand, singleStrand);
        parameterHelper(augustusTask, AugustusTask.PARAM_singleStrand, singleStrand);
        parameterHelper(augustusTask, AugustusTask.PARAM_hintsFile, hintsFile);
        parameterHelper(augustusTask, AugustusTask.PARAM_augustusConfigPath, augustusConfigPath);
        parameterHelper(augustusTask, AugustusTask.PARAM_alternativesFromEvidence, alternativesFromEvidence);
        parameterHelper(augustusTask, AugustusTask.PARAM_alternativesFromSampling, alternativesFromSampling);
        parameterHelper(augustusTask, AugustusTask.PARAM_sample, sample);
        parameterHelper(augustusTask, AugustusTask.PARAM_minExonIntronProb, minExonIntronProb);
        parameterHelper(augustusTask, AugustusTask.PARAM_minMeanExonIntronProb, minMeanExonIntronProb);
        parameterHelper(augustusTask, AugustusTask.PARAM_maxTracks, maxTracks);
        parameterHelper(augustusTask, AugustusTask.PARAM_progress, progress);
        parameterHelper(augustusTask, AugustusTask.PARAM_gff3, gff3);
        parameterHelper(augustusTask, AugustusTask.PARAM_predictionStart, predictionStart);
        parameterHelper(augustusTask, AugustusTask.PARAM_predictionEnd, predictionEnd);
        parameterHelper(augustusTask, AugustusTask.PARAM_UTR, UTR);
        parameterHelper(augustusTask, AugustusTask.PARAM_noInFrameStop, noInFrameStop);
        parameterHelper(augustusTask, AugustusTask.PARAM_noPrediction, noPrediction);
        parameterHelper(augustusTask, AugustusTask.PARAM_uniqueGeneId, uniqueGeneId);

        logger.info("Web Services - runAugustus() config complete - now submitting task");
        return saveAndSubmitJob(augustusTask, "Augustus");
    }

    public String runTrf(@WebParam(name = "username") String username,
                         @WebParam(name = "token") String token,
                         @WebParam(name = "project") String project,
                         @WebParam(name = "workSessionId") String workSessionId,
                         @WebParam(name = "jobName") String jobName,
                         @WebParam(name = "fastaInputNodeId") String fastaInputNodeId,
                         @WebParam(name = "matchingWeight") String matchingWeight,
                         @WebParam(name = "mismatching_penalty") String mismatching_penalty,
                         @WebParam(name = "indel_penalty") String indel_penalty,
                         @WebParam(name = "match_probability") String match_probability,
                         @WebParam(name = "indel_probability") String indel_probability,
                         @WebParam(name = "minscore") String minscore,
                         @WebParam(name = "maxperiod") String maxperiod,
                         @WebParam(name = "masked_sequence_file") String masked_sequence_file,
                         @WebParam(name = "flanking_sequence") String flanking_sequence,
                         @WebParam(name = "data_file") String data_file,
                         @WebParam(name = "suppress_html_input") String suppress_html_input) {
        logger.debug("Web Services - runTrf() acknowledged");
        TrfTask trfTask = new TrfTask();
        trfTask.setOwner(username);
        trfTask.setParameter(TrfTask.PARAM_project, project);
        if (null != workSessionId && !"".equals(workSessionId)) {
            trfTask.setParentTaskId(Long.valueOf(workSessionId));
        }
        trfTask.setJobName(jobName);
        trfTask.setParameter(TrfTask.PARAM_fasta_input_node_id, fastaInputNodeId);
        parameterHelper(trfTask, TrfTask.PARAM_matching_weight, matchingWeight);
        parameterHelper(trfTask, TrfTask.PARAM_mismatching_penalty, mismatching_penalty);
        parameterHelper(trfTask, TrfTask.PARAM_indel_penalty, indel_penalty);
        parameterHelper(trfTask, TrfTask.PARAM_match_probability, match_probability);
        parameterHelper(trfTask, TrfTask.PARAM_indel_probability, indel_probability);
        parameterHelper(trfTask, TrfTask.PARAM_minscore, minscore);
        parameterHelper(trfTask, TrfTask.PARAM_maxperiod, maxperiod);
        parameterHelper(trfTask, TrfTask.PARAM_masked_sequence_file, masked_sequence_file);
        parameterHelper(trfTask, TrfTask.PARAM_flanking_sequence, flanking_sequence);
        parameterHelper(trfTask, TrfTask.PARAM_data_file, data_file);
        parameterHelper(trfTask, TrfTask.PARAM_suppress_html_output, suppress_html_input);

        logger.info("Web Services - runTrf() config complete - now submitting task");
        return saveAndSubmitJob(trfTask, "Trf");
    }

    public String runGenezilla(@WebParam(name = "username") String username,
                               @WebParam(name = "token") String token,
                               @WebParam(name = "project") String project,
                               @WebParam(name = "workSessionId") String workSessionId,
                               @WebParam(name = "jobName") String jobName,
                               @WebParam(name = "fastaInputNodeId") String fastaInputNodeId,
                               @WebParam(name = "isoInputNodeId") String isoInputNodeId,
                               @WebParam(name = "cpgIslandPredictionFile") String cpgIslandPredictionFile,
                               @WebParam(name = "isochorePredictionFile") String isochorePredictionFile,
                               @WebParam(name = "ignoreShortFasta") String ignoreShortFasta) {
        logger.debug("Web Services - runGenezilla() acknowledged");
        GenezillaTask genezillaTask = new GenezillaTask();
        genezillaTask.setOwner(username);
        genezillaTask.setParameter(GenezillaTask.PARAM_project, project);
        if (null != workSessionId && !"".equals(workSessionId)) {
            genezillaTask.setParentTaskId(Long.valueOf(workSessionId));
        }
        genezillaTask.setJobName(jobName);
        genezillaTask.setParameter(GenezillaTask.PARAM_fasta_input_node_id, fastaInputNodeId);
        parameterHelper(genezillaTask, GenezillaTask.PARAM_iso_input_node_id, isoInputNodeId);
        parameterHelper(genezillaTask, GenezillaTask.PARAM_cpg_island_prediction_file, cpgIslandPredictionFile);
        parameterHelper(genezillaTask, GenezillaTask.PARAM_isochore_prediction_file, isochorePredictionFile);
        parameterHelper(genezillaTask, GenezillaTask.PARAM_ignore_short_fasta, ignoreShortFasta);

        logger.info("Web Services - runGenezilla() config complete - now submitting task");
        return saveAndSubmitJob(genezillaTask, "Genezilla");
    }

    public String runRepeatMasker(@WebParam(name = "username") String username,
                                  @WebParam(name = "token") String token,
                                  @WebParam(name = "project") String project,
                                  @WebParam(name = "workSessionId") String workSessionId,
                                  @WebParam(name = "jobName") String jobName,
                                  @WebParam(name = "fastaInputNodeId") String fastaInputNodeId,
                                  @WebParam(name = "nolow") String nolow,
                                  @WebParam(name = "noint") String noint,
                                  @WebParam(name = "norna") String norna,
                                  @WebParam(name = "alu") String alu,
                                  @WebParam(name = "div") String div,
                                  @WebParam(name = "lib") String lib,
                                  @WebParam(name = "cutoff") String cutoff,
                                  @WebParam(name = "species") String species,
                                  @WebParam(name = "is_only") String is_only,
                                  @WebParam(name = "is_clip") String is_clip,
                                  @WebParam(name = "no_is") String no_is,
                                  @WebParam(name = "rodspec") String rodspec,
                                  @WebParam(name = "primspec") String primspec,
                                  @WebParam(name = "wublast") String wublast,
                                  @WebParam(name = "s") String s,
                                  @WebParam(name = "q") String q,
                                  @WebParam(name = "qq") String qq,
                                  @WebParam(name = "gc") String gc,
                                  @WebParam(name = "gccalc") String gccalc,
                                  @WebParam(name = "frag") String frag,
                                  @WebParam(name = "maxsize") String maxsize,
                                  @WebParam(name = "nocut") String nocut,
                                  @WebParam(name = "noisy") String noisy,
                                  @WebParam(name = "ali") String ali,
                                  @WebParam(name = "inv") String inv,
                                  @WebParam(name = "cut") String cut,
                                  @WebParam(name = "small") String small,
                                  @WebParam(name = "xsmall") String xsmall,
                                  @WebParam(name = "x") String x,
                                  @WebParam(name = "poly") String poly,
                                  @WebParam(name = "ace") String ace,
                                  @WebParam(name = "gff") String gff,
                                  @WebParam(name = "u") String u,
                                  @WebParam(name = "xm") String xm,
                                  @WebParam(name = "fixed") String fixed,
                                  @WebParam(name = "no_id") String no_id,
                                  @WebParam(name = "excln") String excln) {
        logger.debug("Web Services - runRepeatMasker() acknowledged");
        RepeatMaskerTask repeatMaskerTask = new RepeatMaskerTask();
        repeatMaskerTask.setOwner(username);
        repeatMaskerTask.setParameter(RepeatMaskerTask.PARAM_project, project);
        if (null != workSessionId && !"".equals(workSessionId)) {
            repeatMaskerTask.setParentTaskId(Long.valueOf(workSessionId));
        }
        repeatMaskerTask.setJobName(jobName);
        repeatMaskerTask.setParameter(RepeatMaskerTask.PARAM_fasta_input_node_id, fastaInputNodeId);
        parameterHelper(repeatMaskerTask, RepeatMaskerTask.PARAM_nolow, nolow);
        parameterHelper(repeatMaskerTask, RepeatMaskerTask.PARAM_noint, noint);
        parameterHelper(repeatMaskerTask, RepeatMaskerTask.PARAM_norna, norna);
        parameterHelper(repeatMaskerTask, RepeatMaskerTask.PARAM_alu, alu);
        parameterHelper(repeatMaskerTask, RepeatMaskerTask.PARAM_div, div);
        parameterHelper(repeatMaskerTask, RepeatMaskerTask.PARAM_lib, lib);
        parameterHelper(repeatMaskerTask, RepeatMaskerTask.PARAM_cutoff, cutoff);
        parameterHelper(repeatMaskerTask, RepeatMaskerTask.PARAM_species, species);
        parameterHelper(repeatMaskerTask, RepeatMaskerTask.PARAM_is_only, is_only);
        parameterHelper(repeatMaskerTask, RepeatMaskerTask.PARAM_is_clip, is_clip);
        parameterHelper(repeatMaskerTask, RepeatMaskerTask.PARAM_no_is, no_is);
        parameterHelper(repeatMaskerTask, RepeatMaskerTask.PARAM_rodspec, rodspec);
        parameterHelper(repeatMaskerTask, RepeatMaskerTask.PARAM_primspec, primspec);
        parameterHelper(repeatMaskerTask, RepeatMaskerTask.PARAM_wublast, wublast);
        parameterHelper(repeatMaskerTask, RepeatMaskerTask.PARAM_s, s);
        parameterHelper(repeatMaskerTask, RepeatMaskerTask.PARAM_q, q);
        parameterHelper(repeatMaskerTask, RepeatMaskerTask.PARAM_qq, qq);
        parameterHelper(repeatMaskerTask, RepeatMaskerTask.PARAM_gc, gc);
        parameterHelper(repeatMaskerTask, RepeatMaskerTask.PARAM_gccalc, gccalc);
        parameterHelper(repeatMaskerTask, RepeatMaskerTask.PARAM_frag, frag);
        parameterHelper(repeatMaskerTask, RepeatMaskerTask.PARAM_maxsize, maxsize);
        parameterHelper(repeatMaskerTask, RepeatMaskerTask.PARAM_nocut, nocut);
        parameterHelper(repeatMaskerTask, RepeatMaskerTask.PARAM_noisy, noisy);
        parameterHelper(repeatMaskerTask, RepeatMaskerTask.PARAM_ali, ali);
        parameterHelper(repeatMaskerTask, RepeatMaskerTask.PARAM_inv, inv);
        parameterHelper(repeatMaskerTask, RepeatMaskerTask.PARAM_cut, cut);
        parameterHelper(repeatMaskerTask, RepeatMaskerTask.PARAM_small, small);
        parameterHelper(repeatMaskerTask, RepeatMaskerTask.PARAM_xsmall, xsmall);
        parameterHelper(repeatMaskerTask, RepeatMaskerTask.PARAM_x, x);
        parameterHelper(repeatMaskerTask, RepeatMaskerTask.PARAM_poly, poly);
        parameterHelper(repeatMaskerTask, RepeatMaskerTask.PARAM_ace, ace);
        parameterHelper(repeatMaskerTask, RepeatMaskerTask.PARAM_gff, gff);
        parameterHelper(repeatMaskerTask, RepeatMaskerTask.PARAM_u, u);
        parameterHelper(repeatMaskerTask, RepeatMaskerTask.PARAM_xm, xm);
        parameterHelper(repeatMaskerTask, RepeatMaskerTask.PARAM_fixed, fixed);
        parameterHelper(repeatMaskerTask, RepeatMaskerTask.PARAM_no_id, no_id);
        parameterHelper(repeatMaskerTask, RepeatMaskerTask.PARAM_excln, excln);

        logger.info("Web Services - runRepeatMasker() config complete - now submitting task");
        return saveAndSubmitJob(repeatMaskerTask, "RepeatMasker");
    }

    public String runFgenesh(@WebParam(name = "username") String username,
                             @WebParam(name = "token") String token,
                             @WebParam(name = "project") String project,
                             @WebParam(name = "workSessionId") String workSessionId,
                             @WebParam(name = "jobName") String jobName,
                             @WebParam(name = "fastaInputNodeId") String fastaInputNodeId,
                             @WebParam(name = "par_file") String par_file,
                             @WebParam(name = "GC_cutoff") String GC_cutoff,
                             @WebParam(name = "position_1") String position_1,
                             @WebParam(name = "position_2") String position_2,
                             @WebParam(name = "condensed") String condensed,
                             @WebParam(name = "exon_table") String exon_table,
                             @WebParam(name = "exon_bonus") String exon_bonus,
                             @WebParam(name = "pmrna") String pmrna,
                             @WebParam(name = "pexons") String pexons,
                             @WebParam(name = "min_thr") String min_thr,
                             @WebParam(name = "scp_prom") String scp_prom,
                             @WebParam(name = "scp_term") String scp_term,
                             @WebParam(name = "min_f_exon") String min_f_exon,
                             @WebParam(name = "min_i_exon") String min_i_exon,
                             @WebParam(name = "min_t_exon") String min_t_exon,
                             @WebParam(name = "min_s_exon") String min_s_exon,
                             @WebParam(name = "nvar") String nvar,
                             @WebParam(name = "try_best_exons") String try_best_exons,
                             @WebParam(name = "try_best_sites") String try_best_sites,
                             @WebParam(name = "not_rem") String not_rem,
                             @WebParam(name = "vthr") String vthr,
                             @WebParam(name = "use_table") String use_table,
                             @WebParam(name = "show_table") String show_table) {
        logger.debug("Web Services - runFgenesh() acknowledged");
        FgeneshTask fgeneshTask = new FgeneshTask();
        fgeneshTask.setOwner(username);
        fgeneshTask.setParameter(FgeneshTask.PARAM_project, project);
        if (null != workSessionId && !"".equals(workSessionId)) {
            fgeneshTask.setParentTaskId(Long.valueOf(workSessionId));
        }
        fgeneshTask.setJobName(jobName);
        fgeneshTask.setParameter(FgeneshTask.PARAM_fasta_input_node_id, fastaInputNodeId);
        parameterHelper(fgeneshTask, FgeneshTask.PARAM_par_file, par_file);
        parameterHelper(fgeneshTask, FgeneshTask.PARAM_GC_cutoff, GC_cutoff);
        parameterHelper(fgeneshTask, FgeneshTask.PARAM_position_1, position_1);
        parameterHelper(fgeneshTask, FgeneshTask.PARAM_position_2, position_2);
        parameterHelper(fgeneshTask, FgeneshTask.PARAM_condensed, condensed);
        parameterHelper(fgeneshTask, FgeneshTask.PARAM_exon_table, exon_table);
        parameterHelper(fgeneshTask, FgeneshTask.PARAM_exon_bonus, exon_bonus);
        parameterHelper(fgeneshTask, FgeneshTask.PARAM_pmrna, pmrna);
        parameterHelper(fgeneshTask, FgeneshTask.PARAM_pexons, pexons);
        parameterHelper(fgeneshTask, FgeneshTask.PARAM_min_thr, min_thr);
        parameterHelper(fgeneshTask, FgeneshTask.PARAM_scp_prom, scp_prom);
        parameterHelper(fgeneshTask, FgeneshTask.PARAM_scp_term, scp_term);
        parameterHelper(fgeneshTask, FgeneshTask.PARAM_min_f_exon, min_f_exon);
        parameterHelper(fgeneshTask, FgeneshTask.PARAM_min_i_exon, min_i_exon);
        parameterHelper(fgeneshTask, FgeneshTask.PARAM_min_t_exon, min_t_exon);
        parameterHelper(fgeneshTask, FgeneshTask.PARAM_min_s_exon, min_s_exon);
        parameterHelper(fgeneshTask, FgeneshTask.PARAM_nvar, nvar);
        parameterHelper(fgeneshTask, FgeneshTask.PARAM_try_best_exons, try_best_exons);
        parameterHelper(fgeneshTask, FgeneshTask.PARAM_try_best_sites, try_best_sites);
        parameterHelper(fgeneshTask, FgeneshTask.PARAM_not_rem, not_rem);
        parameterHelper(fgeneshTask, FgeneshTask.PARAM_vthr, vthr);
        parameterHelper(fgeneshTask, FgeneshTask.PARAM_use_table, use_table);
        parameterHelper(fgeneshTask, FgeneshTask.PARAM_show_table, show_table);

        logger.info("Web Services - runFgenesh() config complete - now submitting task");
        return saveAndSubmitJob(fgeneshTask, "Fgenesh");
    }

    public String runInterProScan(@WebParam(name = "username") String username,
                                  @WebParam(name = "token") String token,
                                  @WebParam(name = "project") String project,
                                  @WebParam(name = "workSessionId") String workSessionId,
                                  @WebParam(name = "jobName") String jobName,
                                  @WebParam(name = "fastaInputNodeId") String fastaInputNodeId,
                                  @WebParam(name = "goterms") String goterms,
                                  @WebParam(name = "iprlookup") String iprlookup,
                                  @WebParam(name = "format") String format) {
        logger.debug("Web Services - runInterProScan() acknowledged");
        InterProScanTask interproscanTask = new InterProScanTask();
        interproscanTask.setOwner(username);
        interproscanTask.setParameter(InterProScanTask.PARAM_project, project);
        if (null != workSessionId && !"".equals(workSessionId)) {
            interproscanTask.setParentTaskId(Long.valueOf(workSessionId));
        }
        interproscanTask.setJobName(jobName);
        interproscanTask.setParameter(InterProScanTask.PARAM_fasta_input_node_id, fastaInputNodeId);

        logger.info("Web Services - runInterProScan() config complete - now submitting task");
        return saveAndSubmitJob(interproscanTask, "InterProScan");
    }

    public String runPrositeScan(@WebParam(name = "username") String username,
                                 @WebParam(name = "token") String token,
                                 @WebParam(name = "project") String project,
                                 @WebParam(name = "workSessionId") String workSessionId,
                                 @WebParam(name = "jobName") String jobName,
                                 @WebParam(name = "fastaInputNodeId") String fastaInputNodeId,
                                 @WebParam(name = "specificEntry") String specificEntry,
                                 @WebParam(name = "outputFormat") String outputFormat,
                                 @WebParam(name = "prositeDatabaseFile") String prositeDatabaseFile,
                                 @WebParam(name = "prositePattern") String prositePattern,
                                 @WebParam(name = "doNotScanProfiles") String doNotScanProfiles,
                                 @WebParam(name = "skipUnspecificProfiles") String skipUnspecificProfiles,
                                 @WebParam(name = "profileCutoffLevel") String profileCutoffLevel,
                                 @WebParam(name = "maximumXCount") String maximumXCount,
                                 @WebParam(name = "noGreediness") String noGreediness,
                                 @WebParam(name = "noOverlaps") String noOverlaps,
                                 @WebParam(name = "allowIncludedMatches") String allowIncludedMatches,
                                 @WebParam(name = "pfsearch") String pfsearch,
                                 @WebParam(name = "useRawScores") String useRawScores,
                                 @WebParam(name = "cutoffValue") String cutoffValue) {
        logger.debug("Web Services - runPrositeScan() acknowledged");
        PrositeScanTask prositeScanTask = new PrositeScanTask();
        prositeScanTask.setOwner(username);
        prositeScanTask.setParameter(PrositeScanTask.PARAM_project, project);
        if (null != workSessionId && !"".equals(workSessionId)) {
            prositeScanTask.setParentTaskId(Long.valueOf(workSessionId));
        }
        prositeScanTask.setJobName(jobName);
        prositeScanTask.setParameter(PrositeScanTask.PARAM_fasta_input_node_id, fastaInputNodeId);
        parameterHelper(prositeScanTask, PrositeScanTask.PARAM_specific_entry, specificEntry);
        parameterHelper(prositeScanTask, PrositeScanTask.PARAM_output_format, outputFormat);
        parameterHelper(prositeScanTask, PrositeScanTask.PARAM_prosite_database_file, prositeDatabaseFile);
        parameterHelper(prositeScanTask, PrositeScanTask.PARAM_prosite_pattern, prositePattern);
        parameterHelper(prositeScanTask, PrositeScanTask.PARAM_do_not_scan_profiles, doNotScanProfiles);
        parameterHelper(prositeScanTask, PrositeScanTask.PARAM_skip_unspecific_profiles, skipUnspecificProfiles);
        parameterHelper(prositeScanTask, PrositeScanTask.PARAM_profile_cutoff_level, profileCutoffLevel);
        parameterHelper(prositeScanTask, PrositeScanTask.PARAM_maximum_x_count, maximumXCount);
        parameterHelper(prositeScanTask, PrositeScanTask.PARAM_no_greediness, noGreediness);
        parameterHelper(prositeScanTask, PrositeScanTask.PARAM_no_overlaps, noOverlaps);
        parameterHelper(prositeScanTask, PrositeScanTask.PARAM_allow_included_matches, allowIncludedMatches);
        parameterHelper(prositeScanTask, PrositeScanTask.PARAM_pfsearch, pfsearch);
        parameterHelper(prositeScanTask, PrositeScanTask.PARAM_use_raw_scores, useRawScores);
        parameterHelper(prositeScanTask, PrositeScanTask.PARAM_cutoff_value, cutoffValue);

        logger.info("Web Services - runPrositeScan() config complete - now submitting task");
        return saveAndSubmitJob(prositeScanTask, "PrositeScan");
    }

    public String runTargetp(@WebParam(name = "username") String username,
                             @WebParam(name = "token") String token,
                             @WebParam(name = "project") String project,
                             @WebParam(name = "workSessionId") String workSessionId,
                             @WebParam(name = "jobName") String jobName,
                             @WebParam(name = "fastaInputNodeId") String fastaInputNodeId,
                             @WebParam(name = "networkType") String networkType,
                             @WebParam(name = "includeCleavage") String includeCleavage,
                             @WebParam(name = "chloroplastCutoff") String chloroplastCutoff,
                             @WebParam(name = "secretoryCutoff") String secretoryCutoff,
                             @WebParam(name = "mitochondrialCutoff") String mitochondrialCutoff,
                             @WebParam(name = "otherCutoff") String otherCutoff) {
        logger.debug("Web Services - runTargetp() acknowledged");
        TargetpTask targetpTask = new TargetpTask();
        targetpTask.setOwner(username);
        targetpTask.setParameter(TargetpTask.PARAM_project, project);
        if (null != workSessionId && !"".equals(workSessionId)) {
            targetpTask.setParentTaskId(Long.valueOf(workSessionId));
        }
        targetpTask.setJobName(jobName);
        targetpTask.setParameter(TargetpTask.PARAM_fasta_input_node_id, fastaInputNodeId);
        parameterHelper(targetpTask, TargetpTask.PARAM_network_type, networkType);
        parameterHelper(targetpTask, TargetpTask.PARAM_include_cleavage, includeCleavage);
        parameterHelper(targetpTask, TargetpTask.PARAM_chloroplast_cutoff, chloroplastCutoff);
        parameterHelper(targetpTask, TargetpTask.PARAM_secretory_cutoff, secretoryCutoff);
        parameterHelper(targetpTask, TargetpTask.PARAM_mitochondrial_cutoff, mitochondrialCutoff);
        parameterHelper(targetpTask, TargetpTask.PARAM_other_cutoff, otherCutoff);

        logger.info("Web Services - runTargetp() config complete - now submitting task");
        return saveAndSubmitJob(targetpTask, "Targetp");
    }

    public String runTmhmm(@WebParam(name = "username") String username,
                           @WebParam(name = "token") String token,
                           @WebParam(name = "project") String project,
                           @WebParam(name = "workSessionId") String workSessionId,
                           @WebParam(name = "jobName") String jobName,
                           @WebParam(name = "fastaInputNodeId") String fastaInputNodeId,
                           @WebParam(name = "html") String html,
                           @WebParam(name = "short") String tmhmmShort, // so named as to avoid a reserved word
                           @WebParam(name = "plot") String plot,
                           @WebParam(name = "v1") String v1) {
        logger.debug("Web Services - runTmhmm() acknowledged");
        TmhmmTask targetpTask = new TmhmmTask();
        targetpTask.setOwner(username);
        targetpTask.setParameter(TmhmmTask.PARAM_project, project);
        if (null != workSessionId && !"".equals(workSessionId)) {
            targetpTask.setParentTaskId(Long.valueOf(workSessionId));
        }
        targetpTask.setJobName(jobName);
        targetpTask.setParameter(TmhmmTask.PARAM_fasta_input_node_id, fastaInputNodeId);
        targetpTask.setParameter(TmhmmTask.PARAM_html, html);
        targetpTask.setParameter(TmhmmTask.PARAM_short, tmhmmShort);
        targetpTask.setParameter(TmhmmTask.PARAM_plot, plot);
        targetpTask.setParameter(TmhmmTask.PARAM_v1, v1);

        logger.info("Web Services - runTmhmm() config complete - now submitting task");
        return saveAndSubmitJob(targetpTask, "Tmhmm");
    }

    public String getBlastDatabaseLocations(@WebParam(name = "username") String username,
                                            @WebParam(name = "token") String token) {
        logger.debug("Web Services - getBlastDatabaseLocations() acknowledged");
        StringBuffer sbuf = new StringBuffer("");
        try {
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            //List<BlastDatabaseFileNode> databases = computeBean.getBlastDatabases();
            List<BlastDatabaseFileNode> databases = computeBean.getBlastDatabasesOfAUser(username);
            for (BlastDatabaseFileNode database : databases) {
                sbuf.append(database.getObjectId()).append("\t").
                        append(database.getName()).append("\t").
                        append(database.getDirectoryPath()).append("\n");
            }
        }
        catch (RemoteException e) {
            String error = "There was a problem getting the blast database info.\n";
            logger.error(error, e);
            sbuf = new StringBuffer(error);
        }
        logger.debug("Web Services - getBlastDatabaseLocations() complete");
        return sbuf.toString();
    }

    public String getBlastStatus(@WebParam(name = "username") String username,
                                 @WebParam(name = "token") String token,
                                 @WebParam(name = "taskId") String taskId) {
        logger.debug("Web Services - getBlastStatus() acknowledged");
        StringBuffer sbuf = new StringBuffer("");
        try {
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            boolean eventComplete = manageTaskStatusData(username, taskId, sbuf);

            // The web service must also pass back where the output is located
            if (eventComplete) {
                BlastResultFileNode resultNode = (BlastResultFileNode) computeBean.getResultNodeByTaskId(Long.parseLong(taskId));
                sbuf.append("Blast Result Location: ");
                if (null != resultNode) {
                    sbuf.append(resultNode.getFilePathByTag(BlastResultFileNode.TAG_ZIP)).append("\n");
                }
                else {
                    sbuf.append("Unable to locate blast output file.\n");
                }
            }
        }
        catch (Exception e) {
            String error = "There was a problem getting the blast status info.\n";
            logger.error(error, e);
            sbuf = new StringBuffer(error);
        }
        logger.debug("Web Services - getBlastStatus() complete");
        return sbuf.toString();
    }

    public String getBlastDatabaseStatus(@WebParam(name = "username") String username,
                                         @WebParam(name = "token") String token,
                                         @WebParam(name = "taskId") String taskId) {
        logger.debug("Web Services - getBlastDatabaseStatus() acknowledged");
        StringBuffer sbuf = new StringBuffer("");
        try {
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            boolean eventComplete = manageTaskStatusData(username, taskId, sbuf);

            // The web service must also pass back where the output is located
            if (eventComplete) {
                BlastDatabaseFileNode resultNode = (BlastDatabaseFileNode) computeBean.getResultNodeByTaskId(Long.parseLong(taskId));
                Task task = computeBean.getTaskForNodeId(resultNode.getObjectId());
                sbuf.append("Blast Database Name: ").append(resultNode.getName()).append("\n");
                sbuf.append("Blast Database Id: ").append(resultNode.getObjectId()).append("\n");
                sbuf.append("Blast Database Location: ").append(resultNode.getDirectoryPath()).append("\n");
                sbuf.append("Fasta File Node Id: ").append(task.getParameter(CreateBlastDatabaseTask.PARAM_FASTA_NODE_ID)).append("\n");
            }
        }
        catch (Exception e) {
            String error = "There was a problem getting the blast status info.\n";
            logger.error(error, e);
            sbuf = new StringBuffer(error);
        }
        logger.debug("Web Services - getBlastDatabaseStatus() complete");
        return sbuf.toString();
    }

    public String getTaskStatus(@WebParam(name = "username") String username,
                                @WebParam(name = "token") String token,
                                @WebParam(name = "taskId") String taskId) {
        logger.debug("Web Services - getTaskStatus() acknowledged");
        StringBuffer sbuf = new StringBuffer("");
        try {
            boolean eventComplete = manageTaskStatusData(username, taskId, sbuf);
            if (eventComplete) {
                ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
                FileNode returnNode = (FileNode) computeBean.getResultNodeByTaskId(Long.parseLong(taskId));
                if ( returnNode != null ) {
                    sbuf.append("\nResult(s) location: ").append(returnNode.getDirectoryPath());
                }
            }
        }
        catch (Exception e) {
            String error = "There was a problem getting the job status info.\n";
            logger.error(error, e);
            sbuf = new StringBuffer(error);
        }
        logger.debug("Web Services - getTaskStatus() complete");
        return sbuf.toString();
    }

    public String getTaskResultNode(@WebParam(name = "token") String token,
                                @WebParam(name = "taskId") String taskId) {
        logger.debug("Web Services - getTaskResultNode() acknowledged");
        StringBuffer sbuf = new StringBuffer("");
        try {

                ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
                FileNode returnNode = (FileNode) computeBean.getResultNodeByTaskId(Long.parseLong(taskId));
                if ( returnNode != null ) {
                    sbuf.append("\nResult(s) location: ").append(returnNode.getDirectoryPath());
                } else {
                    sbuf.append("\nNo Result location found for ").append(taskId);
                }

        }
        catch (Exception e) {
            String error = "There was a problem getting the job status info.\n";
            logger.error(error, e);
            sbuf = new StringBuffer(error);
        }
        logger.debug("Web Services - getTaskResultNode() complete");
        return sbuf.toString();
    }


    private boolean manageTaskStatusData(String username, String taskId, StringBuffer sbuf) throws DaoException, RemoteException {
        ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
        String[] returnString = computeBean.getTaskStatus(Long.parseLong(taskId));
        sbuf.append("\nStatus Type: ").append(returnString[0]);
        sbuf.append("\nStatus Description: ").append(returnString[1]).append("\n");
        return Event.COMPLETED_EVENT.equalsIgnoreCase(returnString[0]);
    }

    public String uploadFastaFileToSystem(@WebParam(name = "username") String username,
                                          @WebParam(name = "token") String token,
                                          @WebParam(name = "workSessionId") String workSessionId,
                                          @WebParam(name = "pathToFastaFile") String pathToFastaFile) {
        logger.debug("Web Services - uploadFastaFileToSystem() acknowledged");
        ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
        StringBuffer sbuf = new StringBuffer("");
        UploadFileTask fileTask = new UploadFileTask(null, username, null, null, pathToFastaFile);
        try {
            if (null != workSessionId && !"".equals(workSessionId)) {
                fileTask.setParentTaskId(Long.valueOf(workSessionId));
            }
            fileTask = (UploadFileTask) computeBean.saveOrUpdateTask(fileTask);
            computeBean.submitJob("UploadFile", fileTask.getObjectId());
            FastaFileNode fastaFileNode = (FastaFileNode) computeBean.getResultNodeByTaskId(fileTask.getObjectId());
            if (null != fastaFileNode) {
                sbuf.append("File id: ").append(fastaFileNode.getObjectId()).append("\n");
            }
            else {
                sbuf.append("There was a problem uploading the fasta file.\n");
            }
        }
        catch (Exception e) {
            String error = "There was a problem uploading the fasta file.\n" + e.getMessage() + "\n";
            sbuf = new StringBuffer(error);
            logTaskError(computeBean, e, fileTask.getObjectId(), error);
        }
        logger.debug("Web Services - uploadFastaFileToSystem() complete");
        return sbuf.toString();
    }

    public String uploadRnaSeqReferenceGenomeToSystem(@WebParam(name = "username") String username,
                                                      @WebParam(name = "token") String token,
                                                      @WebParam(name = "workSessionId") String workSessionId,
                                                      @WebParam(name = "pathToFastaFile") String pathToFastaFile) {
        logger.debug("Web Services - uploadRnaSeqReferenceGenomeToSystem() acknowledged");
        ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
        StringBuffer sbuf = new StringBuffer("");
        UploadRnaSeqReferenceGenomeTask fileTask = new UploadRnaSeqReferenceGenomeTask(null, username, null, null, pathToFastaFile);
        try {
            if (null != workSessionId && !"".equals(workSessionId)) {
                fileTask.setParentTaskId(Long.valueOf(workSessionId));
            }
            fileTask = (UploadRnaSeqReferenceGenomeTask) computeBean.saveOrUpdateTask(fileTask);
            computeBean.submitJob("UploadRnaSeqReferenceGenome", fileTask.getObjectId());
            RnaSeqReferenceGenomeNode refGenomeFileNode = (RnaSeqReferenceGenomeNode) computeBean.getResultNodeByTaskId(fileTask.getObjectId());
            if (null != refGenomeFileNode) {
                sbuf.append("File id: ").append(refGenomeFileNode.getObjectId()).append("\n");
            }
            else {
                sbuf.append("There was a problem uploading the fasta reference genome file.\n");
            }
        }
        catch (Exception e) {
            String error = "There was a problem uploading the fasta reference genome file.\n" + e.getMessage() + "\n";
            sbuf = new StringBuffer(error);
            logTaskError(computeBean, e, fileTask.getObjectId(), error);
        }
        logger.debug("Web Services - uploadRnaSeqReferenceGenomeToSystem() complete");
        return sbuf.toString();
    }

    public String uploadFastqDirectoryToSystem(@WebParam(name = "username") String username,
                                               @WebParam(name = "token") String token,
                                               @WebParam(name = "workSessionId") String workSessionId,
                                               @WebParam(name = "mateMeanInnerDistance") String mateMeanInnerDistance,
                                               @WebParam(name = "pathToFastqDirectory") String pathToFastqDirectory) {

        logger.debug("Web Services - uploadFastqDirectoryToSystem() acknowledged");
        ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
        UploadFastqDirectoryTask uploadTask = new UploadFastqDirectoryTask(null, username, null, null,
                new Long(mateMeanInnerDistance.trim()), pathToFastqDirectory, "UploadFastqDirectoryTask from WS");
        try {
            if (null != workSessionId && !"".equals(workSessionId)) {
                uploadTask.setParentTaskId(Long.valueOf(workSessionId));
            }
            uploadTask = (UploadFastqDirectoryTask) computeBean.saveOrUpdateTask(uploadTask);
        }
        catch (Exception e) {
            String error = "There was a problem uploading the fastq directory.\n" + e.getMessage() + "\n";
            logTaskError(computeBean, e, uploadTask.getObjectId(), error);
            return error;
        }
        logger.debug("Web Services - uploadFastqDirectoryToSystem() complete");
        return saveAndSubmitJobWithoutValidation(uploadTask, "UploadFastqDirectory");
    }

    public String getFastqUploadStatus(@WebParam(name = "username") String username,
                                       @WebParam(name = "token") String token,
                                       @WebParam(name = "taskId") String taskId) {
        logger.debug("Web Services - getFastqUploadStatus() acknowledged");
        StringBuffer sbuf = new StringBuffer("");
        try {
            boolean eventComplete = manageTaskStatusData(username, taskId, sbuf);
            if (eventComplete) {
                ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
                FileNode returnNode = (FileNode) computeBean.getResultNodeByTaskId(Long.parseLong(taskId));
                sbuf.append("\nFastq directory node id=").append(returnNode.getObjectId());
                sbuf.append("\nResult(s) location: ").append(returnNode.getDirectoryPath()).append("\n");
            }
            else {
                ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
                String[] returnString = computeBean.getTaskStatus(Long.parseLong(taskId));
                if (Event.ERROR_EVENT.equalsIgnoreCase(returnString[0])) {
                    sbuf.append("There was a problem uploading the fastq directory.\n");
                    sbuf.append("Make sure the filenames in the source directory have either format:\n");
                    sbuf.append("    <prefix>_<lane-number>.<extension> (unpaired case)\n");
                    sbuf.append("    <prefix>_<lane-number>_<direction-1-or-2>.<extension> (paired case)\n");
                }
            }
        }
        catch (Exception e) {
            String error = "There was a problem getting the job status info.\n";
            logger.error(error, e);
            sbuf = new StringBuffer(error);
        }
        logger.debug("Web Services - getFastqUploadStatus() complete");
        return sbuf.toString();
    }

    public String uploadRnaSeqGenomeReferenceToSystem(@WebParam(name = "username") String username,
                                                      @WebParam(name = "token") String token,
                                                      @WebParam(name = "workSessionId") String workSessionId,
                                                      @WebParam(name = "pathToGenomeReferenceFile") String pathToGenomeReferenceFile) {
        logger.info("Web Services - uploadRnaSeqGenomeReferenceToSystem() acknowledged with path=" + pathToGenomeReferenceFile);
        ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
        UploadRnaSeqReferenceGenomeTask uploadTask = new UploadRnaSeqReferenceGenomeTask(null, username, null, null, pathToGenomeReferenceFile);
        try {
            if (null != workSessionId && !"".equals(workSessionId)) {
                uploadTask.setParentTaskId(Long.valueOf(workSessionId));
            }
            uploadTask = (UploadRnaSeqReferenceGenomeTask) computeBean.saveOrUpdateTask(uploadTask);
        }
        catch (Exception e) {
            String error = "There was a problem uploading the genome reference file.\n" + e.getMessage() + "\n";
            logger.error(error);
            logTaskError(computeBean, e, uploadTask.getObjectId(), error);
            return error;
        }
        logger.info("Web Services - uploadRnaSeqGenomeReferenceToSystem() complete");
        return saveAndSubmitJobWithoutValidation(uploadTask, "UploadGenomeReferenceFile");
    }

    public String getRnaSeqGenomeReferenceUploadStatus(@WebParam(name = "username") String username,
                                                       @WebParam(name = "token") String token,
                                                       @WebParam(name = "taskId") String taskId) {
        logger.info("Web Services - getRnaSeqGenomeReferenceUploadStatus() acknowledged");
        StringBuffer sbuf = new StringBuffer("");
        try {
            boolean eventComplete = manageTaskStatusData(username, taskId, sbuf);
            if (eventComplete) {
                ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
                FileNode returnNode = (FileNode) computeBean.getResultNodeByTaskId(Long.parseLong(taskId));
                sbuf.append("\nRnaSeq Genome Reference File node id=").append(returnNode.getObjectId());
                sbuf.append("\nResult(s) location: ").append(returnNode.getFilePathByTag(RnaSeqReferenceGenomeNode.TAG_FASTA)).append("\n");
            }
            else {
                ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
                String[] returnString = computeBean.getTaskStatus(Long.parseLong(taskId));
                if (Event.ERROR_EVENT.equalsIgnoreCase(returnString[0])) {
                    sbuf.append("There was a problem uploading the RnaSeq Genome Reference file.\n");
                }
            }
        }
        catch (Exception e) {
            String error = "There was a problem getting the job status info.\n";
            logger.error(error, e);
            sbuf = new StringBuffer(error);
        }
        logger.info("Web Services - getRnaSeqGenomeReferenceUploadStatus() complete");
        return sbuf.toString();
    }

    public String uploadSamFileToSystem(@WebParam(name = "username") String username,
                                        @WebParam(name = "token") String token,
                                        @WebParam(name = "workSessionId") String workSessionId,
                                        @WebParam(name = "pathToSamFile") String pathToSamFile) {
        logger.info("Web Services - uploadSamFileToSystem() acknowledged with path=" + pathToSamFile);
        ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
        UploadSamFileTask uploadTask = new UploadSamFileTask(null, username, null, null, pathToSamFile);
        try {
            if (null != workSessionId && !"".equals(workSessionId)) {
                uploadTask.setParentTaskId(Long.valueOf(workSessionId));
            }
            uploadTask = (UploadSamFileTask) computeBean.saveOrUpdateTask(uploadTask);
        }
        catch (Exception e) {
            String error = "There was a problem uploading the sam file.\n" + e.getMessage() + "\n";
            logger.error(error);
            logTaskError(computeBean, e, uploadTask.getObjectId(), error);
            return error;
        }
        logger.info("Web Services - uploadSamFileToSystem() complete");
        return saveAndSubmitJobWithoutValidation(uploadTask, "UploadSamFile");
    }

    public String getSamUploadStatus(@WebParam(name = "username") String username,
                                     @WebParam(name = "token") String token,
                                     @WebParam(name = "taskId") String taskId) {
        logger.info("Web Services - getSamUploadStatus() acknowledged");
        StringBuffer sbuf = new StringBuffer("");
        try {
            boolean eventComplete = manageTaskStatusData(username, taskId, sbuf);
            if (eventComplete) {
                ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
                FileNode returnNode = (FileNode) computeBean.getResultNodeByTaskId(Long.parseLong(taskId));
                sbuf.append("\nSAM File node id=").append(returnNode.getObjectId());
                sbuf.append("\nResult(s) location: ").append(returnNode.getFilePathByTag(SamFileNode.TAG_SAM)).append("\n");
            }
            else {
                ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
                String[] returnString = computeBean.getTaskStatus(Long.parseLong(taskId));
                if (Event.ERROR_EVENT.equalsIgnoreCase(returnString[0])) {
                    sbuf.append("There was a problem uploading the SAM file.\n");
                }
            }
        }
        catch (Exception e) {
            String error = "There was a problem getting the job status info.\n";
            logger.error(error, e);
            sbuf = new StringBuffer(error);
        }
        logger.info("Web Services - getSamUploadStatus() complete");
        return sbuf.toString();
    }

    public String uploadGtfFileToSystem(@WebParam(name = "username") String username,
                                        @WebParam(name = "token") String token,
                                        @WebParam(name = "workSessionId") String workSessionId,
                                        @WebParam(name = "pathToGtfFile") String pathToGtfFile) {
        logger.info("Web Services - uploadGtfFileToSystem() acknowledged");
        ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
        UploadGtfFileTask uploadTask = new UploadGtfFileTask(null, username, null, null, pathToGtfFile);
        try {
            if (null != workSessionId && !"".equals(workSessionId)) {
                uploadTask.setParentTaskId(Long.valueOf(workSessionId));
            }
            uploadTask = (UploadGtfFileTask) computeBean.saveOrUpdateTask(uploadTask);
        }
        catch (Exception e) {
            String error = "There was a problem uploading the gtf file.\n" + e.getMessage() + "\n";
            logTaskError(computeBean, e, uploadTask.getObjectId(), error);
            return error;
        }
        logger.info("Web Services - UploadGtfFileTask() complete");
        return saveAndSubmitJobWithoutValidation(uploadTask, "UploadGtfFile");
    }

    public String getGtfUploadStatus(@WebParam(name = "username") String username,
                                     @WebParam(name = "token") String token,
                                     @WebParam(name = "taskId") String taskId) {
        logger.info("Web Services - getGtfUploadStatus() acknowledged");
        StringBuffer sbuf = new StringBuffer("");
        try {
            boolean eventComplete = manageTaskStatusData(username, taskId, sbuf);
            if (eventComplete) {
                ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
                FileNode returnNode = (FileNode) computeBean.getResultNodeByTaskId(Long.parseLong(taskId));
                sbuf.append("\nGtf File node id=").append(returnNode.getObjectId());
                sbuf.append("\nResult(s) location: ").append(returnNode.getFilePathByTag(GtfFileNode.TAG_GTF)).append("\n");
            }
            else {
                ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
                String[] returnString = computeBean.getTaskStatus(Long.parseLong(taskId));
                if (Event.ERROR_EVENT.equalsIgnoreCase(returnString[0])) {
                    sbuf.append("There was a problem uploading the GTF file.\n");
                }
            }
        }
        catch (Exception e) {
            String error = "There was a problem getting the job status info.\n";
            logger.error(error, e);
            sbuf = new StringBuffer(error);
        }
        logger.info("Web Services - getGtfUploadStatus() complete");
        return sbuf.toString();
    }

    public String uploadGenezillaIsoFileToSystem(@WebParam(name = "username") String username,
                                                 @WebParam(name = "token") String token,
                                                 @WebParam(name = "workSessionId") String workSessionId,
                                                 @WebParam(name = "pathToGenezillaIsoFile") String pathToGenezillaIsoFile) {
        logger.info("Web Services - uploadGenezillaIsoFileToSystem() acknowledged");
        ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
        UploadGenezillaIsoFileTask uploadTask = new UploadGenezillaIsoFileTask(null, username, null, null, pathToGenezillaIsoFile);
        try {
            if (null != workSessionId && !"".equals(workSessionId)) {
                uploadTask.setParentTaskId(Long.valueOf(workSessionId));
            }
            uploadTask = (UploadGenezillaIsoFileTask) computeBean.saveOrUpdateTask(uploadTask);
        }
        catch (Exception e) {
            String error = "There was a problem uploading the iso file.\n" + e.getMessage() + "\n";
            logTaskError(computeBean, e, uploadTask.getObjectId(), error);
            return error;
        }
        logger.info("Web Services - UploadGenezillaIsoFileTask() complete");
        return saveAndSubmitJobWithoutValidation(uploadTask, "UploadGenezillaIsoFile");
    }

    public String getGenezillaIsoUploadStatus(@WebParam(name = "username") String username,
                                              @WebParam(name = "token") String token,
                                              @WebParam(name = "taskId") String taskId) {
        logger.info("Web Services - getGenezillaIsoUploadStatus() acknowledged");
        StringBuffer sbuf = new StringBuffer("");
        try {
            boolean eventComplete = manageTaskStatusData(username, taskId, sbuf);
            if (eventComplete) {
                ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
                FileNode returnNode = (FileNode) computeBean.getResultNodeByTaskId(Long.parseLong(taskId));
                sbuf.append("\nGenezilla Iso File node id=").append(returnNode.getObjectId());
                sbuf.append("\nResult(s) location: ").append(returnNode.getFilePathByTag(GenezillaIsoFileNode.TAG_ISO)).append("\n");
            }
            else {
                ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
                String[] returnString = computeBean.getTaskStatus(Long.parseLong(taskId));
                if (Event.ERROR_EVENT.equalsIgnoreCase(returnString[0])) {
                    sbuf.append("There was a problem uploading the ISO file.\n");
                }
            }
        }
        catch (Exception e) {
            String error = "There was a problem getting the job status info.\n";
            logger.error(error, e);
            sbuf = new StringBuffer(error);
        }
        logger.info("Web Services - getGenezillaIsoUploadStatus() complete");
        return sbuf.toString();
    }

    public String uploadAndFormatBlastDataset(@WebParam(name = "username") String username,
                                              @WebParam(name = "token") String token,
                                              @WebParam(name = "workSessionId") String workSessionId,
                                              @WebParam(name = "blastDBName") String blastDBName,
                                              @WebParam(name = "blastDBDescription") String blastDBDescription,
                                              @WebParam(name = "pathToFastaFile") String pathToFastaFile) {
        logger.debug("Web Services - uploadAndFormatBlastDataset() acknowledged");
        ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
        StringBuffer sbuf = new StringBuffer("");
        CreateBlastDatabaseTask createDBTask = null;
        try {
            // Save the fasta file as a node and override the path to the system location.
            // This prevents copying the data and also allows us to work with node id's in the service, which helps
            // the way the web front-end works, too.
            File tmpFasta = new File(pathToFastaFile);
            FastaFileNode fastaNode = new FastaFileNode(username, null, blastDBName, blastDBDescription, Node.VISIBILITY_INACTIVE,
                    FastaUtil.determineSequenceType(tmpFasta),
                    0, null); // A super-large input file may take a very long time.  Calculate in the service.  The WS bean will timeout.
            fastaNode.setPathOverride(pathToFastaFile);
            fastaNode = (FastaFileNode) computeBean.saveOrUpdateNode(fastaNode);

            createDBTask = new CreateBlastDatabaseTask(null, username, null, null,
                    blastDBName, blastDBDescription, fastaNode.getObjectId().toString());
            if (null != workSessionId && !"".equals(workSessionId)) {
                createDBTask.setParentTaskId(Long.valueOf(workSessionId));
            }
            createDBTask = (CreateBlastDatabaseTask) computeBean.saveOrUpdateTask(createDBTask);
            computeBean.submitJob("CreateBlastDB", createDBTask.getObjectId());
            sbuf.append("Blast Database Name: ").append(blastDBName).append("\n");
            sbuf.append("Check status of job ").append(createDBTask.getObjectId()).append(" with service getBlastDatabaseStatus() to know when your database creation is complete.\n");
        }
        catch (Exception e) {
            String error = "There was a problem uploading the fasta file.\n" + e.getMessage() + "\n";
            sbuf = new StringBuffer(error);
            if (null != createDBTask && null != createDBTask.getObjectId()) {
                logTaskError(computeBean, e, createDBTask.getObjectId(), error);
            }
        }
        logger.debug("Web Services - uploadAndFormatBlastDataset() complete");
        return sbuf.toString();
    }

    public String runHmmpfam(@WebParam(name = "username") String username,
                             @WebParam(name = "token") String token,
                             @WebParam(name = "project") String project,
                             @WebParam(name = "workSessionId") String workSessionId,
                             @WebParam(name = "jobName") String jobName,
                             @WebParam(name = "subjectDBIdentifier") String subjectDBIdentifier,
                             @WebParam(name = "queryFastaFileNodeId") String queryFastaFileNodeId,
                             @WebParam(name = "maxBestDomainAligns") int maxBestDomainAligns,
                             @WebParam(name = "evalueCutoff") String evalueCutoff,
                             @WebParam(name = "tbitThreshold") String tbitThreshold,
                             @WebParam(name = "zModelNumber") int zModelNumber,
                             @WebParam(name = "useHmmAccessions") boolean useHmmAccessions,
                             @WebParam(name = "cutGa") boolean cutGa,
                             @WebParam(name = "cutNc") boolean cutNc,
                             @WebParam(name = "cutTc") boolean cutTc,
                             @WebParam(name = "domE") String domE,
                             @WebParam(name = "domT") String domT,
                             @WebParam(name = "null2") boolean null2) {
        logger.debug("Web Services - runHmmpfam() acknowledged");
        HmmpfamTask hmmpfamTask = new HmmpfamTask();
        hmmpfamTask.setOwner(username);
        hmmpfamTask.setParameter(HmmpfamTask.PARAM_project, project);
        if (null != workSessionId && !"".equals(workSessionId)) {
            hmmpfamTask.setParentTaskId(Long.valueOf(workSessionId));
        }
        hmmpfamTask.setJobName(jobName);
        hmmpfamTask.setParameter(HmmpfamTask.PARAM_pfam_db_node_id, subjectDBIdentifier);
        hmmpfamTask.setParameter(HmmpfamTask.PARAM_query_node_id, queryFastaFileNodeId);
        hmmpfamTask.setParameter(HmmpfamTask.PARAM_max_best_domain_aligns, Integer.toString(maxBestDomainAligns));
        hmmpfamTask.setParameter(HmmpfamTask.PARAM_evalue_cutoff, evalueCutoff);
        hmmpfamTask.setParameter(HmmpfamTask.PARAM_tbit_threshold, tbitThreshold);
        hmmpfamTask.setParameter(HmmpfamTask.PARAM_z_model_number, Integer.toString(zModelNumber));
        hmmpfamTask.setParameter(HmmpfamTask.PARAM_acc, Boolean.toString(useHmmAccessions));
        hmmpfamTask.setParameter(HmmpfamTask.PARAM_cut_ga, Boolean.toString(cutGa));
        hmmpfamTask.setParameter(HmmpfamTask.PARAM_cut_nc, Boolean.toString(cutNc));
        hmmpfamTask.setParameter(HmmpfamTask.PARAM_cut_tc, Boolean.toString(cutTc));
        hmmpfamTask.setParameter(HmmpfamTask.PARAM_domE, domE);
        hmmpfamTask.setParameter(HmmpfamTask.PARAM_domT, domT);
        logger.debug("Web Services - runHmmpfam() complete");
        return saveAndSubmitJob(hmmpfamTask, "HmmPfam");
    }

    public String runHMM3(@WebParam(name = "username") String username,
                          @WebParam(name = "token") String token,
                          @WebParam(name = "project") String project,
                          @WebParam(name = "workSessionId") String workSessionId,
                          @WebParam(name = "jobName") String jobName,
                          @WebParam(name = "subjectDBIdentifier") String subjectDBIdentifier,
                          @WebParam(name = "queryFastaFileNodeId") String queryFastaFileNodeId,
                          @WebParam(name = "evalueCutoff") String evalueCutoff,
                          @WebParam(name = "tbitThreshold") String tbitThreshold,
                          @WebParam(name = "inclusionEvalueCutoff") String incEvalueCutoff,
                          @WebParam(name = "inclusionTbitThreshold") String incTbitThreshold,
                          @WebParam(name = "useHmmAccessions") String useHmmAccessions,
                          @WebParam(name = "cutGa") String cutGa,
                          @WebParam(name = "cutNc") String cutNc,
                          @WebParam(name = "cutTc") String cutTc,
                          @WebParam(name = "domE") String domE,
                          @WebParam(name = "domT") String domT,
                          @WebParam(name = "inclusiondomE") String incdomE,
                          @WebParam(name = "inclusiondomT") String incdomT,
                          @WebParam(name = "skipAlignmentOutput") String skipAlignmentOutput,
                          @WebParam(name = "unlimitTextWidth") String unlimitTextWidth,
                          @WebParam(name = "maxTextWidth") String maxTextWidth,
                          @WebParam(name = "disableHeuristicFilters") String disableHeuristicFilters,
                          @WebParam(name = "disableBiasFilters") String disableBiasFilters,
                          @WebParam(name = "disableScoreCorrections") String disableScoreCorrections,
                          @WebParam(name = "msvThreshold") String msvThreshold,
                          @WebParam(name = "vitThresholdExponent") String vitThresholdExponent,
                          @WebParam(name = "fwdThresholdExponent") String fwdThresholdExponent
    ) {
        logger.debug("Web Services - runHMM3() acknowledged");
        HMMER3Task hmmer3Task = new HMMER3Task();
        hmmer3Task.setOwner(username);
        hmmer3Task.setParameter(HmmpfamTask.PARAM_project, project);
        if (null != workSessionId && !"".equals(workSessionId)) {
            hmmer3Task.setParentTaskId(Long.valueOf(workSessionId));
        }
        hmmer3Task.setJobName(jobName);
        if (hasValue(queryFastaFileNodeId)) {
            hmmer3Task.setParameter(HMMER3Task.PARAM_query_node_id, queryFastaFileNodeId);
        }
        if (hasValue(subjectDBIdentifier)) {
            hmmer3Task.setParameter(HMMER3Task.PARAM_db_node_id, subjectDBIdentifier);
        }
        if (hasValue(evalueCutoff)) {
            hmmer3Task.setParameter(HMMER3Task.PARAM_evalue_cutoff, evalueCutoff);
        }
        if (hasValue(tbitThreshold)) {
            hmmer3Task.setParameter(HMMER3Task.PARAM_tbit_threshold, tbitThreshold);
        }
        if (hasValue(incEvalueCutoff)) {
            hmmer3Task.setParameter(HMMER3Task.PARAM_inc_evalue_cutoff, incEvalueCutoff);
        }
        if (hasValue(incTbitThreshold)) {
            hmmer3Task.setParameter(HMMER3Task.PARAM_inc_tbit_threshold, incTbitThreshold);
        }
        if (hasValue(useHmmAccessions)) {
            hmmer3Task.setParameter(HMMER3Task.PARAM_acc, useHmmAccessions);
        }
        if (hasValue(cutGa)) {
            hmmer3Task.setParameter(HMMER3Task.PARAM_cut_ga, cutGa);
        }
        if (hasValue(cutNc)) {
            hmmer3Task.setParameter(HMMER3Task.PARAM_cut_nc, cutNc);
        }
        if (hasValue(cutTc)) {
            hmmer3Task.setParameter(HMMER3Task.PARAM_cut_tc, cutTc);
        }
        if (hasValue(domE)) {
            hmmer3Task.setParameter(HMMER3Task.PARAM_domE, domE);
        }
        if (hasValue(domT)) {
            hmmer3Task.setParameter(HMMER3Task.PARAM_domT, domT);
        }
        if (hasValue(incdomE)) {
            hmmer3Task.setParameter(HMMER3Task.PARAM_inc_domE, incdomE);
        }
        if (hasValue(incdomT)) {
            hmmer3Task.setParameter(HMMER3Task.PARAM_inc_domT, incdomT);
        }
        if (hasValue(skipAlignmentOutput)) {
            hmmer3Task.setParameter(HMMER3Task.PARAM_skip_alignment_output, skipAlignmentOutput);
        }
        if (hasValue(unlimitTextWidth)) {
            hmmer3Task.setParameter(HMMER3Task.PARAM_unlimit_text_width, unlimitTextWidth);
        }
        if (hasValue(maxTextWidth)) {
            hmmer3Task.setParameter(HMMER3Task.PARAM_max_ascii_output_width, maxTextWidth);
        }
        if (hasValue(disableHeuristicFilters)) {
            hmmer3Task.setParameter(HMMER3Task.PARAM_disable_heuristic_filters, disableHeuristicFilters);
        }
        if (hasValue(disableBiasFilters)) {
            hmmer3Task.setParameter(HMMER3Task.PARAM_disable_bias_filter, disableBiasFilters);
        }
        if (hasValue(disableScoreCorrections)) {
            hmmer3Task.setParameter(HMMER3Task.PARAM_disable_score_corrections, disableScoreCorrections);
        }
        if (hasValue(msvThreshold)) {
            hmmer3Task.setParameter(HMMER3Task.PARAM_msv_threshold, msvThreshold);
        }
        if (hasValue(vitThresholdExponent)) {
            hmmer3Task.setParameter(HMMER3Task.PARAM_vit_threshold, vitThresholdExponent);
        }
        if (hasValue(fwdThresholdExponent)) {
            hmmer3Task.setParameter(HMMER3Task.PARAM_fwd_threshold, fwdThresholdExponent);
        }
        logger.debug("Web Services - runHMM3() call complete");
        return saveAndSubmitJob(hmmer3Task, "HMMER3");
    }

    private boolean hasValue(String testValue) {
        return (null != testValue && !"".equals(testValue) && !"null".equalsIgnoreCase(testValue));
    }

    public String runTrnaScan(@WebParam(name = "username") String username,
                              @WebParam(name = "token") String token,
                              @WebParam(name = "project") String project,
                              @WebParam(name = "workSessionId") String workSessionId,
                              @WebParam(name = "jobName") String jobName,
                              @WebParam(name = "inputFastaFileNodeId") String inputFastaFileNodeId,
                              @WebParam(name = "searchType") String searchType,
                              @WebParam(name = "options") String options) {
        logger.debug("Web Services - runTrnaScan() acknowledged");
        TrnaScanTask trnaScanTask = new TrnaScanTask();
        trnaScanTask.setOwner(username);
        trnaScanTask.setParameter(TrnaScanTask.PARAM_project, project);
        if (null != workSessionId && !"".equals(workSessionId)) {
            trnaScanTask.setParentTaskId(Long.valueOf(workSessionId));
        }
        trnaScanTask.setJobName(jobName);
        trnaScanTask.setParameter(TrnaScanTask.PARAM_input_fasta_node_id, inputFastaFileNodeId);
        if (searchType != null && searchType.trim().length() > 0)
            trnaScanTask.setParameter(TrnaScanTask.PARAM_search_type, searchType);
        if (options != null && options.trim().length() > 0)
            trnaScanTask.setParameter(TrnaScanTask.PARAM_options, options);
        logger.debug("Web Services - runTrnaScan() complete");
        return saveAndSubmitJob(trnaScanTask, "TrnaScan");
    }

    public String runRrnaScan(@WebParam(name = "username") String username,
                              @WebParam(name = "token") String token,
                              @WebParam(name = "project") String project,
                              @WebParam(name = "workSessionId") String workSessionId,
                              @WebParam(name = "jobName") String jobName,
                              @WebParam(name = "inputFastaFileNodeId") String inputFastaFileNodeId,
                              @WebParam(name = "initial_blast_options") String initialBlastOptions,
                              @WebParam(name = "second_blast_options") String secondBlastOptions,
                              @WebParam(name = "min_5S_length") String min5Slength,
                              @WebParam(name = "min_16S_length") String min16Slength,
                              @WebParam(name = "min_18S_length") String min18Slength,
                              @WebParam(name = "min_23S_length") String min23Slength) {
        logger.debug("Web Services - runRrnaScan() acknowledged");
        RrnaScanTask rrnaScanTask = new RrnaScanTask();
        rrnaScanTask.setOwner(username);
        rrnaScanTask.setParameter(RrnaScanTask.PARAM_project, project);
        if (null != workSessionId && !"".equals(workSessionId)) {
            rrnaScanTask.setParentTaskId(Long.valueOf(workSessionId));
        }
        rrnaScanTask.setJobName(jobName);
        rrnaScanTask.setParameter(RrnaScanTask.PARAM_input_fasta_node_id, inputFastaFileNodeId);
        if (initialBlastOptions != null && initialBlastOptions.trim().length() > 0)
            rrnaScanTask.setParameter(RrnaScanTask.PARAM_initial_blast_options, initialBlastOptions);
        if (secondBlastOptions != null && secondBlastOptions.trim().length() > 0)
            rrnaScanTask.setParameter(RrnaScanTask.PARAM_second_blast_options, secondBlastOptions);
        if (min5Slength != null && min5Slength.trim().length() > 0)
            rrnaScanTask.setParameter(RrnaScanTask.PARAM_min_5S_length, min5Slength);
        if (min16Slength != null && min16Slength.trim().length() > 0)
            rrnaScanTask.setParameter(RrnaScanTask.PARAM_min_16S_length, min16Slength);
        if (min18Slength != null && min18Slength.trim().length() > 0)
            rrnaScanTask.setParameter(RrnaScanTask.PARAM_min_18S_length, min18Slength);
        if (min23Slength != null && min23Slength.trim().length() > 0)
            rrnaScanTask.setParameter(RrnaScanTask.PARAM_min_23S_length, min23Slength);
        logger.debug("Web Services - runRrnaScan() complete");
        return saveAndSubmitJob(rrnaScanTask, "RrnaScan");
    }

    public String runPriam(@WebParam(name = "username") String username,
                           @WebParam(name = "token") String token,
                           @WebParam(name = "project") String project,
                           @WebParam(name = "workSessionId") String workSessionId,
                           @WebParam(name = "jobName") String jobName,
                           @WebParam(name = "inputFastaFileNodeId") String inputFastaFileNodeId,
                           @WebParam(name = "rpsblast_options") String rpsBlastOptions,
                           @WebParam(name = "max_evalue") String maxEvalue) {
        logger.debug("Web Services - runPriam() acknowledged");
        PriamTask priamTask = new PriamTask();
        priamTask.setOwner(username);
        priamTask.setParameter(PriamTask.PARAM_project, project);
        if (null != workSessionId && !"".equals(workSessionId)) {
            priamTask.setParentTaskId(Long.valueOf(workSessionId));
        }
        priamTask.setJobName(jobName);
        priamTask.setParameter(RrnaScanTask.PARAM_input_fasta_node_id, inputFastaFileNodeId);
        if (rpsBlastOptions != null && rpsBlastOptions.trim().length() > 0)
            priamTask.setParameter(PriamTask.PARAM_rpsblast_options, rpsBlastOptions);
        if (maxEvalue != null && maxEvalue.trim().length() > 0)
            priamTask.setParameter(PriamTask.PARAM_max_eval, maxEvalue);
        logger.debug("Web Services - runPriam() complete");
        return saveAndSubmitJob(priamTask, "Priam");
    }

    public String runTeragridSimpleBlast(@WebParam(name = "username") String username,
                                         @WebParam(name = "token") String token,
                                         @WebParam(name = "project") String project,
                                         @WebParam(name = "workSessionId") String workSessionId,
                                         @WebParam(name = "jobName") String jobName,
                                         @WebParam(name = "inputFastaFileNodeId") String inputFastaFileNodeId,
                                         @WebParam(name = "teragridGrantNumber") String teragridGrantNumber,
                                         @WebParam(name = "mpiBlastProgram") String mpiBlastProgram,
                                         @WebParam(name = "teragridBlastDbName") String teragridBlastDbName,
                                         @WebParam(name = "teragridBlastDbSize") String teragridBlastDbSize,
                                         @WebParam(name = "sqliteMapDbPath") String sqliteMapDbPath,
                                         @WebParam(name = "mpiBlastParameters") String mpiBlastParameters) {
        logger.debug("Web Services - runTeragridSimpleBlast() acknowledged");
        TeragridSimpleBlastTask tgTask = new TeragridSimpleBlastTask();
        tgTask.setOwner(username);
        tgTask.setParameter(TeragridSimpleBlastTask.PARAM_project, project);
        if (null != workSessionId && !"".equals(workSessionId)) {
            tgTask.setParentTaskId(Long.valueOf(workSessionId));
        }
        tgTask.setJobName(jobName);
        tgTask.setParameter(TeragridSimpleBlastTask.PARAM_query, inputFastaFileNodeId);
        if (teragridGrantNumber != null && teragridGrantNumber.length() > 0)
            tgTask.setParameter(TeragridSimpleBlastTask.PARAM_teragrid_grant_number, teragridGrantNumber);
        if (mpiBlastProgram != null && mpiBlastProgram.length() > 0)
            tgTask.setParameter(TeragridSimpleBlastTask.PARAM_mpi_blast_program, mpiBlastProgram);
        if (teragridBlastDbName != null && teragridBlastDbName.length() > 0)
            tgTask.setParameter(TeragridSimpleBlastTask.PARAM_tg_db_name, teragridBlastDbName);
        if (teragridBlastDbSize != null && teragridBlastDbSize.length() > 0)
            tgTask.setParameter(TeragridSimpleBlastTask.PARAM_tg_db_size, teragridBlastDbSize);
        if (sqliteMapDbPath != null && sqliteMapDbPath.length() > 0)
            tgTask.setParameter(TeragridSimpleBlastTask.PARAM_path_to_sqlite_map_db, sqliteMapDbPath);
        if (mpiBlastParameters != null && mpiBlastParameters.length() > 0)
            tgTask.setParameter(TeragridSimpleBlastTask.PARAM_mpi_blast_parameters, mpiBlastParameters);
        logger.debug("Web Services - runTeragridSimpleBlast() complete");
        return saveAndSubmitJob(tgTask, "TeragridSimpleBlast");
    }

    public String runSimpleOrfCaller(@WebParam(name = "username") String username,
                                     @WebParam(name = "token") String token,
                                     @WebParam(name = "project") String project,
                                     @WebParam(name = "workSessionId") String workSessionId,
                                     @WebParam(name = "jobName") String jobName,
                                     @WebParam(name = "inputFastaFileNodeId") String inputFastaFileNodeId,
                                     @WebParam(name = "translation_table") String translationTable,
                                     @WebParam(name = "beginning_as_start") String beginningAsStart,
                                     @WebParam(name = "end_as_stop") String endAsStop,
                                     @WebParam(name = "assume_stops") String assumeStops,
                                     @WebParam(name = "full_orfs") String fullOrfs,
                                     @WebParam(name = "min_orf_size") String minOrfSize,
                                     @WebParam(name = "max_orf_size") String maxOrfSize,
                                     @WebParam(name = "min_unmasked_size") String minUnmaskedSize,
                                     @WebParam(name = "frames") String frames,
                                     @WebParam(name = "force_methionine") String forceMethionine,
                                     @WebParam(name = "header_additions") String headerAdditions) {
        logger.debug("Web Services - runSimpleOrfCaller() acknowledged");
        SimpleOrfCallerTask simpleOrfCallerTask = new SimpleOrfCallerTask();
        simpleOrfCallerTask.setOwner(username);
        simpleOrfCallerTask.setParameter(SimpleOrfCallerTask.PARAM_project, project);
        if (null != workSessionId && !"".equals(workSessionId)) {
            simpleOrfCallerTask.setParentTaskId(Long.valueOf(workSessionId));
        }
        simpleOrfCallerTask.setJobName(jobName);
        simpleOrfCallerTask.setParameter(SimpleOrfCallerTask.PARAM_input_fasta_node_id, inputFastaFileNodeId);

        if (translationTable != null && translationTable.trim().length() > 0)
            simpleOrfCallerTask.setParameter(SimpleOrfCallerTask.PARAM_translation_table, translationTable);
        if (beginningAsStart != null && beginningAsStart.trim().length() > 0)
            simpleOrfCallerTask.setParameter(SimpleOrfCallerTask.PARAM_beginning_as_start, beginningAsStart);
        if (endAsStop != null && endAsStop.trim().length() > 0)
            simpleOrfCallerTask.setParameter(SimpleOrfCallerTask.PARAM_end_as_stop, endAsStop);
        if (assumeStops != null && assumeStops.trim().length() > 0)
            simpleOrfCallerTask.setParameter(SimpleOrfCallerTask.PARAM_assume_stops, assumeStops);
        if (fullOrfs != null && fullOrfs.trim().length() > 0)
            simpleOrfCallerTask.setParameter(SimpleOrfCallerTask.PARAM_full_orfs, fullOrfs);
        if (minOrfSize != null && minOrfSize.trim().length() > 0)
            simpleOrfCallerTask.setParameter(SimpleOrfCallerTask.PARAM_min_orf_size, minOrfSize);
        if (maxOrfSize != null && maxOrfSize.trim().length() > 0)
            simpleOrfCallerTask.setParameter(SimpleOrfCallerTask.PARAM_max_orf_size, maxOrfSize);
        if (minUnmaskedSize != null && minUnmaskedSize.trim().length() > 0)
            simpleOrfCallerTask.setParameter(SimpleOrfCallerTask.PARAM_min_unmasked_size, minUnmaskedSize);
        if (frames != null && frames.trim().length() > 0)
            simpleOrfCallerTask.setParameter(SimpleOrfCallerTask.PARAM_frames, frames);
        if (forceMethionine != null && forceMethionine.trim().length() > 0)
            simpleOrfCallerTask.setParameter(SimpleOrfCallerTask.PARAM_force_methionine, forceMethionine);
        if (headerAdditions != null && headerAdditions.trim().length() > 0)
            simpleOrfCallerTask.setParameter(SimpleOrfCallerTask.PARAM_header_additions, headerAdditions);
        logger.debug("Web Services - runSimpleOrfCaller() complete");
        return saveAndSubmitJob(simpleOrfCallerTask, "SimpleOrfCaller");
    }

    public String runMetaGenoOrfCaller(@WebParam(name = "username") String username,
                                       @WebParam(name = "token") String token,
                                       @WebParam(name = "project") String project,
                                       @WebParam(name = "workSessionId") String workSessionId,
                                       @WebParam(name = "jobName") String jobName,
                                       @WebParam(name = "inputFastaFileNodeId") String inputFastaFileNodeId,
                                       @WebParam(name = "useClearRange") String useClearRange,
                                       @WebParam(name = "translationTable") String translationTable,
                                       @WebParam(name = "beginningAsStart") String beginningAsStart,
                                       @WebParam(name = "endAsStop") String endAsStop,
                                       @WebParam(name = "aassumeStops") String assumeStops,
                                       @WebParam(name = "fullOrfs") String fullOrfs,
                                       @WebParam(name = "minOrfSize") String minOrfSize,
                                       @WebParam(name = "maxOrfSize") String maxOrfSize,
                                       @WebParam(name = "minUnmaskedSize") String minUnmaskedSize,
                                       @WebParam(name = "frames") String frames,
                                       @WebParam(name = "forceMethionine") String forceMethionine,
                                       @WebParam(name = "clearRangeMinOrfSize") String clearRangeMinOrfSize) {

        logger.debug("Web Services - runMetaGenoOrfCaller() acknowledged");
        MetaGenoOrfCallerTask metaGenoOrfCallerTask = new MetaGenoOrfCallerTask();
        metaGenoOrfCallerTask.setOwner(username);
        metaGenoOrfCallerTask.setParameter(MetaGenoOrfCallerTask.PARAM_project, project);
        if (null != workSessionId && !"".equals(workSessionId)) {
            metaGenoOrfCallerTask.setParentTaskId(Long.valueOf(workSessionId));
        }
        metaGenoOrfCallerTask.setJobName(jobName);
        metaGenoOrfCallerTask.setParameter(MetaGenoOrfCallerTask.PARAM_input_node_id, inputFastaFileNodeId);

        parameterHelper(metaGenoOrfCallerTask, MetaGenoOrfCallerTask.PARAM_useClearRange, useClearRange);
        parameterHelper(metaGenoOrfCallerTask, MetaGenoOrfCallerTask.PARAM_translationTable, translationTable);
        parameterHelper(metaGenoOrfCallerTask, MetaGenoOrfCallerTask.PARAM_beginningAsStart, beginningAsStart);
        parameterHelper(metaGenoOrfCallerTask, MetaGenoOrfCallerTask.PARAM_endAsStop, endAsStop);
        parameterHelper(metaGenoOrfCallerTask, MetaGenoOrfCallerTask.PARAM_assumeStops, assumeStops);
        parameterHelper(metaGenoOrfCallerTask, MetaGenoOrfCallerTask.PARAM_fullOrfs, fullOrfs);
        parameterHelper(metaGenoOrfCallerTask, MetaGenoOrfCallerTask.PARAM_minOrfSize, minOrfSize);
        parameterHelper(metaGenoOrfCallerTask, MetaGenoOrfCallerTask.PARAM_maxOrfSize, maxOrfSize);
        parameterHelper(metaGenoOrfCallerTask, MetaGenoOrfCallerTask.PARAM_minUnmaskedSize, minUnmaskedSize);
        parameterHelper(metaGenoOrfCallerTask, MetaGenoOrfCallerTask.PARAM_frames, frames);
        parameterHelper(metaGenoOrfCallerTask, MetaGenoOrfCallerTask.PARAM_forceMethionine, forceMethionine);
        parameterHelper(metaGenoOrfCallerTask, MetaGenoOrfCallerTask.PARAM_clearRangeMinOrfSize, clearRangeMinOrfSize);

        logger.debug("Web Services - runMetaGenoOrfCaller() complete");
        return saveAndSubmitJob(metaGenoOrfCallerTask, "MetaGenoORFCaller");
    }

    public String runMetaGenoCombinedOrfAnno(@WebParam(name = "username") String username,
                                             @WebParam(name = "token") String token,
                                             @WebParam(name = "project") String project,
                                             @WebParam(name = "workSessionId") String workSessionId,
                                             @WebParam(name = "jobName") String jobName,
                                             @WebParam(name = "inputFastaFileNodeId") String inputFastaFileNodeId,
                                             @WebParam(name = "useClearRange") String useClearRange) {
        logger.debug("Web Services - runMetaGenoCombinedOrfAnno() acknowledged");
        MetaGenoCombinedOrfAnnoTask metaGenoCombinedOrfAnnoTask = new MetaGenoCombinedOrfAnnoTask();
        metaGenoCombinedOrfAnnoTask.setOwner(username);
        metaGenoCombinedOrfAnnoTask.setParameter(MetaGenoCombinedOrfAnnoTask.PARAM_project, project);
        if (null != workSessionId && !"".equals(workSessionId)) {
            metaGenoCombinedOrfAnnoTask.setParentTaskId(Long.valueOf(workSessionId));
        }
        metaGenoCombinedOrfAnnoTask.setJobName(jobName);
        metaGenoCombinedOrfAnnoTask.setParameter(MetaGenoCombinedOrfAnnoTask.PARAM_input_node_id, inputFastaFileNodeId);

        if (useClearRange != null && useClearRange.trim().length() > 0)
            metaGenoCombinedOrfAnnoTask.setParameter(MetaGenoCombinedOrfAnnoTask.PARAM_useClearRange, useClearRange);

        logger.debug("Web Services - runMetaGenoCombinedOrfAnno() complete");
        return saveAndSubmitJob(metaGenoCombinedOrfAnnoTask, "MetaGenoCombinedOrfAnno");
    }

    public String runMetaGenoAnnotation(@WebParam(name = "username") String username,
                                        @WebParam(name = "token") String token,
                                        @WebParam(name = "project") String project,
                                        @WebParam(name = "workSessionId") String workSessionId,
                                        @WebParam(name = "jobName") String jobName,
                                        @WebParam(name = "inputFastaFileNodeId") String inputFastaFileNodeId) {
        logger.debug("Web Services - runMetaGenoAnnotation() acknowledged");
        MetaGenoAnnotationTask metaGenoAnnotationTask = new MetaGenoAnnotationTask();
        metaGenoAnnotationTask.setOwner(username);
        metaGenoAnnotationTask.setParameter(MetaGenoOrfCallerTask.PARAM_project, project);
        if (null != workSessionId && !"".equals(workSessionId)) {
            metaGenoAnnotationTask.setParentTaskId(Long.valueOf(workSessionId));
        }
        metaGenoAnnotationTask.setJobName(jobName);
        metaGenoAnnotationTask.setParameter(MetaGenoOrfCallerTask.PARAM_input_node_id, inputFastaFileNodeId);
        logger.debug("Web Services - runMetaGenoAnnotation() complete");
        return saveAndSubmitJob(metaGenoAnnotationTask, "MetaGenoAnnotation");
    }

    public String runGtfToPasaIntegration(@WebParam(name = "username") String username,
                                          @WebParam(name = "token") String token,
                                          @WebParam(name = "project") String project,
                                          @WebParam(name = "workSessionId") String workSessionId,
                                          @WebParam(name = "jobName") String jobName,
                                          @WebParam(name = "pasaDatabaseName") String pasaDatabaseName,
                                          @WebParam(name = "referenceGenomeFastaNodeId") String referenceGenomeFastaNodeId,
                                          @WebParam(name = "gtfNodeId") String gtfNodeId) {
        logger.debug("Web Services - runGtfToPasaIntegration() acknowledged");
        GtfToPasaIntegrationTask gpTask = new GtfToPasaIntegrationTask();
        gpTask.setOwner(username);
        gpTask.setParameter(GtfToPasaIntegrationTask.PARAM_project, project);
        if (null != workSessionId && !"".equals(workSessionId)) {
            gpTask.setParentTaskId(Long.valueOf(workSessionId));
        }
        gpTask.setParameter(GtfToPasaIntegrationTask.PARAM_pasa_database_name, pasaDatabaseName);
        gpTask.setParameter(GtfToPasaIntegrationTask.PARAM_refgenome_fasta_node_id, referenceGenomeFastaNodeId);
        gpTask.setParameter(GtfToPasaIntegrationTask.PARAM_gtf_node_id, gtfNodeId);
        logger.debug("Web Services - runGtfToPasaIntegration() complete");
        return saveAndSubmitJob(gpTask, "GtfToPasaIntegration");
    }

    public String runMetagene(@WebParam(name = "username") String username,
                              @WebParam(name = "token") String token,
                              @WebParam(name = "project") String project,
                              @WebParam(name = "workSessionId") String workSessionId,
                              @WebParam(name = "jobName") String jobName,
                              @WebParam(name = "inputFastaFileNodeId") String inputFastaFileNodeId) {
        logger.debug("Web Services - runMetagene() acknowledged");
        MetageneTask metageneTask = new MetageneTask();
        metageneTask.setOwner(username);
        metageneTask.setParameter(MetageneTask.PARAM_project, project);
        if (null != workSessionId && !"".equals(workSessionId)) {
            metageneTask.setParentTaskId(Long.valueOf(workSessionId));
        }
        metageneTask.setJobName(jobName);
        metageneTask.setParameter(MetageneTask.PARAM_input_fasta_node_id, inputFastaFileNodeId);
        logger.debug("Web Services - runMetagene() complete");
        return saveAndSubmitJob(metageneTask, "Metagene");
    }

    public String runRnaSeqPipeline(@WebParam(name = "username") String username,
                                    @WebParam(name = "token") String token,
                                    @WebParam(name = "project") String project,
                                    @WebParam(name = "workSessionId") String workSessionId,
                                    @WebParam(name = "jobName") String jobName,
                                    @WebParam(name = "pasaDatabaseName") String pasaDatabaseName,
                                    @WebParam(name = "referenceGenomeFastaNodeId") String referenceGenomeFastaNodeId,
                                    @WebParam(name = "readMapperName") String readMapperName,
                                    @WebParam(name = "transcriptAssemblerName") String transcriptAssemblerName,
                                    @WebParam(name = "inputReadsFastqNodeId") String inputReadsFastqNodeId,
                                    @WebParam(name = "innerMatePairMeanDistance") String innerMatePairMeanDistance,
                                    @WebParam(name = "innerMatePairStdDev") String innerMatePairStdDev,
                                    @WebParam(name = "maxIntronLength") String maxIntronLength,
                                    @WebParam(name = "minIsoformFraction") String minIsoformFraction,
                                    @WebParam(name = "preMrnaFraction") String preMrnaFraction,
                                    @WebParam(name = "minMapQual") String minMapQual) {
        logger.debug("Web Services - runRnaSeqPipeline() acknowledged");
        RnaSeqPipelineTask rnaSeqPipelineTask = new RnaSeqPipelineTask();
        rnaSeqPipelineTask.setOwner(username);
        rnaSeqPipelineTask.setParameter(CufflinksTask.PARAM_project, project);
        if (null != workSessionId && !"".equals(workSessionId)) {
            rnaSeqPipelineTask.setParentTaskId(Long.valueOf(workSessionId));
        }
        rnaSeqPipelineTask.setJobName(jobName);
        parameterHelper(rnaSeqPipelineTask, RnaSeqPipelineTask.PARAM_pasa_db_name, pasaDatabaseName);
        parameterHelper(rnaSeqPipelineTask, RnaSeqPipelineTask.PARAM_input_refgenome_fasta_node_id, referenceGenomeFastaNodeId);
        parameterHelper(rnaSeqPipelineTask, RnaSeqPipelineTask.PARAM_read_mapper, readMapperName);
        parameterHelper(rnaSeqPipelineTask, RnaSeqPipelineTask.PARAM_transcript_assembler, transcriptAssemblerName);
        parameterHelper(rnaSeqPipelineTask, RnaSeqPipelineTask.PARAM_input_reads_fastQ_node_id, inputReadsFastqNodeId);
        parameterHelper(rnaSeqPipelineTask, RnaSeqPipelineTask.PARAM_CUFFLINKS_inner_dist_mean, innerMatePairMeanDistance);
        parameterHelper(rnaSeqPipelineTask, RnaSeqPipelineTask.PARAM_CUFFLINKS_inner_dist_std_dev, innerMatePairStdDev);
        parameterHelper(rnaSeqPipelineTask, RnaSeqPipelineTask.PARAM_CUFFLINKS_max_intron_length, maxIntronLength);
        parameterHelper(rnaSeqPipelineTask, RnaSeqPipelineTask.PARAM_CUFFLINKS_min_isoform_fraction, minIsoformFraction);
        parameterHelper(rnaSeqPipelineTask, RnaSeqPipelineTask.PARAM_CUFFLINKS_pre_mrna_fraction, preMrnaFraction);
        parameterHelper(rnaSeqPipelineTask, RnaSeqPipelineTask.PARAM_CUFFLINKS_min_mapqual, minMapQual);
        logger.info("Web Services - runRnaSeqPipeline() config complete - now submitting task");
        return saveAndSubmitJob(rnaSeqPipelineTask, "RnaSeqPipeline");
    }

    public String getHmmerDatabaseLocations(@WebParam(name = "username") String username,
                                            @WebParam(name = "token") String token) {
        logger.debug("Web Services - getHmmerDatabaseLocations() acknowledged");
        StringBuffer sbuf = new StringBuffer("");
        try {
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            List<HmmerPfamDatabaseNode> databases = computeBean.getHmmerPfamDatabases();
            for (HmmerPfamDatabaseNode database : databases) {
                sbuf.append(database.getObjectId()).append("\t").
                        append(database.getName()).append("\t").
                        append(database.getDirectoryPath()).append("\n");
            }
        }
        catch (RemoteException e) {
            String error = "There was a problem getting the hmmerpfam database info.\n";
            logger.error(error, e);
            sbuf = new StringBuffer(error);
        }
        logger.debug("Web Services - getHmmerDatabaseLocations() complete");
        return sbuf.toString();
    }

    public String getHmmpfamStatus(@WebParam(name = "username") String username,
                                   @WebParam(name = "token") String token,
                                   @WebParam(name = "taskId") String taskId) {
        logger.debug("Web Services - getHmmpfamStatus() acknowledged");
        StringBuffer sbuf = new StringBuffer("");
        try {
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            boolean eventComplete = manageTaskStatusData(username, taskId, sbuf);

            // The web service must also pass back where the output is located
            if (eventComplete) {
                HmmerPfamResultNode resultNode = (HmmerPfamResultNode) computeBean.getResultNodeByTaskId(Long.parseLong(taskId));
                sbuf.append("Hmmpfam Result Location: ");
                if (null != resultNode) {
                    sbuf.append(resultNode.getFilePathByTag(HmmerPfamResultNode.TAG_TEXT_OUTPUT)).append("\n");
                }
                else {
                    sbuf.append("Unable to locate hmmpfam output file.\n");
                }
            }
        }
        catch (Exception e) {
            String error = "There was a problem getting the hmmpfam status info.\n";
            logger.error(error, e);
            sbuf = new StringBuffer(error);
        }
        logger.debug("Web Services - getHmmpfamStatus() complete");
        return sbuf.toString();
    }

    public String getTrnaScanStatus(@WebParam(name = "username") String username,
                                    @WebParam(name = "token") String token,
                                    @WebParam(name = "taskId") String taskId) {
        logger.debug("Web Services - getTrnaScanStatus() acknowledged");
        StringBuffer sbuf = new StringBuffer("");
        try {
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            boolean eventComplete = manageTaskStatusData(username, taskId, sbuf);

            // The web service must also pass back where the output is located
            if (eventComplete) {
                TrnaScanResultNode resultNode = (TrnaScanResultNode) computeBean.getResultNodeByTaskId(Long.parseLong(taskId));
                sbuf.append("TrnaScan Result Location: ");
                if (null != resultNode) {
                    sbuf.append(resultNode.getFilePathByTag(TrnaScanResultNode.TAG_RAW_OUTPUT)).append("\n");
                }
                else {
                    sbuf.append("Unable to locate TrnaScan output file.\n");
                }
            }
        }
        catch (Exception e) {
            String error = "There was a problem getting the TrnaScan status info.\n";
            logger.error(error, e);
            sbuf = new StringBuffer(error);
        }
        logger.debug("Web Services - getTrnaScanStatus() complete");
        return sbuf.toString();
    }

    public String getRrnaScanStatus(@WebParam(name = "username") String username,
                                    @WebParam(name = "token") String token,
                                    @WebParam(name = "taskId") String taskId) {
        logger.debug("Web Services - getRrnaScanStatus() acknowledged");
        StringBuffer sbuf = new StringBuffer("");
        try {
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            boolean eventComplete = manageTaskStatusData(username, taskId, sbuf);

            // The web service must also pass back where the output is located
            if (eventComplete) {
                RrnaScanResultNode resultNode = (RrnaScanResultNode) computeBean.getResultNodeByTaskId(Long.parseLong(taskId));
                sbuf.append("RrnaScan Result Location: ");
                if (null != resultNode) {
                    sbuf.append(resultNode.getFilePathByTag(RrnaScanResultNode.TAG_RRNA_FASTA_OUTPUT)).append("\n");
                    sbuf.append(resultNode.getFilePathByTag(RrnaScanResultNode.TAG_POSTMASK_FASTA_OUTPUT)).append("\n");
                }
                else {
                    sbuf.append("Unable to locate RrnaScan output file.\n");
                }
            }
        }
        catch (Exception e) {
            String error = "There was a problem getting the RrnaScan status info.\n";
            logger.error(error, e);
            sbuf = new StringBuffer(error);
        }
        logger.debug("Web Services - getRrnaScanStatus() complete");
        return sbuf.toString();
    }

    public String getPriamStatus(@WebParam(name = "username") String username,
                                 @WebParam(name = "token") String token,
                                 @WebParam(name = "taskId") String taskId) {
        logger.debug("Web Services - getPriamStatus() acknowledged");
        StringBuffer sbuf = new StringBuffer("");
        try {
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            boolean eventComplete = manageTaskStatusData(username, taskId, sbuf);

            // The web service must also pass back where the output is located
            if (eventComplete) {
                PriamResultNode resultNode = (PriamResultNode) computeBean.getResultNodeByTaskId(Long.parseLong(taskId));
                sbuf.append("Priam Result Location: ");
                if (null != resultNode) {
                    sbuf.append(resultNode.getFilePathByTag(PriamResultNode.TAG_PRIAM_EC_HIT_FILE)).append("\n");
                    sbuf.append(resultNode.getFilePathByTag(PriamResultNode.TAG_PRIAM_EC_HIT_TAB_FILE)).append("\n");
                    sbuf.append(resultNode.getFilePathByTag(PriamResultNode.TAG_PRIAM_EC_HIT_TAB_PARSED_FILE)).append("\n");
                }
                else {
                    sbuf.append("Unable to locate Priam output file.\n");
                }
            }
        }
        catch (Exception e) {
            String error = "There was a problem getting the Priam status info.\n";
            logger.error(error, e);
            sbuf = new StringBuffer(error);
        }
        logger.debug("Web Services - getPriamStatus() complete");
        return sbuf.toString();
    }

    public String getTeragridSimpleBlastStatus(@WebParam(name = "username") String username,
                                               @WebParam(name = "token") String token,
                                               @WebParam(name = "taskId") String taskId) {
        logger.debug("Web Services - getTeragridSimpleBlastStatus() acknowledged");
        StringBuffer sbuf = new StringBuffer("");
        try {
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            boolean eventComplete = manageTaskStatusData(username, taskId, sbuf);

            // The web service must also pass back where the output is located
            if (eventComplete) {
                BlastResultFileNode resultNode = (BlastResultFileNode) computeBean.getResultNodeByTaskId(Long.parseLong(taskId));
                sbuf.append("TeragridSimpleBlast Result Location: ");
                if (null != resultNode) {
                    sbuf.append(resultNode.getFilePathByTag(BlastResultFileNode.TAG_XML)).append("\n");
                    sbuf.append(resultNode.getFilePathByTag(BlastResultFileNode.TAG_BTAB)).append("\n");
                }
                else {
                    sbuf.append("Unable to locate TeragridSimpleBlast output file.\n");
                }
            }
        }
        catch (Exception e) {
            String error = "There was a problem getting the TeragridSimpleBlast status info.\n";
            logger.error(error, e);
            sbuf = new StringBuffer(error);
        }
        logger.debug("Web Services - getTeragridSimpleBlastStatus() complete");
        return sbuf.toString();
    }

    public String getTophatStatus(@WebParam(name = "username") String username,
                                  @WebParam(name = "token") String token,
                                  @WebParam(name = "taskId") String taskId) {
        logger.debug("Web Services - getTophatStatus() acknowledged");
        StringBuffer sbuf = new StringBuffer("");
        try {
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            boolean eventComplete = manageTaskStatusData(username, taskId, sbuf);

            // The web service must also pass back where the output is located
            if (eventComplete) {
                TophatResultNode resultNode = (TophatResultNode) computeBean.getResultNodeByTaskId(Long.parseLong(taskId));
                if (null != resultNode) {
                    sbuf.append("Tophat Result Location: ").append(resultNode.getDirectoryPath());
                }
                else {
                    sbuf.append("Unable to locate Tophat output file.\n");
                }
            }
        }
        catch (Exception e) {
            String error = "There was a problem getting the Tophat status info.\n";
            logger.error(error, e);
            sbuf = new StringBuffer(error);
        }
        logger.debug("Web Services - getTophatStatus() complete");
        return sbuf.toString();
    }

    public String getCufflinksStatus(@WebParam(name = "username") String username,
                                     @WebParam(name = "token") String token,
                                     @WebParam(name = "taskId") String taskId) {
        logger.debug("Web Services - getCufflinksStatus() acknowledged");
        StringBuffer sbuf = new StringBuffer("");
        try {
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            boolean eventComplete = manageTaskStatusData(username, taskId, sbuf);

            // The web service must also pass back where the output is located
            if (eventComplete) {
                CufflinksResultNode resultNode = (CufflinksResultNode) computeBean.getResultNodeByTaskId(Long.parseLong(taskId));
                if (null != resultNode) {
                    sbuf.append("Cufflinks Result Location: ").append(resultNode.getDirectoryPath());
                }
                else {
                    sbuf.append("Unable to locate Cufflinks output file.\n");
                }
            }
        }
        catch (Exception e) {
            String error = "There was a problem getting the Cufflinks status info.\n";
            logger.error(error, e);
            sbuf = new StringBuffer(error);
        }
        logger.debug("Web Services - getCufflinksStatus() complete");
        return sbuf.toString();
    }

    public String getSimpleOrfCallerStatus(@WebParam(name = "username") String username,
                                           @WebParam(name = "token") String token,
                                           @WebParam(name = "taskId") String taskId) {
        logger.debug("Web Services - getSimpleOrfCallerStatus() acknowledged");
        StringBuffer sbuf = new StringBuffer("");
        try {
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            boolean eventComplete = manageTaskStatusData(username, taskId, sbuf);

            // The web service must also pass back where the output is located
            if (eventComplete) {
                SimpleOrfCallerResultNode resultNode = (SimpleOrfCallerResultNode) computeBean.getResultNodeByTaskId(Long.parseLong(taskId));
                sbuf.append("SimpleOrfCaller Result Location: ");
                if (null != resultNode) {
                    sbuf.append(resultNode.getFilePathByTag(SimpleOrfCallerResultNode.TAG_NUCLEOTIDE_ORF_FASTA_OUTPUT)).append("\n");
                    sbuf.append(resultNode.getFilePathByTag(SimpleOrfCallerResultNode.TAG_PEPTIDE_ORF_FASTA_OUTPUT)).append("\n");
                }
                else {
                    sbuf.append("Unable to locate SimpleOrfCaller output file.\n");
                }
            }
        }
        catch (Exception e) {
            String error = "There was a problem getting the SimpleOrfCaller status info.\n";
            logger.error(error, e);
            sbuf = new StringBuffer(error);
        }
        logger.debug("Web Services - getSimpleOrfCallerStatus() complete");
        return sbuf.toString();
    }

    public String getMetaGenoOrfCallerStatus(@WebParam(name = "username") String username,
                                             @WebParam(name = "token") String token,
                                             @WebParam(name = "taskId") String taskId) {
        logger.debug("Web Services - getMetaGenoOrfCallerStatus() acknowledged");
        StringBuffer sbuf = new StringBuffer("");
        try {
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            boolean eventComplete = manageTaskStatusData(username, taskId, sbuf);

            // The web service must also pass back where the output is located
            if (eventComplete) {
                MetaGenoOrfCallerResultNode resultNode = (MetaGenoOrfCallerResultNode) computeBean.getResultNodeByTaskId(Long.parseLong(taskId));
                sbuf.append("MetaGenoOrfCaller Result Location: ");
                if (null != resultNode) {
                    sbuf.append(resultNode.getDirectoryPath());
                }
                else {
                    sbuf.append("Unable to locate MetaGenoOrfCaller output directory.\n");
                }
            }
        }
        catch (Exception e) {
            String error = "There was a problem getting the MetaGenoOrfCaller status info.\n";
            logger.error(error, e);
            sbuf = new StringBuffer(error);
        }
        logger.debug("Web Services - getMetaGenoOrfCallerStatus() complete");
        return sbuf.toString();
    }

    public String getMetaGenoAnnotationStatus(@WebParam(name = "username") String username,
                                              @WebParam(name = "token") String token,
                                              @WebParam(name = "taskId") String taskId) {
        logger.debug("Web Services - getMetaGenoAnnotationStatus() acknowledged");
        StringBuffer sbuf = new StringBuffer("");
        try {
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            boolean eventComplete = manageTaskStatusData(username, taskId, sbuf);

            // The web service must also pass back where the output is located
            if (eventComplete) {
                MetaGenoAnnotationResultNode resultNode = (MetaGenoAnnotationResultNode) computeBean.getResultNodeByTaskId(Long.parseLong(taskId));
                sbuf.append("MetaGenoAnnotation Result Location: ");
                if (null != resultNode) {
                    sbuf.append(resultNode.getDirectoryPath());
                }
                else {
                    sbuf.append("Unable to locate MetaGenoAnnotation output directory.\n");
                }
            }
        }
        catch (Exception e) {
            String error = "There was a problem getting the MetaGenoAnnotation status info.\n";
            logger.error(error, e);
            sbuf = new StringBuffer(error);
        }
        logger.debug("Web Services - getMetaGenoAnnotationStatus() complete");
        return sbuf.toString();
    }

    public String getMetaGenoCombinedOrfAnnoStatus(@WebParam(name = "username") String username,
                                                   @WebParam(name = "token") String token,
                                                   @WebParam(name = "taskId") String taskId) {
        logger.debug("Web Services - getMetaGenoCombinedOrfAnnoStatus() acknowledged");
        StringBuffer sbuf = new StringBuffer("");
        try {
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            boolean eventComplete = manageTaskStatusData(username, taskId, sbuf);

            // The web service must also pass back where the output is located
            if (eventComplete) {
                List<Task> childTaskList = computeBean.getChildTasksByParentTaskId(new Long(taskId.trim()));
                for (Task t : childTaskList) {
                    if (t instanceof MetaGenoOrfCallerTask) {
                        MetaGenoOrfCallerResultNode resultNode = (MetaGenoOrfCallerResultNode) computeBean.getResultNodeByTaskId(t.getObjectId());
                        sbuf.append("MetaGenoOrfCaller Result Location: ").append(resultNode.getDirectoryPath()).append("\n");
                    }
                    else if (t instanceof MetaGenoAnnotationTask) {
                        MetaGenoAnnotationResultNode resultNode = (MetaGenoAnnotationResultNode) computeBean.getResultNodeByTaskId(t.getObjectId());
                        sbuf.append("MetaGenoAnnotation Result Location: ").append(resultNode.getDirectoryPath()).append("\n");
                    }
                    else {
                        throw new Exception("For MetaGenoCombinedOrfAnnoTask, do not recognize child task type=" + t.getClass().getName());
                    }
                }
            }
        }
        catch (Exception e) {
            String error = "There was a problem getting the MetaGenoCombinedOrfAnno status info.\n";
            logger.error(error, e);
            sbuf = new StringBuffer(error);
        }
        logger.debug("Web Services - getMetaGenoCombinedOrfAnnoStatus() complete");
        return sbuf.toString();
    }

    public String getMetageneStatus(@WebParam(name = "username") String username,
                                    @WebParam(name = "token") String token,
                                    @WebParam(name = "taskId") String taskId) {
        logger.debug("Web Services - getMetageneStatus() acknowledged");
        StringBuffer sbuf = new StringBuffer("");
        try {
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            boolean eventComplete = manageTaskStatusData(username, taskId, sbuf);

            // The web service must also pass back where the output is located
            if (eventComplete) {
                MetageneResultNode resultNode = (MetageneResultNode) computeBean.getResultNodeByTaskId(Long.parseLong(taskId));
                sbuf.append("Metagene Result Location: ");
                if (null != resultNode) {
                    sbuf.append(resultNode.getFilePathByTag(MetageneResultNode.TAG_RAW_OUTPUT)).append("\n");
                    sbuf.append(resultNode.getFilePathByTag(MetageneResultNode.TAG_BTAB_OUTPUT)).append("\n");
                }
                else {
                    sbuf.append("Unable to locate Metagene output file.\n");
                }
            }
        }
        catch (Exception e) {
            String error = "There was a problem getting the Metagene status info.\n";
            logger.error(error, e);
            sbuf = new StringBuffer(error);
        }
        logger.debug("Web Services - getMetageneStatus() complete");
        return sbuf.toString();
    }

    public String runReversePsiBlast(@WebParam(name = "username") String username,
                                     @WebParam(name = "token") String token,
                                     @WebParam(name = "project") String project,
                                     @WebParam(name = "workSessionId") String workSessionId,
                                     @WebParam(name = "jobName") String jobName,
                                     @WebParam(name = "subjectDBIdentifier") String subjectDBIdentifier,
                                     @WebParam(name = "queryFastaFileNodeId") String queryFastaFileNodeId,
                                     @WebParam(name = "eValueExponent") int eValueExponent,
                                     @WebParam(name = "blastExtensionDropoffBits") int blastExtensionDropoffBits,
                                     @WebParam(name = "believeDefline") boolean believeDefline,
                                     @WebParam(name = "showGIsInDeflines") boolean showGIsInDeflines,
                                     @WebParam(name = "lowercaseFiltering") boolean lowercaseFiltering,
                                     @WebParam(name = "forceLegacyBlastEngine") boolean forceLegacyBlastEngine,
                                     @WebParam(name = "filterQueryWithSEG") boolean filterQueryWithSEG,
                                     @WebParam(name = "gappedAlignmentDropoff") int gappedAlignmentDropoff,
                                     @WebParam(name = "bitsToTriggerGapping") int bitsToTriggerGapping,
                                     @WebParam(name = "finalGappedAlignmentDropoff") int finalGappedAlignmentDropoff,
                                     @WebParam(name = "databaseAlignmentsPerQuery") int databaseAlignmentsPerQuery) {
        logger.debug("Web Services - runReversePsiBlast() acknowledged");
        ReversePsiBlastTask rpsblastTask = new ReversePsiBlastTask();
        rpsblastTask.setOwner(username);
        rpsblastTask.setJobName(jobName);
        rpsblastTask.setParameter(ReversePsiBlastTask.PARAM_query_node_id, queryFastaFileNodeId);
        MultiSelectVO ms = new MultiSelectVO();
        ArrayList<String> dbList = new ArrayList<String>();
        dbList.add(subjectDBIdentifier);
        ms.setPotentialChoices(dbList);
        ms.setActualUserChoices(dbList);
        rpsblastTask.setParameter(ReversePsiBlastTask.PARAM_project, project);
        if (null != workSessionId && !"".equals(workSessionId)) {
            rpsblastTask.setParentTaskId(Long.valueOf(workSessionId));
        }
        rpsblastTask.setParameter(ReversePsiBlastTask.PARAM_evalue, Integer.toString(eValueExponent));
        rpsblastTask.setParameter(ReversePsiBlastTask.PARAM_subjectDatabases, Task.csvStringFromCollection(ms.getActualUserChoices()));
        rpsblastTask.setParameter(ReversePsiBlastTask.PARAM_blastExtensionDropoff, Integer.toString(blastExtensionDropoffBits));
        rpsblastTask.setParameter(ReversePsiBlastTask.PARAM_believeQueryDefline, Boolean.toString(believeDefline));
        rpsblastTask.setParameter(ReversePsiBlastTask.PARAM_giInDeflines, Boolean.toString(showGIsInDeflines));
        rpsblastTask.setParameter(ReversePsiBlastTask.PARAM_useLowercaseFiltering, Boolean.toString(lowercaseFiltering));
        rpsblastTask.setParameter(ReversePsiBlastTask.PARAM_forceLegacyBlast, Boolean.toString(forceLegacyBlastEngine));
        rpsblastTask.setParameter(ReversePsiBlastTask.PARAM_segFilter, Boolean.toString(filterQueryWithSEG));
        rpsblastTask.setParameter(ReversePsiBlastTask.PARAM_gappedAlignmentDropoff, Integer.toString(gappedAlignmentDropoff));
        rpsblastTask.setParameter(ReversePsiBlastTask.PARAM_gappingBitTrigger, Integer.toString(bitsToTriggerGapping));
        rpsblastTask.setParameter(ReversePsiBlastTask.PARAM_finalGappedAlignmentDropoff, Integer.toString(finalGappedAlignmentDropoff));
        rpsblastTask.setParameter(ReversePsiBlastTask.PARAM_databaseAlignments, Integer.toString(databaseAlignmentsPerQuery));
        logger.debug("Web Services - runReversePsiBlast() complete");
        return saveAndSubmitJob(rpsblastTask, "ReversePsiBlast");
    }

    public String getReversePsiBlastDatabaseLocations(@WebParam(name = "username") String username,
                                                      @WebParam(name = "token") String token) {
        logger.debug("Web Services - getReversePsiBlastDatabaseLocations() acknowledged");
        StringBuffer sbuf = new StringBuffer("");
        try {
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            List<ReversePsiBlastDatabaseNode> databases = computeBean.getReversePsiBlastDatabases();
            for (ReversePsiBlastDatabaseNode database : databases) {
                sbuf.append(database.getObjectId()).append("\t").
                        append(database.getName()).append("\t").
                        append(database.getDirectoryPath()).append("\n");
            }
        }
        catch (RemoteException e) {
            String error = "There was a problem getting the Reverse PSI-BLAST database info.\n";
            logger.error(error, e);
            sbuf = new StringBuffer(error);
        }
        logger.debug("Web Services - getReversePsiBlastDatabaseLocations() complete");
        return sbuf.toString();
    }

    public String getSystemDatabaseIdByName(@WebParam(name = "databaseName") String databaseName) {
        logger.debug("Web Services - getSystemDatabaseIdByName() acknowledged");
        StringBuffer sbuf = new StringBuffer("");
        try {
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            Long databaseId = computeBean.getSystemDatabaseIdByName(databaseName);
            if (databaseId == null) {
                throw new Exception("computeBean.getSystemDatabaseIdByName() returned null for databaseName=" + databaseName);
            }
            sbuf.append(databaseId.toString()).append("\n");
        }
        catch (Exception e) {
            String error = "There was a problem getting the system database ID for databaseName=" + databaseName;
            logger.error(error, e);
            sbuf = new StringBuffer(error);
        }
        logger.debug("Web Services - getSystemDatabaseIdByName() complete");
        return sbuf.toString();
    }

    public String deleteTaskById(@WebParam(name = "username") String username,
                                 @WebParam(name = "token") String token,
                                 @WebParam(name = "taskId") String taskId) {
        logger.debug("Web Services - deleteTaskById() acknowledged");
        StringBuffer sbuf = new StringBuffer("");
        try {
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            Task task = computeBean.getTaskById(new Long(taskId.trim()));
            if (!task.getOwner().equals(username)) {
                throw new Exception("Given user=" + username + " does not match actual task user=" + task.getOwner());
            }
            computeBean.deleteTaskById(new Long(taskId.trim()));
            sbuf.append("Task=").append(taskId).append(" and all subtasks successfully deleted\n");
        }
        catch (Exception e) {
            String error = "There was a problem deleting task=" + taskId + " : " + e.getMessage();
            logger.error(error, e);
            sbuf = new StringBuffer(error + "\n");
        }
        logger.debug("Web Services - deleteTaskById() complete");
        return sbuf.toString();
    }

    public String persistMgAnnoSqlByNodeId(@WebParam(name = "username") String username,
                                           @WebParam(name = "token") String token,
                                           @WebParam(name = "project") String project,
                                           @WebParam(name = "workSessionId") String workSessionId,
                                           @WebParam(name = "resultNodeId") String resultNodeId) {
        logger.debug("Web Services - persistMgAnnoSqlByNodeId() acknowledged");
        try {
            MetaGenoPersistAnnoSqlTask persistTask = new MetaGenoPersistAnnoSqlTask();
            persistTask.setOwner(username);
            persistTask.setParameter(MetaGenoPersistAnnoSqlTask.PARAM_project, project);
            persistTask.setParameter(MetaGenoPersistAnnoSqlTask.PARAM_input_anno_result_node_id, resultNodeId);
            if (null != workSessionId && !"".equals(workSessionId)) {
                persistTask.setParentTaskId(Long.valueOf(workSessionId));
            }
            return saveAndSubmitJob(persistTask, "MetaGenoPersistAnnoSql");
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return "Error: " + ex.getMessage();
        }
    }

    public String persistMgOrfSqlByNodeId(@WebParam(name = "username") String username,
                                          @WebParam(name = "token") String token,
                                          @WebParam(name = "project") String project,
                                          @WebParam(name = "workSessionId") String workSessionId,
                                          @WebParam(name = "resultNodeId") String resultNodeId) {
        return "Not yet implemented";
    }


    public String run16sAnalysis(@WebParam(name = "username") String username,
                                 @WebParam(name = "token") String token,
                                 @WebParam(name = "project") String project,
                                 @WebParam(name = "jobName") String jobName,
                                 @WebParam(name = "pathToInputFile") String pathToInputFile,
                                 @WebParam(name = "pathToQualFile") String pathToQualFile,
                                 @WebParam(name = "referenceDataset") String referenceDataset,
                                 @WebParam(name = "ampliconSize") int ampliconSize,
                                 @WebParam(name = "primer1Defline") String primer1Defline,
                                 @WebParam(name = "primer1Sequence") String primer1Sequence,
                                 @WebParam(name = "primer2Defline") String primer2Defline,
                                 @WebParam(name = "primer2Sequence") String primer2Sequence,
                                 @WebParam(name = "readLengthMinimum") int readLengthMinimum,
                                 @WebParam(name = "minAvgQualityValue") int minAvgQualityValue,
                                 @WebParam(name = "maxNCountInARead") int maxNCountInARead,
                                 @WebParam(name = "minIdentityCountIn16sHit") int minIdentityCountIn16sHit,
                                 @WebParam(name = "filenamePrefix") String filenamePrefix,
                                 @WebParam(name = "useMsuRdpClassifier") String useMsuRdpClassifier,
                                 @WebParam(name = "iterateCdHitEstClustering") String iterateCdHitEstClustering,
                                 @WebParam(name = "skipClustalWStep") String skipClustalWStep) {
        logger.debug("Web Services - run16sAnalysis() acknowledged");
        try {
            // Check access to the input files
//            if (FileUtil.fileExists(pathToInputFile)){
            AnalysisPipeline16sTask analysisTask = new AnalysisPipeline16sTask();
            analysisTask.setOwner(username);
            analysisTask.setParameter(Task.PARAM_project, project);
            analysisTask.setJobName(jobName);
            analysisTask.setParameter(AnalysisPipeline16sTask.PARAM_fragmentFiles, pathToInputFile);
            analysisTask.setParameter(AnalysisPipeline16sTask.PARAM_subjectDatabase, referenceDataset);
            analysisTask.setParameter(AnalysisPipeline16sTask.PARAM_filenamePrefix, filenamePrefix);
            analysisTask.setParameter(AnalysisPipeline16sTask.PARAM_skipClustalW, skipClustalWStep);
            analysisTask.setParameter(AnalysisPipeline16sTask.PARAM_iterateCdHitESTClustering, iterateCdHitEstClustering);
            analysisTask.setParameter(AnalysisPipeline16sTask.PARAM_useMsuRdpClassifier, useMsuRdpClassifier);
            analysisTask.setParameter(AnalysisPipeline16sTask.PARAM_ampliconSize, Integer.toString(ampliconSize));
            analysisTask.setParameter(AnalysisPipeline16sTask.PARAM_primer1Defline, primer1Defline);
            analysisTask.setParameter(AnalysisPipeline16sTask.PARAM_primer1Sequence, primer1Sequence);
            analysisTask.setParameter(AnalysisPipeline16sTask.PARAM_primer2Defline, primer2Defline);
            analysisTask.setParameter(AnalysisPipeline16sTask.PARAM_primer2Sequence, primer2Sequence);
            analysisTask.setParameter(AnalysisPipeline16sTask.PARAM_readLengthMinimum, Integer.toString(readLengthMinimum));
            analysisTask.setParameter(AnalysisPipeline16sTask.PARAM_minAvgQV, Integer.toString(minAvgQualityValue));
            analysisTask.setParameter(AnalysisPipeline16sTask.PARAM_maxNCount, Integer.toString(maxNCountInARead));
            analysisTask.setParameter(AnalysisPipeline16sTask.PARAM_minIdentCount, Integer.toString(minIdentityCountIn16sHit));
            analysisTask.setParameter(AnalysisPipeline16sTask.PARAM_qualFile, pathToQualFile);
            return saveAndSubmitJob(analysisTask, "AnalysisPipeline16S");
//            }
//            else{
//                return "The input file "+pathToInputFile+" is unreachable or does not exist.";
//            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return "Error: " + ex.getMessage();
        }
    }

//    public String uploadAndFormatMpiBlastDataset(@WebParam(name="username")String username,
//                                              @WebParam(name="token")String token,
//                                              @WebParam(name="project")String project,
//                                              @WebParam(name="workSessionId")String workSessionId,
//                                              @WebParam(name="blastDBName")String blastDBName,
//                                              @WebParam(name="blastDBDescription")String blastDBDescription,
//                                              @WebParam(name="pathToFastaFile")String pathToFastaFile,
//                                              @WebParam(name="numFrags")String numFrags){
//        logger.debug("Web Services - uploadAndFormatMpiBlastDataset() acknowledged");
//        ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
//        StringBuffer sbuf = new StringBuffer("");
//        CreateMpiBlastDatabaseTask createDBTask=null;
//        try {
//            // Save the fasta file as a node and override the path to the system location.
//            // This prevents copying the data and also allows us to work with node id's in the service, which helps
//            // the way the web front-end works, too.
//            File tmpFasta = new File(pathToFastaFile);
//            FastaFileNode fastaNode = new FastaFileNode(username, null, blastDBName, blastDBDescription, Node.VISIBILITY_INACTIVE,
//                    FastaUtil.determineSequenceType(tmpFasta),
//                    0, null); // A super-large input file may take a very long time.  Calculate in the service.  The WS bean will timeout.
//            fastaNode.setPathOverride(pathToFastaFile);
//            fastaNode = (FastaFileNode)computeBean.saveOrUpdateNode(fastaNode);
//
//            createDBTask = new CreateMpiBlastDatabaseTask(null, username, null, null,
//                blastDBName, blastDBDescription, fastaNode.getObjectId().toString(), Integer.valueOf(numFrags));
//            if (null!=workSessionId&&!"".equals(workSessionId)) {
//                createDBTask.setParentTaskId(Long.valueOf(workSessionId));
//            }
//            createDBTask.setParameter(Task.PARAM_project, project);
//            createDBTask = (CreateMpiBlastDatabaseTask)computeBean.saveOrUpdateTask(createDBTask);
//            computeBean.submitJob("CreateMpiBlastDB", createDBTask.getObjectId());
//            sbuf.append("MPI Blast Database Name: ").append(blastDBName).append("\n");
//            sbuf.append("Check status of job ").append(createDBTask.getObjectId()).append(" with service getMpiBlastDatabaseStatus() to know when your database creation is complete.\n");
//        }
//        catch (Exception e) {
//            String error = "There was a problem uploading the fasta file.\n"+e.getMessage()+"\n";
//            sbuf = new StringBuffer(error);
//            if (null!=createDBTask && null!=createDBTask.getObjectId()) {
//                logTaskError(computeBean, e, createDBTask.getObjectId(), error);
//            }
//        }
//        logger.debug("Web Services - uploadAndFormatBlastDataset() complete");
//        return sbuf.toString();
//    }
//

    public String definePluginService(String username, String token, String workSessionId, String name, String initialization, String execution, String finalization,
                                      String readme) {
        return genericServiceDefinition(username, token, workSessionId, name, initialization, execution, finalization, readme);
    }

    public String genericServiceDefinition(String username, String token, String workSessionId, String name, String initialization, String execution, String finalization,
                                           String readme) {
        try {
            if (name == null)
                throw new Exception("genericServiceDefinition: null service name.");
            if (execution == null || execution.length() == 0)
                throw new Exception("genericServiceDefinition: null execution script.");
            if (readme == null || readme.length() == 0)
                throw new Exception("genericServiceDefinition: null readme.");
            if (!(new File(execution)).canExecute())
                throw new Exception("genericServiceDefinition: execution script ".concat(execution).concat(" is not executable"));
            if (!(new File(readme)).canRead())
                throw new Exception("genericServiceDefinition: readme file is not readable: ".concat(readme));
            if (initialization != null && initialization.length() > 0 && !(new File(initialization)).canExecute())
                throw new Exception("genericServiceDefinition: initialization script ".concat(initialization).concat(" is not executable"));
            if (finalization != null && finalization.length() > 0 && !(new File(finalization)).canExecute())
                throw new Exception("genericServiceDefinition: finalization script ".concat(finalization).concat(" is not executable"));
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            GenericServiceDefinitionNode oldDefinition = computeBean.getGenericServiceDefinitionByName(name);
            if (oldDefinition != null && !oldDefinition.getOwner().equals(username))
                throw new Exception("genericServiceDefinition: cannot modify service owned by ".concat(oldDefinition.getOwner()).concat("."));
            GenericServiceDefinitionNode newDefinition = new GenericServiceDefinitionNode();
            newDefinition.setOwner(username);
            newDefinition.setName(name);
            newDefinition.setDescription("Definition for ".concat(name).concat(" Service"));
            newDefinition.setVisibility("inactive");
            newDefinition = (GenericServiceDefinitionNode) computeBean.createNode(newDefinition);
            FileUtil.ensureDirExists(newDefinition.getDirectoryPath());
            if (initialization != null && initialization.length() > 0)
                try {
                    FileWriter fstream = new FileWriter(newDefinition.getDirectoryPath().concat("/initialization.sh"));
                    BufferedWriter out = new BufferedWriter(fstream);
                    out.write(initialization);
                    out.close();
                }
                catch (Exception e) {
                    throw new Exception("genericServiceDefinition: write initialization script: ".concat(e.getMessage()));
                }
            if (execution != null)
                try {
                    FileWriter fstream = new FileWriter(newDefinition.getDirectoryPath().concat("/execution.sh"));
                    BufferedWriter out = new BufferedWriter(fstream);
                    out.write(execution);
                    out.close();
                }
                catch (Exception e) {
                    throw new Exception("genericServiceDefinition: write execution script: ".concat(e.getMessage()));
                }
            if (finalization != null)
                try {
                    FileWriter fstream = new FileWriter(newDefinition.getDirectoryPath().concat("/finalization.sh"));
                    BufferedWriter out = new BufferedWriter(fstream);
                    out.write(finalization);
                    out.close();
                }
                catch (Exception e) {
                    throw new Exception("genericServiceDefinition: write finalization script: ".concat(e.getMessage()));
                }
            if (readme != null)
                try {
                    FileWriter fstream = new FileWriter(newDefinition.getDirectoryPath().concat("/readme.txt"));
                    BufferedWriter out = new BufferedWriter(fstream);
                    out.write(readme);
                    out.close();
                }
                catch (Exception e) {
                    throw new Exception("genericServiceDefinition: write readme file: ".concat(e.getMessage()));
                }
            if (oldDefinition != null) {
                oldDefinition.setVisibility("inactive");
                computeBean.saveOrUpdateNode(oldDefinition);
            }
            newDefinition.setVisibility("public");
            computeBean.saveOrUpdateNode(newDefinition);
            newDefinition = computeBean.getGenericServiceDefinitionByName(name);
            return "service name: ".concat(name).concat("\ndefinition location: ").concat(newDefinition.getDirectoryPath());
        }
        catch (Exception e) {
            return "genericServiceDefinition (".concat(name).concat(") error: ").concat(e.getMessage());
        }
    }

    public String getPluginServiceHelp(String serviceName) {
        return genericServiceHelp(serviceName);
    }

    public String genericServiceHelp(String serviceName) {
        GenericServiceDefinitionNode serviceDefinition;
        String readmeLink;
        try {
            if (serviceName == null || serviceName.length() == 0)
                throw new Exception("genericServicHelp: service name not specified");
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            serviceDefinition = computeBean.getGenericServiceDefinitionByName(serviceName);
            if (null == serviceDefinition)
                return "service ".concat(serviceName).concat(" does not exist.");

            readmeLink = serviceDefinition.getFilePathByTag("readme");
            if (null == readmeLink)
                return "No help is available for service ".concat(serviceName).concat(".");
            String readmePath = TextFileIO.readTextFile(readmeLink);
            readmePath = (new File(readmePath)).getAbsolutePath();
            String readmeText = TextFileIO.readTextFile(readmePath);
            return "Service ".concat(serviceName).concat(" help:\n").concat(readmeText);
        }
        catch (Exception e) {
            return "genericServiceHelp (".concat(serviceName).concat(") error: ").concat(e.getMessage());
        }
    }

    public String executePluginService(String username, String token, String project, String workSessionId, String jobName, String serviceName, String serviceOptions,
                                       String gridOptions) {
        return genericService(username, token, project, workSessionId, jobName, serviceName, serviceOptions, gridOptions);
    }

    public String genericService(String username, String token, String project, String workSessionId, String jobName, String serviceName, String serviceOptions,
                                 String gridOptions) {
        logger.debug("Web Services - run16sAnalysis() acknowledged");
        try {
/*
            ComputeDAO computeDAO = new ComputeDAO(logger);
            GenericServiceDefinitionNode serviceDefinition = computeDAO.getGenericServiceDefinitionByName(serviceName);
            if ( serviceDefinition == null ) {
                 return "genericService error: service \"" + serviceName + "\" is not defined.";
            }
*/
            GenericServiceTask serviceTask = new GenericServiceTask();
            serviceTask.setOwner(username);
            serviceTask.setParameter("project", project);
            if (null != workSessionId && workSessionId.length() > 0)
                serviceTask.setParentTaskId((new Integer(workSessionId)).longValue());
            serviceTask.setJobName(jobName);
            serviceTask.setParameter("service name", serviceName);
            serviceTask.setParameter("service options", serviceOptions);
            serviceTask.setParameter("grid_options", gridOptions);
            return saveAndSubmitJob(serviceTask, "GenericService");
        }
        catch (Exception e) {
            e.printStackTrace();
            return (new StringBuilder()).append("genericService error: ").append(e.getMessage()).toString();
        }
    }

    public String getBlastDBInfo(@WebParam(name = "blastdb") String blastdb) {
        try {
            BlastDatabaseFileNode blastdbNode = null;
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();

            Long nodeId = new Long(blastdb);
            if (null != nodeId) {
                blastdbNode = (BlastDatabaseFileNode) computeBean.getNodeById(new Long(blastdb));
            }
            if (null == blastdbNode) {
                List blastdbList = computeBean.getNodeByName(blastdb);
                if (null == blastdbList) {
                    return "could not find a blast database identified by \"" + blastdb + "\".";
                }
                Iterator iter = blastdbList.iterator();
                while (iter.hasNext()) {
                    Node tmpnode = (Node) iter.next();
                    if (!tmpnode.getVisibility().equals(Node.VISIBILITY_PUBLIC)) {
                        iter.remove();
                    }
                }
                if (blastdbList.size() > 1) {
                    return "\"" + blastdb + "\" is an ambiguous identifier.";
                }
                blastdbNode = (BlastDatabaseFileNode) blastdbList.get(0);
            }
            String id = blastdbNode.getObjectId().toString();
            String name = blastdbNode.getName();
            if (null == name) {
                name = "n/a";
            }
            String description = blastdbNode.getDescription();
            if (null == description) {
                description = "n/a";
            }
            String sequenceType = blastdbNode.getSequenceType();
            Integer tmpint = blastdbNode.getSequenceCount();
            String sequenceCount;
            if (null == tmpint) {
                sequenceCount = "n/a";
            }
            else {
                sequenceCount = tmpint.toString();
            }
            Long tmplong = blastdbNode.getLength();
            String length;
            if (null == tmplong) {
                length = "n/a";
            }
            else {
                length = tmplong.toString();
            }
            tmpint = blastdbNode.getPartitionCount();
            String partitionCount;
            if (null == tmpint) {
                partitionCount = "n/a";
            }
            else {
                partitionCount = tmpint.toString();
            }
            String directoryPath = blastdbNode.getDirectoryPath();

            return
                    "id: " + id + "\n" +
                            "name: " + name + "\n" +
                            "description: " + description + "\n" +
                            "location: " + directoryPath + "\n" +
                            "sequence type: " + sequenceType + "\n" +
                            "sequence count: " + sequenceCount + "\n" +
                            "sequence length: " + length + "\n" +
                            "partition count: " + partitionCount;
        }
        catch (Exception e) {
            e.printStackTrace();
            return (new StringBuilder()).append("getBlastDBInfo error: ").append(e.getMessage()).toString();
        }
    }

    public String runInspect(@WebParam(name = "username") String username,
                             @WebParam(name = "token") String token,
                             @WebParam(name = "project") String project,
                             @WebParam(name = "workSessionId") String workSessionId,
                             @WebParam(name = "jobName") String jobName,
                             @WebParam(name = "archiveFilePath") String archiveFilePath) throws RemoteException {
        logger.debug("Web Services - runInspect() acknowledged");
        InspectTask inspectTask = new InspectTask();
        inspectTask.setOwner(username);
        inspectTask.setJobName(jobName);
        inspectTask.setParameter(InspectTask.PARAM_archiveFilePath, archiveFilePath);
        inspectTask.setParameter(Task.PARAM_project, project);
        if (null != workSessionId && !"".equals(workSessionId)) {
            inspectTask.setParentTaskId(Long.valueOf(workSessionId));
        }
        logger.debug("Web Services - runInspect() complete");
        return saveAndSubmitJob(inspectTask, "Inspect");
    }

    public String deleteBlastDatabase(@WebParam(name = "username") String username,
                                      @WebParam(name = "token") String token,
                                      @WebParam(name = "blastDbNodeId") String blastDbNodeId) throws RemoteException {
        return deleteNodeId(username, token, blastDbNodeId);
    }

    public String deleteNodeId(@WebParam(name = "username") String username,
                               @WebParam(name = "token") String token,
                               @WebParam(name = "nodeId") String nodeId) throws RemoteException {
        logger.debug("Web Services - deleteNodeId() acknowledged");
        try {
            boolean success = EJBFactory.getLocalComputeBean().deleteNode(username, Long.valueOf(nodeId), true);
            if (!success) {
                throw new Exception("Call to delete node failed on the server.");
            }
        }
        catch (Exception e) {
            String nodeError = "Unable to successfully call deleteNode with " + nodeId + " for user " + username;
            logger.error(nodeError, e);
            return nodeError;
        }
        logger.debug("Web Services - deleteNodeId() complete");
        return "Deletion successful";
    }

    public String updateGenomeProject(@WebParam(name="username")String username,
                             @WebParam(name="token")String token,
                             @WebParam(name="project")String project,
                             @WebParam(name="workSessionId")String workSessionId,
                             @WebParam(name="jobName")String jobName,
                             @WebParam(name="projectMode")String projectMode,
                             @WebParam(name="genomeProjectStatus")String genomeProjectStatus) throws RemoteException {

        logger.debug("updateGenomeProject(\n   \""+username+
                "\",\n   \""+token+
                "\",\n   \""+project+
                "\",\n   \""+workSessionId+
                "\",\n   \""+jobName+
                "\",\n   \""+projectMode+
                "\",\n   \""+genomeProjectStatus+"\")\n");
        try {
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            StringBuffer sbuf = new StringBuffer("");
            Task task = new GenomeProjectUpdateTask(projectMode,genomeProjectStatus,null,username,null,null);
            task.setJobName(jobName);
            task.setParameter("project", project);
             if (null != workSessionId && workSessionId.length() > 0)
                task.setParentTaskId((new Integer(workSessionId)).longValue());
            task = computeBean.saveOrUpdateTask(task);
            computeBean.submitJob("GPUpdate", task.getObjectId());
            sbuf.append("Check status of job ").append(task.getObjectId()).append(" with service getTaskStatus() to know when your genome update is complete.\n");
            return sbuf.toString();
//            return saveAndSubmitJob(task, "GenomeProjectUpdate");
        }
        catch (Exception e) {
            e.printStackTrace();
            return (new StringBuilder()).append("GenomeProjectUpdate error: ").append(e.getMessage()).toString();
        }
    }
}
