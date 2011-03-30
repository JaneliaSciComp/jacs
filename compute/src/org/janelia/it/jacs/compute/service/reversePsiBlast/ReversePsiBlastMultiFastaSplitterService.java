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

package org.janelia.it.jacs.compute.service.reversePsiBlast;

import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.file.FileServiceConstants;
import org.janelia.it.jacs.compute.service.common.file.LockLessMultiFastaSplitterService;
import org.janelia.it.jacs.compute.service.common.file.PartitionList;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.psiBlast.ReversePsiBlastTask;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.reversePsiBlast.ReversePsiBlastDatabaseNode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Oct 29, 2008
 * Time: 4:36:13 PM
 */
public class ReversePsiBlastMultiFastaSplitterService extends LockLessMultiFastaSplitterService {
    private ReversePsiBlastTask rpsblastTask;
    protected FileNode resultFileNode;
    private static final int MAX_BLAST_OUTPUT_FILE_SIZE = (SystemConfigurationProperties.getInt("RpsBlast.MaxOutputFileSizeMB") * 1000000);
    private static final int MAX_QUERIES_PER_EXEC = SystemConfigurationProperties.getInt("RpsBlast.MaxQueriesPerExec");
    private static final int MAX_NUMBER_OF_JOBS = SystemConfigurationProperties.getInt("RpsBlast.MaxNumberOfJobs");
    private static final int XML_JUNK_SIZE_IN_OUTPUT = 1000;
    private static final int MATCH_PLUS_BONDS_PLUS_MATE = 3;
    private static final int DEFAULT_EXPECTED_ALIGNMENTS_WITH_NO_LIMIT = 100;

    public void execute(IProcessData processData) throws ServiceException {
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            computeDAO = new ComputeDAO(logger);
            this.rpsblastTask = (ReversePsiBlastTask) ProcessDataHelper.getTask(processData);
            this.resultFileNode = ProcessDataHelper.getResultFileNode(processData);
            File inputFile = getInputFileFromTask(rpsblastTask.getParameter(ReversePsiBlastTask.PARAM_query_node_id));
            processData.putItem(FileServiceConstants.INPUT_FILE, inputFile);
            int dbAlignmentsRequested = Integer.valueOf(rpsblastTask.getParameter(ReversePsiBlastTask.PARAM_databaseAlignments));
            if (dbAlignmentsRequested == 0) {
                processData.putItem(FileServiceConstants.MAX_RESULTS_PER_JOB, DEFAULT_EXPECTED_ALIGNMENTS_WITH_NO_LIMIT);
            }
            else {
                processData.putItem(FileServiceConstants.MAX_RESULTS_PER_JOB, dbAlignmentsRequested);
            }
            PartitionList partitionList = getPartitionList();
            processData.putItem(FileServiceConstants.PARTITION_LIST, partitionList);
            processData.putItem(FileServiceConstants.MAX_OUTPUT_SIZE, MAX_BLAST_OUTPUT_FILE_SIZE);
            processData.putItem(FileServiceConstants.MAX_INPUT_ENTRIES_PER_JOB, MAX_QUERIES_PER_EXEC);
            processData.putItem(FileServiceConstants.MAX_NUMBER_OF_JOBS, MAX_NUMBER_OF_JOBS);
            processData.putItem(FileServiceConstants.OUTPUT_ADDITIONAL_SIZE, XML_JUNK_SIZE_IN_OUTPUT);
            processData.putItem(FileServiceConstants.PER_INPUT_ENTRY_SIZE_MULTIPLIER, MATCH_PLUS_BONDS_PLUS_MATE);
            super.execute(processData);
        }
        catch (ServiceException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    /**
     * For rpsblast, the Partition is the name of the library: Pfam, CDD, PRK, etc
     *
     * @return PartitionList object
     * @throws ServiceException - problem executing the service
     */
    private PartitionList getPartitionList() throws ServiceException {
        PartitionList partitionList = new PartitionList();
        Long rpsblastDbNodeId = Long.parseLong(rpsblastTask.getParameter(ReversePsiBlastTask.PARAM_subjectDatabases));
        File dbFile;
        try {
            ReversePsiBlastDatabaseNode tmpDbNode = (ReversePsiBlastDatabaseNode) computeDAO.genericLoad(
                    ReversePsiBlastDatabaseNode.class, rpsblastDbNodeId);
            dbFile = new File(tmpDbNode.getDirectoryPath());
            partitionList.setDatabaseLength(dbFile.length());
            List<File> fileList = new ArrayList<File>();
            fileList.add(dbFile);
            partitionList.setFileList(fileList);
            return partitionList;
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

}