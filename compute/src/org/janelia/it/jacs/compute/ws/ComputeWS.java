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

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Sep 11, 2008
 * Time: 11:40:25 AM
 */
@SOAPBinding(style = SOAPBinding.Style.RPC)
@WebService()
public interface ComputeWS extends Remote {
    // These methods allow someone running an external pipeline to bin their work into a common area
    public String createWorkSession(@WebParam(name="username")String username,
                            @WebParam(name="token")String token,
                            @WebParam(name="sessionName")String sessionName);
    // This method copies all data from the local session FileNode to the users directory
    public String exportWorkFromSession(@WebParam(name="username")String username,
                            @WebParam(name="token")String token,
                            @WebParam(name="workSessionId")String workSessionId,
                            @WebParam(name="finalOutputDirectory")String finalOutputDirectory);
    // This removes all files under the FileNode associated with the session (workflow task)
    public String deleteAllWorkForSession(@WebParam(name="username")String username,
                            @WebParam(name="token")String token,
                            @WebParam(name="workSessionId")String workSessionId);

    public String runBlastN(@WebParam(name="username")String username,
                            @WebParam(name="token")String token,
                            @WebParam(name="project")String project,
                            @WebParam(name="workSessionId")String workSessionId,
                            @WebParam(name="jobName")String jobName,
                            @WebParam(name="subjectDBIdentifier")String subjectDBIdentifier,
                            @WebParam(name="queryFastaFileNodeId")String queryFastaFileNodeId,
                            @WebParam(name="databaseAlignmentsPerQuery")int databaseAlignmentsPerQuery,
                            @WebParam(name="filter")String filter,
                            @WebParam(name="eValueExponent")int eValueExponent,
                            @WebParam(name="lowercaseFiltering")boolean lowercaseFiltering,
                            @WebParam(name="believeDefline")boolean believeDefline,
                            @WebParam(name="databaseSize")long databaseSize,
                            @WebParam(name="gapExtendCost")int gapExtendCost,
                            @WebParam(name="gappedAlignment")boolean gappedAlignment,
                            @WebParam(name="hitExtensionThreshold")int hitExtensionThreshold,
                            @WebParam(name="matrix")String matrix,
                            @WebParam(name="multihitWindowSize")int multihitWindowSize,
                            @WebParam(name="searchStrand")String searchStrand,
                            @WebParam(name="ungappedExtensionDropoff")int ungappedExtensionDropoff,
                            @WebParam(name="bestHitsToKeep")int bestHitsToKeep,
                            @WebParam(name="finalGappedDropoff")int finalGappedDropoff,
                            @WebParam(name="gapOpenCost")int gapOpenCost,
                            @WebParam(name="gappedAlignmentDropoff")int gappedAlignmentDropoff,
                            @WebParam(name="matchReward")int matchReward,
                            @WebParam(name="mismatchPenalty")int mismatchPenalty,
                            @WebParam(name="searchSize")int searchSize,
                            @WebParam(name="showGIs")boolean showGIs,
                            @WebParam(name="wordsize")int wordsize,
                            @WebParam(name="formatTypesCsv")String formatTypesCsv) throws RemoteException;

    public String runVTeraBlastN(@WebParam(name="username")String username,
                            @WebParam(name="token")String token,
                            @WebParam(name="project")String project,
                            @WebParam(name="workSessionId")String workSessionId,
                            @WebParam(name="jobName")String jobName,
                            @WebParam(name="subjectDBIdentifier")String subjectDBIdentifier,
                            @WebParam(name="queryFastaFileNodeId")String queryFastaFileNodeId,
                            @WebParam(name="databaseAlignmentsPerQuery")int databaseAlignmentsPerQuery,
                            @WebParam(name="filter")String filter,
                            @WebParam(name="eValueExponent")int eValueExponent,
                            @WebParam(name="lowercaseFiltering")boolean lowercaseFiltering,
                            @WebParam(name="believeDefline")boolean believeDefline,
                            @WebParam(name="databaseSize")long databaseSize,
                            @WebParam(name="gapExtendCost")int gapExtendCost,
                            @WebParam(name="gappedAlignment")boolean gappedAlignment,
                            @WebParam(name="hitExtensionThreshold")int hitExtensionThreshold,
                            @WebParam(name="matrix")String matrix,
                            @WebParam(name="multihitWindowSize")int multihitWindowSize,
                            @WebParam(name="searchStrand")String searchStrand,
                            @WebParam(name="ungappedExtensionDropoff")int ungappedExtensionDropoff,
                            @WebParam(name="bestHitsToKeep")int bestHitsToKeep,
                            @WebParam(name="finalGappedDropoff")int finalGappedDropoff,
                            @WebParam(name="gapOpenCost")int gapOpenCost,
                            @WebParam(name="gappedAlignmentDropoff")int gappedAlignmentDropoff,
                            @WebParam(name="matchReward")int matchReward,
                            @WebParam(name="mismatchPenalty")int mismatchPenalty,
                            @WebParam(name="searchSize")int searchSize,
                            @WebParam(name="showGIs")boolean showGIs,
                            @WebParam(name="wordsize")int wordsize,
                            @WebParam(name="formatTypesCsv")String formatTypesCsv) throws RemoteException;

    public String runMegaBlast(@WebParam(name="username")String username,
                            @WebParam(name="token")String token,
                            @WebParam(name="project")String project,
                            @WebParam(name="workSessionId")String workSessionId,
                            @WebParam(name="jobName")String jobName,
                            @WebParam(name="subjectDBIdentifier")String subjectDBIdentifier,
                            @WebParam(name="queryFastaFileNodeId")String queryFastaFileNodeId,
                            @WebParam(name="databaseAlignmentsPerQuery")int databaseAlignmentsPerQuery,
                            @WebParam(name="filter")String filter,
                            @WebParam(name="eValueExponent")int eValueExponent,
                            @WebParam(name="lowercaseFiltering")boolean lowercaseFiltering,
                            @WebParam(name="believeDefline")boolean believeDefline,
                            @WebParam(name="databaseSize")long databaseSize,
                            @WebParam(name="gapExtendCost")int gapExtendCost,
                            @WebParam(name="gappedAlignment")boolean gappedAlignment,
                            @WebParam(name="hitExtensionThreshold")int hitExtensionThreshold,
                            @WebParam(name="matrix")String matrix,
                            @WebParam(name="multihitWindowSize")int multihitWindowSize,
                            @WebParam(name="showGIs")boolean showGIs,
                            @WebParam(name="wordsize")int wordsize,
                            @WebParam(name="bestHitsToKeep")int bestHitsToKeep,
                            @WebParam(name="finalGappedDropoff")int finalGappedDropoff,
                            @WebParam(name="gapOpenCost")int gapOpenCost,
                            @WebParam(name="gappedAlignmentDropoff")int gappedAlignmentDropoff,
                            @WebParam(name="matchReward")int matchReward,
                            @WebParam(name="mismatchPenalty")int mismatchPenalty,
                            @WebParam(name="searchSize")int searchSize,
                            @WebParam(name="ungappedExtensionDropoff")int ungappedExtensionDropoff,
                            @WebParam(name="formatTypesCsv")String formatTypesCsv) throws RemoteException;

    public String runTBlastX(@WebParam(name="username")String username,
                            @WebParam(name="token")String token,
                            @WebParam(name="project")String project,
                            @WebParam(name="workSessionId")String workSessionId,
                            @WebParam(name="jobName")String jobName,
                            @WebParam(name="subjectDBIdentifier")String subjectDBIdentifier,
                            @WebParam(name="queryFastaFileNodeId")String queryFastaFileNodeId,
                            @WebParam(name="databaseAlignmentsPerQuery")int databaseAlignmentsPerQuery,
                            @WebParam(name="filter")String filter,
                            @WebParam(name="eValueExponent")int eValueExponent,
                            @WebParam(name="lowercaseFiltering")boolean lowercaseFiltering,
                            @WebParam(name="believeDefline")boolean believeDefline,
                            @WebParam(name="databaseSize")long databaseSize,
                            @WebParam(name="gapExtendCost")int gapExtendCost,
                            @WebParam(name="gappedAlignmentDropoff")int gappedAlignmentDropoff,
                            @WebParam(name="matrix")String matrix,
                            @WebParam(name="searchSize")int searchSize,
                            @WebParam(name="showGIs")boolean showGIs,
                            @WebParam(name="wordsize")int wordsize,
                            @WebParam(name="bestHitsToKeep")int bestHitsToKeep,
                            @WebParam(name="finalGappedDropoff")int finalGappedDropoff,
                            @WebParam(name="gapOpenCost")int gapOpenCost,
                            @WebParam(name="hitExtensionThreshold")int hitExtensionThreshold,
                            @WebParam(name="multihitWindowSize")int multihitWindowSize,
                            @WebParam(name="searchStrand")String searchStrand,
                            @WebParam(name="ungappedExtensionDropoff")int ungappedExtensionDropoff,
                            @WebParam(name="formatTypesCsv")String formatTypesCsv) throws RemoteException;

    public String runVTeraTBlastX(@WebParam(name="username")String username,
                            @WebParam(name="token")String token,
                            @WebParam(name="project")String project,
                            @WebParam(name="workSessionId")String workSessionId,
                            @WebParam(name="jobName")String jobName,
                            @WebParam(name="subjectDBIdentifier")String subjectDBIdentifier,
                            @WebParam(name="queryFastaFileNodeId")String queryFastaFileNodeId,
                            @WebParam(name="databaseAlignmentsPerQuery")int databaseAlignmentsPerQuery,
                            @WebParam(name="filter")String filter,
                            @WebParam(name="eValueExponent")int eValueExponent,
                            @WebParam(name="lowercaseFiltering")boolean lowercaseFiltering,
                            @WebParam(name="believeDefline")boolean believeDefline,
                            @WebParam(name="databaseSize")long databaseSize,
                            @WebParam(name="gapExtendCost")int gapExtendCost,
                            @WebParam(name="gappedAlignmentDropoff")int gappedAlignmentDropoff,
                            @WebParam(name="matrix")String matrix,
                            @WebParam(name="searchSize")int searchSize,
                            @WebParam(name="showGIs")boolean showGIs,
                            @WebParam(name="wordsize")int wordsize,
                            @WebParam(name="bestHitsToKeep")int bestHitsToKeep,
                            @WebParam(name="finalGappedDropoff")int finalGappedDropoff,
                            @WebParam(name="gapOpenCost")int gapOpenCost,
                            @WebParam(name="hitExtensionThreshold")int hitExtensionThreshold,
                            @WebParam(name="multihitWindowSize")int multihitWindowSize,
                            @WebParam(name="searchStrand")String searchStrand,
                            @WebParam(name="ungappedExtensionDropoff")int ungappedExtensionDropoff,
                            @WebParam(name="formatTypesCsv")String formatTypesCsv) throws RemoteException;

    public String runTBlastN(@WebParam(name="username")String username,
                            @WebParam(name="token")String token,
                            @WebParam(name="project")String project,
                            @WebParam(name="workSessionId")String workSessionId,
                            @WebParam(name="jobName")String jobName,
                            @WebParam(name="subjectDBIdentifier")String subjectDBIdentifier,
                            @WebParam(name="queryFastaFileNodeId")String queryFastaFileNodeId,
                            @WebParam(name="databaseAlignmentsPerQuery")int databaseAlignmentsPerQuery,
                            @WebParam(name="filter")String filter,
                            @WebParam(name="eValueExponent")int eValueExponent,
                            @WebParam(name="lowercaseFiltering")boolean lowercaseFiltering,
                            @WebParam(name="believeDefline")boolean believeDefline,
                            @WebParam(name="databaseSize")long databaseSize,
                            @WebParam(name="gapExtendCost")int gapExtendCost,
                            @WebParam(name="gappedAlignment")boolean gappedAlignment,
                            @WebParam(name="hitExtensionThreshold")int hitExtensionThreshold,
                            @WebParam(name="multihitWindowSize")int multihitWindowSize,
                            @WebParam(name="showGIs")boolean showGIs,
                            @WebParam(name="wordsize")int wordsize,
                            @WebParam(name="bestHitsToKeep")int bestHitsToKeep,
                            @WebParam(name="finalGappedDropoff")int finalGappedDropoff,
                            @WebParam(name="gapOpenCost")int gapOpenCost,
                            @WebParam(name="gappedAlignmentDropoff")int gappedAlignmentDropoff,
                            @WebParam(name="matrix")String matrix,
                            @WebParam(name="searchSize")int searchSize,
                            @WebParam(name="ungappedExtensionDropoff")int ungappedExtensionDropoff,
                            @WebParam(name="formatTypesCsv")String formatTypesCsv) throws RemoteException;

    public String runVTeraTBlastN(@WebParam(name="username")String username,
                            @WebParam(name="token")String token,
                            @WebParam(name="project")String project,
                            @WebParam(name="workSessionId")String workSessionId,
                            @WebParam(name="jobName")String jobName,
                            @WebParam(name="subjectDBIdentifier")String subjectDBIdentifier,
                            @WebParam(name="queryFastaFileNodeId")String queryFastaFileNodeId,
                            @WebParam(name="databaseAlignmentsPerQuery")int databaseAlignmentsPerQuery,
                            @WebParam(name="filter")String filter,
                            @WebParam(name="eValueExponent")int eValueExponent,
                            @WebParam(name="lowercaseFiltering")boolean lowercaseFiltering,
                            @WebParam(name="believeDefline")boolean believeDefline,
                            @WebParam(name="databaseSize")long databaseSize,
                            @WebParam(name="gapExtendCost")int gapExtendCost,
                            @WebParam(name="gappedAlignment")boolean gappedAlignment,
                            @WebParam(name="hitExtensionThreshold")int hitExtensionThreshold,
                            @WebParam(name="multihitWindowSize")int multihitWindowSize,
                            @WebParam(name="showGIs")boolean showGIs,
                            @WebParam(name="wordsize")int wordsize,
                            @WebParam(name="bestHitsToKeep")int bestHitsToKeep,
                            @WebParam(name="finalGappedDropoff")int finalGappedDropoff,
                            @WebParam(name="gapOpenCost")int gapOpenCost,
                            @WebParam(name="gappedAlignmentDropoff")int gappedAlignmentDropoff,
                            @WebParam(name="matrix")String matrix,
                            @WebParam(name="searchSize")int searchSize,
                            @WebParam(name="ungappedExtensionDropoff")int ungappedExtensionDropoff,
                            @WebParam(name="formatTypesCsv")String formatTypesCsv) throws RemoteException;

    public String runBlastP(@WebParam(name="username")String username,
                            @WebParam(name="token")String token,
                            @WebParam(name="project")String project,
                            @WebParam(name="workSessionId")String workSessionId,
                            @WebParam(name="jobName")String jobName,
                            @WebParam(name="subjectDBIdentifier")String subjectDBIdentifier,
                            @WebParam(name="queryFastaFileNodeId")String queryFastaFileNodeId,
                            @WebParam(name="databaseAlignmentsPerQuery")int databaseAlignmentsPerQuery,
                            @WebParam(name="filter")String filter,
                            @WebParam(name="eValueExponent")int eValueExponent,
                            @WebParam(name="lowercaseFiltering")boolean lowercaseFiltering,
                            @WebParam(name="believeDefline")boolean believeDefline,
                            @WebParam(name="databaseSize")long databaseSize,
                            @WebParam(name="gapExtendCost")int gapExtendCost,
                            @WebParam(name="gappedAlignment")boolean gappedAlignment,
                            @WebParam(name="hitExtensionThreshold")int hitExtensionThreshold,
                            @WebParam(name="multihitWindowSize")int multihitWindowSize,
                            @WebParam(name="showGIs")boolean showGIs,
                            @WebParam(name="wordsize")int wordsize,
                            @WebParam(name="bestHitsToKeep")int bestHitsToKeep,
                            @WebParam(name="finalGappedDropoff")int finalGappedDropoff,
                            @WebParam(name="gapOpenCost")int gapOpenCost,
                            @WebParam(name="gappedAlignmentDropoff")int gappedAlignmentDropoff,
                            @WebParam(name="matrix")String matrix,
                            @WebParam(name="searchSize")int searchSize,
                            @WebParam(name="ungappedExtensionDropoff")int ungappedExtensionDropoff,
                            @WebParam(name="formatTypesCsv")String formatTypesCsv) throws RemoteException;

    public String runVTeraBlastP(@WebParam(name="username")String username,
                            @WebParam(name="token")String token,
                            @WebParam(name="project")String project,
                            @WebParam(name="workSessionId")String workSessionId,
                            @WebParam(name="jobName")String jobName,
                            @WebParam(name="subjectDBIdentifier")String subjectDBIdentifier,
                            @WebParam(name="queryFastaFileNodeId")String queryFastaFileNodeId,
                            @WebParam(name="databaseAlignmentsPerQuery")int databaseAlignmentsPerQuery,
                            @WebParam(name="filter")String filter,
                            @WebParam(name="eValueExponent")int eValueExponent,
                            @WebParam(name="lowercaseFiltering")boolean lowercaseFiltering,
                            @WebParam(name="believeDefline")boolean believeDefline,
                            @WebParam(name="databaseSize")long databaseSize,
                            @WebParam(name="gapExtendCost")int gapExtendCost,
                            @WebParam(name="gappedAlignment")boolean gappedAlignment,
                            @WebParam(name="hitExtensionThreshold")int hitExtensionThreshold,
                            @WebParam(name="multihitWindowSize")int multihitWindowSize,
                            @WebParam(name="showGIs")boolean showGIs,
                            @WebParam(name="wordsize")int wordsize,
                            @WebParam(name="bestHitsToKeep")int bestHitsToKeep,
                            @WebParam(name="finalGappedDropoff")int finalGappedDropoff,
                            @WebParam(name="gapOpenCost")int gapOpenCost,
                            @WebParam(name="gappedAlignmentDropoff")int gappedAlignmentDropoff,
                            @WebParam(name="matrix")String matrix,
                            @WebParam(name="searchSize")int searchSize,
                            @WebParam(name="ungappedExtensionDropoff")int ungappedExtensionDropoff,
                            @WebParam(name="formatTypesCsv")String formatTypesCsv) throws RemoteException;

    public String runBlastX(@WebParam(name="username")String username,
                            @WebParam(name="token")String token,
                            @WebParam(name="project")String project,
                            @WebParam(name="workSessionId")String workSessionId,
                            @WebParam(name="jobName")String jobName,
                            @WebParam(name="subjectDBIdentifier")String subjectDBIdentifier,
                            @WebParam(name="queryFastaFileNodeId")String queryFastaFileNodeId,
                            @WebParam(name="databaseAlignmentsPerQuery")int databaseAlignmentsPerQuery,
                            @WebParam(name="filter")String filter,
                            @WebParam(name="eValueExponent")int eValueExponent,
                            @WebParam(name="lowercaseFiltering")boolean lowercaseFiltering,
                            @WebParam(name="believeDefline")boolean believeDefline,
                            @WebParam(name="databaseSize")long databaseSize,
                            @WebParam(name="frameshiftPenalty")String frameshiftPenalty,
                            @WebParam(name="gapOpenCost")int gapOpenCost,
                            @WebParam(name="gappedAlignmentDropoff")int gappedAlignmentDropoff,
                            @WebParam(name="matrix")String matrix,
                            @WebParam(name="searchSize")int searchSize,
                            @WebParam(name="showGIs")boolean showGIs,
                            @WebParam(name="wordsize")int wordsize,
                            @WebParam(name="bestHitsToKeep")int bestHitsToKeep,
                            @WebParam(name="finalGappedDropoff")int finalGappedDropoff,
                            @WebParam(name="gapExtendCost")int gapExtendCost,
                            @WebParam(name="gappedAlignment")boolean gappedAlignment,
                            @WebParam(name="hitExtensionThreshold")int hitExtensionThreshold,
                            @WebParam(name="multihitWindowSize")int multihitWindowSize,
                            @WebParam(name="searchStrand")String searchStrand,
                            @WebParam(name="ungappedExtensionDropoff")int ungappedExtensionDropoff,
                            @WebParam(name="formatTypesCsv")String formatTypesCsv) throws RemoteException;

    public String runVTeraBlastX(@WebParam(name="username")String username,
                            @WebParam(name="token")String token,
                            @WebParam(name="project")String project,
                            @WebParam(name="workSessionId")String workSessionId,
                            @WebParam(name="jobName")String jobName,
                            @WebParam(name="subjectDBIdentifier")String subjectDBIdentifier,
                            @WebParam(name="queryFastaFileNodeId")String queryFastaFileNodeId,
                            @WebParam(name="databaseAlignmentsPerQuery")int databaseAlignmentsPerQuery,
                            @WebParam(name="filter")String filter,
                            @WebParam(name="eValueExponent")int eValueExponent,
                            @WebParam(name="lowercaseFiltering")boolean lowercaseFiltering,
                            @WebParam(name="believeDefline")boolean believeDefline,
                            @WebParam(name="databaseSize")long databaseSize,
                            @WebParam(name="frameshiftPenalty")String frameshiftPenalty,
                            @WebParam(name="gapOpenCost")int gapOpenCost,
                            @WebParam(name="gappedAlignmentDropoff")int gappedAlignmentDropoff,
                            @WebParam(name="matrix")String matrix,
                            @WebParam(name="searchSize")int searchSize,
                            @WebParam(name="showGIs")boolean showGIs,
                            @WebParam(name="wordsize")int wordsize,
                            @WebParam(name="bestHitsToKeep")int bestHitsToKeep,
                            @WebParam(name="finalGappedDropoff")int finalGappedDropoff,
                            @WebParam(name="gapExtendCost")int gapExtendCost,
                            @WebParam(name="gappedAlignment")boolean gappedAlignment,
                            @WebParam(name="hitExtensionThreshold")int hitExtensionThreshold,
                            @WebParam(name="multihitWindowSize")int multihitWindowSize,
                            @WebParam(name="searchStrand")String searchStrand,
                            @WebParam(name="ungappedExtensionDropoff")int ungappedExtensionDropoff,
                            @WebParam(name="formatTypesCsv")String formatTypesCsv) throws RemoteException;

    public String runHmmpfam(@WebParam(name="username")String username,
                             @WebParam(name="token")String token,
                             @WebParam(name="project")String project,
                             @WebParam(name="workSessionId")String workSessionId,
                             @WebParam(name="jobName")String jobName,
                             @WebParam(name="subjectDBIdentifier")String subjectDBIdentifier,
                             @WebParam(name="queryFastaFileNodeId")String queryFastaFileNodeId,
                             @WebParam(name="maxBestDomainAligns")int maxBestDomainAligns,
                             @WebParam(name="evalueCutoff")String evalueCutoff,
                             @WebParam(name="tbitThreshold")String tbitThreshold,
                             @WebParam(name="zModelNumber")int zModelNumber,
                             @WebParam(name="useHmmAccessions")boolean useHmmAccessions,
                             @WebParam(name="cutGa")boolean cutGa,
                             @WebParam(name="cutNc")boolean cutNc,
                             @WebParam(name="cutTc")boolean cutTc,
                             @WebParam(name="domE")String domE,
                             @WebParam(name="domT")String domT,
                             @WebParam(name="null2")boolean null2) throws RemoteException;

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
                          @WebParam(name = "fwdThresholdExponent") String fwdThresholdExponent) throws RemoteException;

    public String runTrnaScan(@WebParam(name="username")String username,
                             @WebParam(name="token")String token,
                             @WebParam(name="project")String project,
                             @WebParam(name="workSessionId")String workSessionId,
                             @WebParam(name="jobName")String jobName,
                             @WebParam(name="inputFastaFileNodeId")String inputFastaFileNodeId,
                             @WebParam(name="searchType")String searchType,
                             @WebParam(name="options")String options) throws RemoteException;

    public String runRrnaScan(@WebParam(name="username")String username,
                             @WebParam(name="token")String token,
                             @WebParam(name="project")String project,
                             @WebParam(name="workSessionId")String workSessionId,
                             @WebParam(name="jobName")String jobName,
                             @WebParam(name="inputFastaFileNodeId")String inputFastaFileNodeId,
                             @WebParam(name="initial_blast_options")String initialBlastOptions,
                             @WebParam(name="second_blast_options")String secondBlastOptions,
                             @WebParam(name="min_5S_length")String min5Slength,
                             @WebParam(name="min_16S_length")String min16Slength,
                             @WebParam(name="min_18S_length")String min18Slength,
                             @WebParam(name="min_23S_length")String min23Slength) throws RemoteException;

    public String runSimpleOrfCaller(@WebParam(name="username")String username,
                             @WebParam(name="token")String token,
                             @WebParam(name="project")String project,
                             @WebParam(name="workSessionId")String workSessionId,
                             @WebParam(name="jobName")String jobName,
                             @WebParam(name="inputFastaFileNodeId")String inputFastaFileNodeId,
                             @WebParam(name="translation_table")String translationTable,
                             @WebParam(name="beginning_as_start")String beginningAsStart,
                             @WebParam(name="end_as_stop")String endAsStop,
                             @WebParam(name="assume_stops")String assumeStops,
                             @WebParam(name="full_orfs")String fullOrfs,
                             @WebParam(name="min_orf_size")String minOrfSize,
                             @WebParam(name="max_orf_size")String maxOrfSize,
                             @WebParam(name="min_unmasked_size")String minUnmaskedSize,
                             @WebParam(name="frames")String frames,
                             @WebParam(name="force_methionine")String forceMethionine,
                             @WebParam(name="header_additions")String headerAdditions) throws RemoteException;

    public String runMetaGenoOrfCaller(@WebParam(name="username")String username,
                             @WebParam(name="token")String token,
                             @WebParam(name="project")String project,
                             @WebParam(name="workSessionId")String workSessionId,
                             @WebParam(name="jobName")String jobName,
                             @WebParam(name="inputFastaFileNodeId")String inputFastaFileNodeId,
                             @WebParam(name="useClearRange")String useClearRange,
                             @WebParam(name="translationTable")String translationTable,
                             @WebParam(name="beginningAsStart")String beginningAsStart,
                             @WebParam(name="endAsStop")String endAsStop,
                             @WebParam(name="aassumeStops")String assumeStops,
                             @WebParam(name="fullOrfs")String fullOrfs,
                             @WebParam(name="minOrfSize")String minOrfSize,
                             @WebParam(name="maxOrfSize")String maxOrfSize,
                             @WebParam(name="minUnmaskedSize")String minUnmaskedSize,
                             @WebParam(name="frames")String frames,
                             @WebParam(name="forceMethionine")String forceMethionine,
                             @WebParam(name="clearRangeMinOrfSize")String clearRangeMinOrfSize
                             ) throws RemoteException;

    public String runMetaGenoAnnotation(@WebParam(name="username")String username,
                             @WebParam(name="token")String token,
                             @WebParam(name="project")String project,
                             @WebParam(name="workSessionId")String workSessionId,
                             @WebParam(name="jobName")String jobName,
                             @WebParam(name="inputFastaFileNodeId")String inputFastaFileNodeId) throws RemoteException;

    public String runMetaGenoCombinedOrfAnno(@WebParam(name="username")String username,
                             @WebParam(name="token")String token,
                             @WebParam(name="project")String project,
                             @WebParam(name="workSessionId")String workSessionId,
                             @WebParam(name="jobName")String jobName,
                             @WebParam(name="inputFastaFileNodeId")String inputFastaFileNodeId,
                             @WebParam(name="useClearRange")String useClearRange) throws RemoteException;

    public String runMetagene(@WebParam(name="username")String username,
                              @WebParam(name="token")String token,
                              @WebParam(name="project")String project,
                              @WebParam(name="workSessionId")String workSessionId,
                              @WebParam(name="jobName")String jobName,
                              @WebParam(name="inputFastaFileNodeId")String inputFastaFileNodeId) throws RemoteException;

    public String runReversePsiBlast(@WebParam(name="username")String username,
                            @WebParam(name="token")String token,
                            @WebParam(name="project")String project,
                            @WebParam(name="workSessionId")String workSessionId,
                            @WebParam(name="jobName")String jobName,
                            @WebParam(name="subjectDBIdentifier")String subjectDBIdentifier,
                            @WebParam(name="queryFastaFileNodeId")String queryFastaFileNodeId,
                            @WebParam(name="eValueExponent")int eValueExponent,
                            @WebParam(name="blastExtensionDropoffBits")int blastExtensionDropoffBits,
                            @WebParam(name="believeDefline")boolean believeDefline,
                            @WebParam(name="showGIsInDeflines")boolean showGIsInDeflines,
                            @WebParam(name="lowercaseFiltering")boolean lowercaseFiltering,
                            @WebParam(name="forceLegacyBlastEngine")boolean forceLegacyBlastEngine,
                            @WebParam(name="filterQueryWithSEG")boolean filterQueryWithSEG,
                            @WebParam(name="gappedAlignmentDropoff")int gappedAlignmentDropoff,
                            @WebParam(name="bitsToTriggerGapping")int bitsToTriggerGapping,
                            @WebParam(name="finalGappedAlignmentDropoff")int finalGappedAlignmentDropoff,
                            @WebParam(name="databaseAlignmentsPerQuery")int databaseAlignmentsPerQuery) throws RemoteException;

    public String runPriam(@WebParam(name = "username") String username,
                              @WebParam(name = "token") String token,
                              @WebParam(name = "project") String project,
                              @WebParam(name = "workSessionId") String workSessionId,
                              @WebParam(name = "jobName") String jobName,
                              @WebParam(name = "inputFastaFileNodeId") String inputFastaFileNodeId,
                              @WebParam(name = "rpsblast_options") String rpsBlastOptions,
                              @WebParam(name = "max_evalue") String maxEvalue) throws RemoteException;

    public String runCddSearch(@WebParam(name = "username") String username,
                                  @WebParam(name = "token") String token,
                                  @WebParam(name = "project") String project,
                                  @WebParam(name = "workSessionId") String workSessionId,
                                  @WebParam(name = "jobName") String jobName,
                                  @WebParam(name = "inputFastaFileNodeId") String inputFastaFileNodeId,
                                  @WebParam(name = "rpsblast_options") String rpsBlastOptions,
                                  @WebParam(name = "max_evalue") String maxEvalue) throws RemoteException;

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
                                         @WebParam(name = "mpiBlastParameters") String mpiBlastParameters) throws RemoteException;

    public String runTophat(@WebParam(name="username") String username,
                            @WebParam(name="token") String token,
                            @WebParam(name="project") String project,
                            @WebParam(name="workSessionId") String workSessionId,
                            @WebParam(name="jobName") String jobName,
                            @WebParam(name="inputFastqReadDirectoryNodeId") String inputFastqReadDirectoryNodeId,
                            @WebParam(name="inputReferenceGenomeFastaFileId") String inputReferenceGenomeFastaFileId) throws RemoteException;

    public String runCufflinks(@WebParam(name="username") String username,
                               @WebParam(name="token") String token,
                               @WebParam(name="project") String project,
                               @WebParam(name="workSessionId") String workSessionId,
                               @WebParam(name="jobName") String jobName,
                               @WebParam(name="inputSamFileNodeId") String inputSamFileNodeId,
                               @WebParam(name="matePairInnerMeanDistance") String matePairInnerMeanDistance,
                               @WebParam(name="matePairStdDevDistance") String matePairStdDevDistance,
                               @WebParam(name="maxIntronLength") String maxIntronLength,
                               @WebParam(name="minIsoformFraction") String minIsoformFraction,
                               @WebParam(name="preMrnaFraction") String preMrnaFraction,
                               @WebParam(name="minMapQual") String minMapQual,
                               @WebParam(name="altLabel") String altLabel,
                               @WebParam(name="optionalGtfFileNodeId") String optionalGtfFileNodeId) throws RemoteException;

    public String runExonerate(@WebParam(name="username") String username,
                                @WebParam(name="token") String token,
                                @WebParam(name="project") String project,
                                @WebParam(name="workSessionId") String workSessionId,
                                @WebParam(name="jobName") String jobName,
                                // Sequence Input Options
                                @WebParam(name="query_fasta_node_id") String query_fasta_node_id,
                                @WebParam(name="target_fasta_node_id") String target_fasta_node_id,
                                @WebParam(name="querytype") String querytype,
                                @WebParam(name="targettype") String targettype,
                                @WebParam(name="querychunkidtype") String querychunkidtype,
                                @WebParam(name="targetchunkidtype") String targetchunkidtype,
                                @WebParam(name="querychunktotaltype") String querychunktotaltype,
                                @WebParam(name="targetchunktotaltype") String targetchunktotaltype,
                                @WebParam(name="verbose") String verbose,
                                // Analysis Options
                                @WebParam(name="exhaustive") String exhaustive,
                                @WebParam(name="bigseq") String bigseq,
                                @WebParam(name="forcescan") String forcescan,
                                @WebParam(name="saturatethreshold") String saturatethreshold,
                                @WebParam(name="customserver") String customserver,
                                // Fasta Database Options
                                @WebParam(name="fastasuffix") String fastasuffix,
                                // Gapped Alignment Options
                                @WebParam(name="model") String model,
                                @WebParam(name="score") String score,
                                @WebParam(name="percent") String percent,
                                @WebParam(name="showalignment") String showalignment,
                                @WebParam(name="showsugar") String showsugar,
                                @WebParam(name="showcigar") String showcigar,
                                @WebParam(name="showvulgar") String showvulgar,
                                @WebParam(name="showquerygff") String showquerygff,
                                @WebParam(name="showtargetgff") String showtargetgff,
                                @WebParam(name="ryo") String ryo,
                                @WebParam(name="bestn") String bestn,
                                @WebParam(name="subopt") String subopt,
                                @WebParam(name="gappedextension") String gappedextension,
                                @WebParam(name="refine") String refine,
                                @WebParam(name="refineboundary") String refineboundary,
                                // Viterbi algorithm options
                                @WebParam(name="dpmemory") String dpmemory,
                                // Code generation options
                                @WebParam(name="compiled") String compiled,
                                // Heuristic Options
                                @WebParam(name="terminalrangeint") String terminalrangeint,
                                @WebParam(name="terminalrangeext") String terminalrangeext,
                                @WebParam(name="joinrangeint") String joinrangeint,
                                @WebParam(name="joinrangeext") String joinrangeext,
                                @WebParam(name="spanrangeint") String spanrangeint,
                                @WebParam(name="spanrangeext") String spanrangeext,
                                // Seeded Dynamic Programming options
                                @WebParam(name="extensionthreshold") String extensionthreshold,
                                @WebParam(name="singlepass") String singlepass,
                                // BSDP algorithm options
                                @WebParam(name="joinfilter") String joinfilter,
                                // Sequence Options
                                @WebParam(name="annotation") String annotation,
                                // Symbol Comparison Options
                                @WebParam(name="softmaskquery") String softmaskquery,
                                @WebParam(name="softmasktarget") String softmasktarget,
                                @WebParam(name="dnasubmat") String dnasubmat,
                                @WebParam(name="proteinsubmat") String proteinsubmat,
                                // Alignment Seeding Options
                                @WebParam(name="fsmmemory") String fsmmemory,
                                @WebParam(name="forcefsm") String forcefsm,
                                @WebParam(name="wordjump") String wordjump,
                                // Affine Model Options
                                @WebParam(name="gapopen") String gapopen,
                                @WebParam(name="gapextend") String gapextend,
                                @WebParam(name="codongapopen") String codongapopen,
                                @WebParam(name="codongapextend") String codongapextend,
                                // NER Model Options
                                @WebParam(name="minner") String minner,
                                @WebParam(name="maxner") String maxner,
                                @WebParam(name="neropen") String neropen,
                                // Intron Modelling Options
                                @WebParam(name="minintron") String minintron,
                                @WebParam(name="maxintron") String maxintron,
                                @WebParam(name="intronpenalty") String intronpenalty,
                                // Frameshift Options
                                @WebParam(name="frameshift") String frameshift,
                                // Alphabet Options
                                @WebParam(name="useaatla") String useaatla,
                                // Translation Options
                                @WebParam(name="geneticcode") String geneticcode,
                                // HSP creation options
                                @WebParam(name="hspfilter") String hspfilter,
                                @WebParam(name="useworddropoff") String useworddropoff,
                                @WebParam(name="seedrepeat") String seedrepeat,
                                @WebParam(name="dnawordlen") String dnawordlen,
                                @WebParam(name="proteinwordlen") String proteinwordlen,
                                @WebParam(name="codonwordlen") String codonwordlen,
                                @WebParam(name="dnahspdropoff") String dnahspdropoff,
                                @WebParam(name="proteinhspdropoff") String proteinhspdropoff,
                                @WebParam(name="codonhspdropoff") String codonhspdropoff,
                                @WebParam(name="dnahspthreshold") String dnahspthreshold,
                                @WebParam(name="proteinhspthreshold") String proteinhspthreshold,
                                @WebParam(name="codonhspthreshold") String codonhspthreshold,
                                @WebParam(name="dnawordlimit") String dnawordlimit,
                                @WebParam(name="proteinwordlimit") String proteinwordlimit,
                                @WebParam(name="codonwordlimit") String codonwordlimit,
                                @WebParam(name="geneseed") String geneseed,
                                @WebParam(name="geneseedrepeat") String geneseedrepeat,
                                // Alignment options
                                @WebParam(name="alignmentwidth") String alignmentwidth,
                                @WebParam(name="forwardcoordinates") String forwardcoordinates,
                                // SAR Options
                                @WebParam(name="quality") String quality,
                                // Splice Site Prediction Options
                                @WebParam(name="splice3") String splice3,
                                @WebParam(name="splice5") String splice5,
                                @WebParam(name="forcegtag") String forcegtag) throws RemoteException;

    public String runSignalp(@WebParam(name="username") String username,
                               @WebParam(name="token") String token,
                               @WebParam(name="project") String project,
                               @WebParam(name="workSessionId") String workSessionId,
                               @WebParam(name="jobName") String jobName,
                               @WebParam(name="fastaInputNodeId") String fastaInputNodeId,
                               @WebParam(name="typeOfOrganism") String typeOfOrganism,
                               @WebParam(name="format") String format,
                               @WebParam(name="method") String method,
                               @WebParam(name="truncateLength") String truncateLength) throws RemoteException;

    public String runEAP(@WebParam(name="username") String username,
                               @WebParam(name="token") String token,
                               @WebParam(name="project") String project,
                               @WebParam(name="workSessionId") String workSessionId,
                               @WebParam(name="jobName") String jobName,
                               @WebParam(name="input_fasta") String input_fasta,
                               @WebParam(name="dataset_name") String dataset_name,
                               @WebParam(name="dataset_description") String dataset_description,
                               @WebParam(name="attribute_list") String attribute_list,
                               @WebParam(name="configuration_db") String configuration_db,
                               @WebParam(name="query_aliases") String query_aliases,
                               @WebParam(name="max_retries") String max_retries,
                               @WebParam(name="compute_list") String compute_list,
                               @WebParam(name="jacs_wsdl_url") String jacs_wsdl_url,
                               @WebParam(name="gzip") String gzip) throws RemoteException;

     public String runSiftProteinSubstitution(@WebParam(name="username") String username,
                               @WebParam(name="token") String token,
                               @WebParam(name="project") String project,
                               @WebParam(name="workSessionId") String workSessionId,
                               @WebParam(name="jobName") String jobName,
                               @WebParam(name="fastaInputNodeId") String fastaInputNodeId,
                               @WebParam(name="substitutionString") String substitutionString) throws RemoteException;

    public String runEvidenceModeler(@WebParam(name="username") String username,
                                @WebParam(name="token") String token,
                                @WebParam(name="project") String project,
                                @WebParam(name="workSessionId") String workSessionId,
                                @WebParam(name="jobName") String jobName,
                                @WebParam(name="fastaInputNodeId") String fastaInputNodeId,
                                @WebParam(name="weights") String weights,
                                @WebParam(name="gene_predictions") String gene_predictions,
                                @WebParam(name="protein_alignments") String protein_alignments,
                                @WebParam(name="transcript_alignments") String transcript_alignments,
                                @WebParam(name="repeats") String repeats,
                                @WebParam(name="terminalExons") String terminalExons,
                                @WebParam(name="stitch_ends") String stitch_ends,
                                @WebParam(name="extend_to_terminal") String extend_to_terminal,
                                @WebParam(name="stop_codons") String stop_codons,
                                @WebParam(name="min_intron_length") String min_intron_length,
                                @WebParam(name="INTERGENIC_SCORE_ADJUST_FACTOR") String INTERGENIC_SCORE_ADJUST_FACTOR,
                                @WebParam(name="exec_dir") String exec_dir,
                                @WebParam(name="forwardStrandOnly") String forwardStrandOnly,
                                @WebParam(name="reverseStrandOnly") String reverseStrandOnly,
                                @WebParam(name="verbose") String verbose,
                                @WebParam(name="debug") String debug,
                                @WebParam(name="report_ELM") String report_ELM,
                                @WebParam(name="RECURSE") String RECURSE,
                                @WebParam(name="limit_range_lend") String limit_range_lend,
                                @WebParam(name="limit_range_rend") String limit_range_rend,
                                @WebParam(name="segmentSize") String segmentSize,
                                @WebParam(name="overlapSize") String overlapSize) throws RemoteException;


    public String runAugustus( @WebParam(name="username") String username,
                               @WebParam(name="token") String token,
                               @WebParam(name="project") String project,
                               @WebParam(name="workSessionId") String workSessionId,
                               @WebParam(name="jobName") String jobName,
                               @WebParam(name="fastaInputNodeId") String fastaInputNodeId,
                               @WebParam(name="species") String species,
                               @WebParam(name="strand") String strand,
                               @WebParam(name="geneModel") String geneModel,
                               @WebParam(name="singleStrand") String singleStrand,
                               @WebParam(name="hintsFile") String hintsFile,
                               @WebParam(name="augustusConfigPath") String augustusConfigPath,
                               @WebParam(name="alternativesFromEvidence") String alternativesFromEvidence,
                               @WebParam(name="alternativesFromSampling") String alternativesFromSampling,
                               @WebParam(name="sample") String sample,
                               @WebParam(name="minExonIntronProb") String minExonIntronProb,
                               @WebParam(name="minMeanExonIntronProb") String minMeanExonIntronProb,
                               @WebParam(name="maxTracks") String maxTracks,
                               @WebParam(name="progress") String progress,
                               @WebParam(name="gff3") String gff3,
                               @WebParam(name="predictionStart") String predictionStart,
                               @WebParam(name="predictionEnd") String predictionEnd,
                               @WebParam(name="UTR") String UTR,
                               @WebParam(name="noInFrameStop") String noInFrameStop,
                               @WebParam(name="noPrediction") String noPrediction,
                               @WebParam(name="uniqueGeneId") String uniqueGeneId) throws RemoteException;

    public String runTrf(      @WebParam(name="username") String username,
                               @WebParam(name="token") String token,
                               @WebParam(name="project") String project,
                               @WebParam(name="workSessionId") String workSessionId,
                               @WebParam(name="jobName") String jobName,
                               @WebParam(name="fastaInputNodeId") String fastaInputNodeId,
                               @WebParam(name="matchingWeight") String matchingWeight,
                               @WebParam(name="mismatching_penalty") String mismatching_penalty,
                               @WebParam(name="indel_penalty") String indel_penalty,
                               @WebParam(name="match_probability") String match_probability,
                               @WebParam(name="indel_probability") String indel_probability,
                               @WebParam(name="minscore") String minscore,
                               @WebParam(name="maxperiod") String maxperiod,
                               @WebParam(name="masked_sequence_file") String masked_sequence_file,
                               @WebParam(name="flanking_sequence") String flanking_sequence,
                               @WebParam(name="data_file") String data_file,
                               @WebParam(name="suppress_html_input") String suppress_html_input) throws RemoteException;

    public String runGenezilla(@WebParam(name="username") String username,
                               @WebParam(name="token") String token,
                               @WebParam(name="project") String project,
                               @WebParam(name="workSessionId") String workSessionId,
                               @WebParam(name="jobName") String jobName,
                               @WebParam(name="fastaInputNodeId") String fastaInputNodeId,
                               @WebParam(name="isoInputNodeId") String isoInputNodeId,
                               @WebParam(name="cpgIslandPredictionFile") String cpgIslandPredictionFile,
                               @WebParam(name="isochorePredictionFile") String isochorePredictionFile,
                               @WebParam(name="ignoreShortFasta") String ignoreShortFasta) throws RemoteException;

    public String runRepeatMasker(@WebParam(name="username") String username,
                               @WebParam(name="token") String token,
                               @WebParam(name="project") String project,
                               @WebParam(name="workSessionId") String workSessionId,
                               @WebParam(name="jobName") String jobName,
                               @WebParam(name="fastaInputNodeId") String fastaInputNodeId,
                               @WebParam(name="nolow") String nolow,
                               @WebParam(name="noint") String noint,
                               @WebParam(name="norna") String norna,
                               @WebParam(name="alu") String alu,
                               @WebParam(name="div") String div,
                               @WebParam(name="lib") String lib,
                               @WebParam(name="cutoff") String cutoff,
                               @WebParam(name="species") String species,
                               @WebParam(name="is_only") String is_only,
                               @WebParam(name="is_clip") String is_clip,
                               @WebParam(name="no_is") String no_is,
                               @WebParam(name="rodspec") String rodspec,
                               @WebParam(name="primspec") String primspec,
                               @WebParam(name="wublast") String wublast,
                               @WebParam(name="s") String s,
                               @WebParam(name="q") String q,
                               @WebParam(name="qq") String qq,
                               @WebParam(name="gc") String gc,
                               @WebParam(name="gccalc") String gccalc,
                               @WebParam(name="frag") String frag,
                               @WebParam(name="maxsize") String maxsize,
                               @WebParam(name="nocut") String nocut,
                               @WebParam(name="noisy") String noisy,
                               @WebParam(name="ali") String ali,
                               @WebParam(name="inv") String inv,
                               @WebParam(name="cut") String cut,
                               @WebParam(name="small") String small,
                               @WebParam(name="xsmall") String xsmall,
                               @WebParam(name="x") String x,
                               @WebParam(name="poly") String poly,
                               @WebParam(name="ace") String ace,
                               @WebParam(name="gff") String gff,
                               @WebParam(name="u") String u,
                               @WebParam(name="xm") String xm,
                               @WebParam(name="fixed") String fixed,
                               @WebParam(name="no_id") String no_id,
                               @WebParam(name="excln") String excln) throws RemoteException;

    public String runFgenesh(@WebParam(name="username") String username,
                               @WebParam(name="token") String token,
                               @WebParam(name="project") String project,
                               @WebParam(name="workSessionId") String workSessionId,
                               @WebParam(name="jobName") String jobName,
                               @WebParam(name="fastaInputNodeId") String fastaInputNodeId,
                               @WebParam(name="par_file") String par_file,
                               @WebParam(name="GC_cutoff") String GC_cutoff,
                               @WebParam(name="position_1") String position_1,
                               @WebParam(name="position_2") String position_2,
                               @WebParam(name="condensed") String condensed,
                               @WebParam(name="exon_table") String exon_table,
                               @WebParam(name="exon_bonus") String exon_bonus,
                               @WebParam(name="pmrna") String pmrna,
                               @WebParam(name="pexons") String pexons,
                               @WebParam(name="min_thr") String min_thr,
                               @WebParam(name="scp_prom") String scp_prom,
                               @WebParam(name="scp_term") String scp_term,
                               @WebParam(name="min_f_exon") String min_f_exon,
                               @WebParam(name="min_i_exon") String min_i_exon,
                               @WebParam(name="min_t_exon") String min_t_exon,
                               @WebParam(name="min_s_exon") String min_s_exon,
                               @WebParam(name="nvar") String nvar,
                               @WebParam(name="try_best_exons") String try_best_exons,
                               @WebParam(name="try_best_sites") String try_best_sites,
                               @WebParam(name="not_rem") String not_rem,
                               @WebParam(name="vthr") String vthr,
                               @WebParam(name="use_table") String use_table,
                               @WebParam(name="show_table") String show_table) throws RemoteException;
    
    public String runInterProScan(@WebParam(name="username") String username,
                               @WebParam(name="token") String token,
                               @WebParam(name="project") String project,
                               @WebParam(name="workSessionId") String workSessionId,
                               @WebParam(name="jobName") String jobName,
                               @WebParam(name="fastaInputNodeId") String fastaInputNodeId,
                               @WebParam(name="goterms") String goterms,
                               @WebParam(name="iprlookup") String iprlookup,
                               @WebParam(name="format") String format) throws RemoteException;

    public String runPrositeScan(@WebParam(name="username") String username,
                               @WebParam(name="token") String token,
                               @WebParam(name="project") String project,
                               @WebParam(name="workSessionId") String workSessionId,
                               @WebParam(name="jobName") String jobName,
                               @WebParam(name="fastaInputNodeId") String fastaInputNodeId,
                               @WebParam(name="specificEntry") String specificEntry,
                               @WebParam(name="outputFormat") String outputFormat,
                               @WebParam(name="prositeDatabaseFile") String prositeDatabaseFile,
                               @WebParam(name="prositePattern") String prositePattern,
                               @WebParam(name="doNotScanProfiles") String doNotScanProfiles,
                               @WebParam(name="skipUnspecificProfiles") String skipUnspecificProfiles,
                               @WebParam(name="profileCutoffLevel") String profileCutoffLevel,
                               @WebParam(name="maximumXCount") String maximumXCount,
                               @WebParam(name="noGreediness") String noGreediness,
                               @WebParam(name="noOverlaps") String noOverlaps,  
                               @WebParam(name="allowIncludedMatches") String allowIncludedMatches,
                               @WebParam(name="pfsearch") String pfsearch,
                               @WebParam(name="useRawScores") String useRawScores,
                               @WebParam(name="cutoffValue") String cutoffValue) throws RemoteException;
    
   
    public String runTargetp(@WebParam(name="username") String username,
                               @WebParam(name="token") String token,
                               @WebParam(name="project") String project,
                               @WebParam(name="workSessionId") String workSessionId,
                               @WebParam(name="jobName") String jobName,
                               @WebParam(name="fastaInputNodeId") String fastaInputNodeId,
                               @WebParam(name="networkType") String networkType,
                               @WebParam(name="includeCleavage") String includeCleavage,
                               @WebParam(name="chloroplastCutoff") String chloroplastCutoff,
                               @WebParam(name="secretoryCutoff") String secretoryCutoff,
                               @WebParam(name="mitochondrialCutoff") String mitochondrialCutoff,
                               @WebParam(name="otherCutoff") String otherCutoff) throws RemoteException;

    public String runGtfToPasaIntegration(@WebParam(name="username") String username,
                               @WebParam(name="token") String token,
                               @WebParam(name="project") String project,
                               @WebParam(name="workSessionId") String workSessionId,
                               @WebParam(name="jobName") String jobName,
                               @WebParam(name="pasaDatabaseName") String pasaDatabaseName,
                               @WebParam(name="referenceGenomeFastaNodeId") String referenceGenomeFastaNodeId,
                               @WebParam(name="gtfNodeId") String gtfNodeId) throws RemoteException;
    
     public String runRnaSeqPipeline(@WebParam(name="username") String username,
                               @WebParam(name="token") String token,
                               @WebParam(name="project") String project,
                               @WebParam(name="workSessionId") String workSessionId,
                               @WebParam(name="jobName") String jobName,
                               @WebParam(name="pasaDatabaseName") String pasaDatabaseName,
                               @WebParam(name="referenceGenomeFastaNodeId") String referenceGenomeFastaNodeId,
                               @WebParam(name="readMapperName") String readMapperName,
                               @WebParam(name="transcriptAssemblerName") String transcriptAssemblerName,
                               @WebParam(name="inputReadsFastqNodeId") String inputReadsFastqNodeId,
                               @WebParam(name="innerMatePairMeanDistance") String innerMatePairMeanDistance,
                               @WebParam(name="innerMatePairStdDev") String innerMatePairStdDev,
                               @WebParam(name="maxIntronLength") String maxIntronLength,
                               @WebParam(name="minIsoformFraction") String minIsoformFraction,
                               @WebParam(name="preMrnaFraction") String preMrnaFraction,
                               @WebParam(name="minMapQual") String minMapQual) throws RemoteException;
     
    public String runTmhmm(@WebParam(name="username") String username,
                               @WebParam(name="token") String token,
                               @WebParam(name="project") String project,
                               @WebParam(name="workSessionId") String workSessionId,
                               @WebParam(name="jobName") String jobName,
                               @WebParam(name="fastaInputNodeId") String fastaInputNodeId,
                               @WebParam(name="html") String html,
                               @WebParam(name="short") String tmhmmShort, // so named as to avoid a reserved word
                               @WebParam(name="plot") String plot,
                               @WebParam(name="v1") String v1) throws RemoteException;

    public String runJaccard(@WebParam(name="username") String username,
                               @WebParam(name="token") String token,
                               @WebParam(name="project") String project,
                               @WebParam(name="workSessionId") String workSessionId,
                               @WebParam(name="jobName") String jobName,
                               @WebParam(name="input_file_list") String input_file_list,
                               @WebParam(name="bsml_search_list") String bsml_search_list,
                               @WebParam(name="linkscore") String linkscore,
                               @WebParam(name="percent_identity") String percent_identity,
                               @WebParam(name="percent_coverage") String percent_coverage,
                               @WebParam(name="p_value") String p_value,
                               @WebParam(name="maxCogSeqCount") String maxCogSeqCount) throws RemoteException;
   
    public String runJocs(@WebParam(name = "username") String username,
                             @WebParam(name = "token") String token,
                             @WebParam(name = "project") String project,
                             @WebParam(name = "workSessionId") String workSessionId,
                             @WebParam(name = "jobName") String jobName,
                             @WebParam(name="bsmlSearchList") String bsmlSearchList,
                             @WebParam(name="bsmlModelList") String bsmlModelList,
                             @WebParam(name="bsmlJaccardlList") String bsmlJaccardlList,
                             @WebParam(name="pvalcut") String pvalcut,
                             @WebParam(name="coverage_cutoff") String coverageCutoff,
                             @WebParam(name="jaccard_coefficient") String jaccardCoefficient,
                             @WebParam(name="j_cutoff") String jCutoff,
                             @WebParam(name="max_cog_seq_count") String maxCogSeqCount) throws RemoteException;

    public String runLegacy2Bsml(@WebParam(name="username") String username,
                                @WebParam(name="token") String token,
                                @WebParam(name="project") String project,
                                @WebParam(name="workSessionId") String workSessionId,
                                @WebParam(name="jobName") String jobName,
                                @WebParam(name="backup") String backup,
                                @WebParam(name="db_username") String db_username,
                                @WebParam(name="password") String password,
                                @WebParam(name="mode") String mode,
                                @WebParam(name="fastadir") String fastadi,
                                @WebParam(name="rdbms") String rdbm,
                                @WebParam(name="host") String host,
                                @WebParam(name="schema") String schema,
                                @WebParam(name="no_misc_features") String no_misc_features,
                                @WebParam(name="no_repeat_features") String no_repeat_features,
                                @WebParam(name="no_transposon_features") String no_transposon_features,
                                @WebParam(name="no_id_generator") String no_id_generator,
                                @WebParam(name="input_id_mapping_files") String input_id_mapping_files,
                                @WebParam(name="input_id_mapping_directories") String input_id_mapping_directories,
                                @WebParam(name="idgen_identifier_version") String idgen_identifier_version,
                                @WebParam(name="no_die_null_sequences") String no_die_null_sequences,
                                @WebParam(name="sourcename") String sourcename,
                                @WebParam(name="control_file") String control_file,
                                @WebParam(name="root_project") String root_project)throws RemoteException;

    public String runFasta2Bsml(@WebParam(name="username") String username,
                                @WebParam(name="token") String token,
                                @WebParam(name="project") String project,
                                @WebParam(name="workSessionId") String workSessionId,
                                @WebParam(name="jobName") String jobName,
                                @WebParam(name="fasta_input") String fasta_input,
                                @WebParam(name="fasta_list") String fasta_list,
                                @WebParam(name="format") String format,
                                @WebParam(name="type_class") String type_class,
                                @WebParam(name="organism") String organism,
                                @WebParam(name="genus") String genus,
                                @WebParam(name="species") String species) throws RemoteException;

     public String runClustalw2(@WebParam(name="username") String username,
                               @WebParam(name="token") String token,
                               @WebParam(name="project") String project,
                               @WebParam(name="workSessionId") String workSessionId,
                               @WebParam(name="jobName") String jobName,
                               @WebParam(name="fastaInputNodeId") String fastaInputNodeId,
                               @WebParam(name="fastaInputFileList") String fastaInputFileList,
                               @WebParam(name="align") String align,
                               @WebParam(name="tree") String tree,
                               @WebParam(name="bootstrap") String bootstrap,
                               @WebParam(name="conver") String convert,

                               // General Settings Parameters
                               @WebParam(name="quicktree") String quicktree,
                               @WebParam(name="type") String type,
                               @WebParam(name="negative") String negative,
                               @WebParam(name="output") String output,
                               @WebParam(name="outorder") String outorder,
                               @WebParam(name="clustal_case") String clustal_case,
                               @WebParam(name="seqnos") String seqnos,
                               @WebParam(name="seqno_range") String seqno_range,
                               @WebParam(name="range") String range,
                               @WebParam(name="maxseqlen") String maxseqlen,
                               @WebParam(name="quiet") String quiet,
                               @WebParam(name="stats") String stats,

                               // Fast Pairwaise Alignments Parameters
                               @WebParam(name="ktuple") String ktuple,
                               @WebParam(name="topdiags") String topdiags,
                               @WebParam(name="window") String window,
                               @WebParam(name="pairgap") String pairgap,
                               @WebParam(name="score") String score,

                               // Slow Pairwise Alignments Parameters
                               @WebParam(name="pwmatrix") String pwmatrix,
                               @WebParam(name="pwdnamatrix") String pwdnamatrix,
                               @WebParam(name="pwgapopen") String pwgapopen,
                               @WebParam(name="pwgapext") String pwgapext,

                               // Multiple Alignments Parameters
                               @WebParam(name="newtree") String newtree,
                               @WebParam(name="usetree") String usetree,
                               @WebParam(name="matrix") String matrix,
                               @WebParam(name="dnamatrix") String dnamatrix,
                               @WebParam(name="gapopen") String gapopen,
                               @WebParam(name="gapext") String gapext,
                               @WebParam(name="endgaps") String endgaps,
                               @WebParam(name="gapdist") String gapdist,
                               @WebParam(name="nopgap") String nopgap,
                               @WebParam(name="nohgap") String nohgap,
                               @WebParam(name="hgapresidues") String hgapresidues,
                               @WebParam(name="maxdiv") String maxdiv,
                               @WebParam(name="transweight") String transweight,
                               @WebParam(name="iteration") String iteration,
                               @WebParam(name="numiter") String numiter,
                               @WebParam(name="noweights") String noweights,

                               // Profile Alignments Paramaters
                               @WebParam(name="profile") String profile,
                               @WebParam(name="newtree1") String newtree1,
                               @WebParam(name="usetree1") String usetree1,
                               @WebParam(name="newtree2") String newtree2,
                               @WebParam(name="usetree2") String usetree2,

                               // Sequence to Profile Alignments Parameters
                               @WebParam(name="sequences") String sequences,

                               // Structure Alignments Parameters
                               @WebParam(name="nosecstr1") String nosecstr1,
                               @WebParam(name="nosecstr2") String nosecstr2,
                               @WebParam(name="secstrout") String secstrout,
                               @WebParam(name="helixgap") String helixgap,
                               @WebParam(name="strandgap") String strandgap,
                               @WebParam(name="loopgap") String loopgap,
                               @WebParam(name="terminalgap") String terminalgap,
                               @WebParam(name="helixendin") String helixendin,
                               @WebParam(name="helixendout") String helixendout,
                               @WebParam(name="strandendin") String strandendin,
                               @WebParam(name="strandendou") String strandendout,

                               //TREES Parameters
                               @WebParam(name="outputtree") String outputtree,
                               @WebParam(name="seed") String seed,
                               @WebParam(name="kimura") String kimura,
                               @WebParam(name="tossgaps") String tossgaps,
                               @WebParam(name="bootlabels") String bootlabels,
                               @WebParam(name="clustering") String clustering,
                               @WebParam(name="batch") String batch) throws RemoteException;  
    public String getBlastDatabaseLocations(@WebParam(name="username")String username,
                            @WebParam(name="token")String token) throws RemoteException;
    public String getHmmerDatabaseLocations(@WebParam(name="username")String username,
                            @WebParam(name="token")String token) throws RemoteException;
    public String getReversePsiBlastDatabaseLocations(@WebParam(name="username")String username,
                            @WebParam(name="token")String token) throws RemoteException;

    public String getBlastStatus(@WebParam(name="username")String username,
                                 @WebParam(name="token")String token,
                                 @WebParam(name="taskId")String taskId) throws RemoteException;

    public String getTaskStatus(@WebParam(name="username")String username,
                                @WebParam(name="token")String token,
                                @WebParam(name="taskId")String taskId) throws RemoteException;

    public String getTaskResultNode (@WebParam(name="token")String token,
                                @WebParam(name="taskId")String taskId) throws RemoteException;

    public String getBlastDatabaseStatus(@WebParam(name="username")String username,
                                 @WebParam(name="token")String token,
                                 @WebParam(name="taskId")String taskId) throws RemoteException;

    public String getHmmpfamStatus(@WebParam(name="username")String username,
                                   @WebParam(name="token")String token,
                                   @WebParam(name="taskId")String taskId) throws RemoteException;

    public String getTrnaScanStatus(@WebParam(name="username")String username,
                                    @WebParam(name="token")String token,
                                    @WebParam(name="taskId")String taskId) throws RemoteException;

    public String getRrnaScanStatus(@WebParam(name="username")String username,
                                    @WebParam(name="token")String token,
                                    @WebParam(name="taskId")String taskId) throws RemoteException;

    public String getSimpleOrfCallerStatus(@WebParam(name="username")String username,
                                           @WebParam(name="token")String token,
                                           @WebParam(name="taskId")String taskId) throws RemoteException;

    public String getMetaGenoOrfCallerStatus(@WebParam(name="username")String username,
                                             @WebParam(name="token")String token,
                                             @WebParam(name="taskId")String taskId) throws RemoteException;

    public String getMetaGenoAnnotationStatus(@WebParam(name="username")String username,
                                              @WebParam(name="token")String token,
                                              @WebParam(name="taskId")String taskId) throws RemoteException;

    public String getMetaGenoCombinedOrfAnnoStatus(@WebParam(name="username")String username,
                                             @WebParam(name="token")String token,
                                             @WebParam(name="taskId")String taskId) throws RemoteException;

    public String getMetageneStatus(@WebParam(name="username")String username,
                                    @WebParam(name="token")String token,
                                    @WebParam(name="taskId")String taskId) throws RemoteException;

    public String getPriamStatus(@WebParam(name = "username") String username,
                                 @WebParam(name = "token") String token,
                                 @WebParam(name = "taskId") String taskId) throws RemoteException;

    public String getTeragridSimpleBlastStatus(@WebParam(name = "username") String username,
                                 @WebParam(name = "token") String token,
                                 @WebParam(name = "taskId") String taskId) throws RemoteException;

    public String getTophatStatus(@WebParam(name="username") String username,
                                  @WebParam(name="token") String token,
                                  @WebParam(name="taskId") String taskId) throws RemoteException;

    public String getCufflinksStatus(@WebParam(name="username") String username,
                                     @WebParam(name="token") String token,
                                     @WebParam(name="taskId") String taskId) throws RemoteException;

    public String uploadFastaFileToSystem(@WebParam(name="username")String username,
                                          @WebParam(name="token")String token,
                                          @WebParam(name="workSessionId")String workSessionId,
                                          @WebParam(name="pathToFastaFile")String pathToFastaFile) throws RemoteException;

    public String uploadRnaSeqReferenceGenomeToSystem(@WebParam(name="username")String username,
                                          @WebParam(name="token")String token,
                                          @WebParam(name="workSessionId")String workSessionId,
                                          @WebParam(name="pathToFastaFile")String pathToFastaFile) throws RemoteException;

    public String uploadFastqDirectoryToSystem(@WebParam(name="username")String username,
                                               @WebParam(name="token")String token,
                                               @WebParam(name="workSessionId")String workSessionId,
                                               @WebParam(name="mateMeanInnerDistance") String mateMeanInnerDistance,
                                               @WebParam(name="pathToFastqDirectory")String pathToFastqDirectory) throws RemoteException;

    public String getFastqUploadStatus(@WebParam(name="username")String username,
                                       @WebParam(name="token")String token,
                                       @WebParam(name="taskId")String taskId) throws RemoteException;

    public String uploadRnaSeqGenomeReferenceToSystem(@WebParam(name = "username") String username,
                                               @WebParam(name = "token") String token,
                                               @WebParam(name = "workSessionId") String workSessionId,
                                               @WebParam(name = "pathToGenomeReferenceFile") String pathToGenomeReferenceFile) throws RemoteException;

    public String getRnaSeqGenomeReferenceUploadStatus(@WebParam(name = "username") String username,
                                       @WebParam(name = "token") String token,
                                       @WebParam(name = "taskId") String taskId) throws RemoteException;

    public String uploadSamFileToSystem(@WebParam(name="username")String username,
                                        @WebParam(name="token")String token,
                                        @WebParam(name="workSessionId")String workSessionId,
                                        @WebParam(name="pathToSamFile")String pathToSamFile) throws RemoteException;

    public String getSamUploadStatus(@WebParam(name="username")String username,
                                     @WebParam(name="token")String token,
                                     @WebParam(name="taskId")String taskId) throws RemoteException;

    public String uploadGtfFileToSystem(@WebParam(name="username")String username,
                                        @WebParam(name="token")String token,
                                        @WebParam(name="workSessionId")String workSessionId,
                                        @WebParam(name="pathToGtfFile")String pathToGtfFile) throws RemoteException;

    public String getGtfUploadStatus(@WebParam(name="username")String username,
                                     @WebParam(name="token")String token,
                                     @WebParam(name="taskId")String taskId) throws RemoteException;
    
    public String uploadGenezillaIsoFileToSystem(@WebParam(name="username")String username,
                                        @WebParam(name="token")String token,
                                        @WebParam(name="workSessionId")String workSessionId,
                                        @WebParam(name="pathToGenezillaIsoFile")String pathToGenezillaIsoFile) throws RemoteException;

    public String getGenezillaIsoUploadStatus(@WebParam(name="username")String username,
                                     @WebParam(name="token")String token,
                                     @WebParam(name="taskId")String taskId) throws RemoteException;

    public String uploadAndFormatBlastDataset(@WebParam(name="username")String username,
                                              @WebParam(name="token")String token,
                                              @WebParam(name="workSessionId")String workSessionId,
                                              @WebParam(name="blastDBName")String blastDBName,
                                              @WebParam(name="blastDBDescription")String blastDBDescription,
                                              @WebParam(name="pathToFastaFile")String pathToFastaFile) throws RemoteException;

    public String getSystemDatabaseIdByName(@WebParam(name="databaseName")String databaseName) throws RemoteException;

    public String deleteTaskById(@WebParam(name="username")String username,
                                 @WebParam(name="token")String token,
                                 @WebParam(name="taskId")String taskId) throws RemoteException;

    public String persistMgAnnoSqlByNodeId(@WebParam(name="username")String username,
                                           @WebParam(name="token")String token,
                                           @WebParam(name="project")String project,
                                           @WebParam(name="workSessionId")String workSessionId,
                                           @WebParam(name="resultNodeId")String resultNodeId) throws RemoteException;

    public String persistMgOrfSqlByNodeId(@WebParam(name="username")String username,
                                          @WebParam(name="token")String token,
                                          @WebParam(name="project")String project,
                                          @WebParam(name="workSessionId")String workSessionId,
                                          @WebParam(name="resultNodeId")String resultNodeId) throws RemoteException;
    
    public String run16sAnalysis(@WebParam(name="username")String username,
                                 @WebParam(name="token")String token,
                                 @WebParam(name="project")String project,
                                 @WebParam(name="jobName")String jobName,
                                 @WebParam(name="pathToInputFile")String pathToInputFile,
                                 @WebParam(name="pathToQualFile")String pathToQualFile,
                                 @WebParam(name="referenceDataset")String referenceDataset,
                                 @WebParam(name="ampliconSize")int ampliconSize,
                                 @WebParam(name="primer1Defline")String primer1Defline,
                                 @WebParam(name="primer1Sequence")String primer1Sequence,
                                 @WebParam(name="primer2Defline")String primer2Defline,
                                 @WebParam(name="primer2Sequence")String primer2Sequence,
                                 @WebParam(name="readLengthMinimum")int readLengthMinimum,
                                 @WebParam(name="minAvgQualityValue")int minAvgQualityValue,
                                 @WebParam(name="maxNCountInARead")int maxNCountInARead,
                                 @WebParam(name="minIdentityCountIn16sHit")int minIdentityCountIn16sHit,
                                 @WebParam(name="filenamePrefix")String filenamePrefix,
                                 @WebParam(name="useMsuRdpClassifier")String useMsuRdpClassifier,
                                 @WebParam(name="iterateCdHitEstClustering")String iterateCdHitEstClustering,
                                 @WebParam(name="skipClustalWStep")String skipClustalWStep);

//    public String uploadAndFormatMpiBlastDataset(@WebParam(name="username")String username,
//                                              @WebParam(name="token")String token,
//                                              @WebParam(name="project") String project,
//                                              @WebParam(name="workSessionId")String workSessionId,
//                                              @WebParam(name="blastDBName")String blastDBName,
//                                              @WebParam(name="blastDBDescription")String blastDBDescription,
//                                              @WebParam(name="pathToFastaFile")String pathToFastaFile,
//                                              @WebParam(name="numFrags")String numFrags) throws RemoteException;

    public String genericServiceDefinition(@WebParam(name="username")String username,
                                           @WebParam(name="token")String token,
                                           @WebParam(name="workSessionId")String workSessionId,
                                           @WebParam(name="name")String name,
                                           @WebParam(name="initialization")String preprocessor,
                                           @WebParam(name="execution")String splitter,
                                           @WebParam(name="finalization")String iterator,
                                           @WebParam(name="readme")String readme);

    public String genericServiceHelp(@WebParam(name="serviceName")String serviceName);

    public String genericService(@WebParam(name="username")String username,
                                 @WebParam(name="token")String token,
                                 @WebParam(name="project")String project,
                                 @WebParam(name="workSessionId")String workSessionId,
                                 @WebParam(name="jobName")String jobName,
                                 @WebParam(name="serviceName")String serviceName,
                                 @WebParam(name="serviceOptions")String serviceOptions,
                                 @WebParam(name="gridOptions")String gridOptions);

    public String getBlastDBInfo(@WebParam(name="blastdb")String blastdb);
    public String runInspect(@WebParam(name="username")String username,
                             @WebParam(name="token")String token,
                             @WebParam(name="project")String project,
                             @WebParam(name="workSessionId")String workSessionId,
                             @WebParam(name="jobName")String jobName,
                             @WebParam(name="archiveFilePath")String archiveFilePath) throws RemoteException;
    public String deleteBlastDatabase(@WebParam(name="username")String username,
                                      @WebParam(name="token")String token,
                                      @WebParam(name="blastDbNodeId")String blastDbNodeId) throws RemoteException;
    public String deleteNodeId(@WebParam(name="username")String username,
                               @WebParam(name="token")String token,
                               @WebParam(name="nodeId")String nodeId) throws RemoteException;

    public String updateGenomeProject(@WebParam(name="username")String username,
                             @WebParam(name="token")String token,
                             @WebParam(name="project")String project,
                             @WebParam(name="workSessionId")String workSessionId,
                             @WebParam(name="jobName")String jobName,
                             @WebParam(name="projectMode")String projectMode,
                             @WebParam(name="genomeProjectStatus")String genomeProjectStatus) throws RemoteException;
}
