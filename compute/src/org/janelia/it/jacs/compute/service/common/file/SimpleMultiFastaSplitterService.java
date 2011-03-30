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
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.ProcessData;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.shared.node.FastaUtil;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.File;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 24, 2009
 * Time: 2:03:29 PM
 */
public class SimpleMultiFastaSplitterService extends MultiFastaSplitterService {
    private static final int MAX_OUTPUT_SIZE = 1000000000; // 1gb
    private static final int MAX_NUMBER_OF_JOBS = 1000000000; // 1 billion, i.e., unlimited
    private static final int XML_JUNK_SIZE_IN_OUTPUT = 0; // we don't need an output constraint

    public void splitFastaFile(IProcessData processData, File inputFile, int entriesPerFile) throws ServiceException {
        try {
            processData.putItem(FileServiceConstants.INPUT_FILE, inputFile);
            processData.putItem(FileServiceConstants.MAX_RESULTS_PER_JOB, 1); // keep calc dependent on other vars
            processData.putItem(FileServiceConstants.PARTITION_LIST, null); // null should be OK
            processData.putItem(FileServiceConstants.MAX_OUTPUT_SIZE, MAX_OUTPUT_SIZE);
            processData.putItem(FileServiceConstants.MAX_INPUT_ENTRIES_PER_JOB, entriesPerFile);
            processData.putItem(FileServiceConstants.MAX_NUMBER_OF_JOBS, MAX_NUMBER_OF_JOBS);
            processData.putItem(FileServiceConstants.OUTPUT_ADDITIONAL_SIZE, XML_JUNK_SIZE_IN_OUTPUT);
            processData.putItem(FileServiceConstants.PER_INPUT_ENTRY_SIZE_MULTIPLIER, 1); // 1==no adjustment
            super.execute(processData);
        }
        catch (ServiceException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    public static FastaFileNode createFastaFileNode(String user, String name, String description,
                                                    String sourcePath, String type, Logger logger, String sessionName)
            throws Exception {
        if (logger.isInfoEnabled()) logger.info("Starting createFastaFileNode() with source path: " + sourcePath);
        if (!(type.equals(FastaFileNode.PEPTIDE) || type.equals(FastaFileNode.NUCLEOTIDE))) {
            throw new Exception("FastaFileNode type not recognized=" + type);
        }
        File sourceFile = new File(sourcePath);
        long[] sequenceCountAndTotalLength = FastaUtil.findSequenceCountAndTotalLength(sourceFile);
        FastaFileNode ffn = new FastaFileNode(user, null/*Task*/, name, description,
                Node.VISIBILITY_PUBLIC, type, (int) sequenceCountAndTotalLength[0], sessionName);
        ffn.setLength(sequenceCountAndTotalLength[1]);
        ffn = (FastaFileNode) EJBFactory.getRemoteComputeBean().saveOrUpdateNode(ffn);
        File ffnDir = new File(ffn.getDirectoryPath());
        ffnDir.mkdirs();
        String copyCmd = "cp " + sourcePath + " " + ffn.getFastaFilePath();
        if (logger.isInfoEnabled()) logger.info("Executing: " + copyCmd);
        SystemCall call = new SystemCall(logger);
        int exitVal = call.emulateCommandLine(copyCmd, true);
        if (logger.isInfoEnabled()) logger.info("Exit value: " + exitVal);
        return ffn;
    }

    public static synchronized List<File> splitInputFileToList(IProcessData processData, File inputFile, int entriesPerExec, Logger logger) throws Exception {
        Task task = ProcessDataHelper.getTask(processData);
        String sessionName = ProcessDataHelper.getSessionRelativePath(processData);
        SimpleMultiFastaSplitterService splitter = new SimpleMultiFastaSplitterService();
        FastaFileNode ffn = SimpleMultiFastaSplitterService.createFastaFileNode(task.getOwner(), task.getDisplayName() + " fasta file node",
                task.getDisplayName() + " fasta file node", inputFile.getAbsolutePath(), FastaFileNode.NUCLEOTIDE, logger, sessionName);
        File inputFileForSplitter = new File(ffn.getFastaFilePath());
        logger.info("SimpleMultiFastaSplitterService using this path as input file for splitter=" + inputFileForSplitter.getAbsolutePath());
        // We create a tmp process data to make sure we don't conflict with other shared instances of process data
        ProcessData tmpForSplitProcessData = new ProcessData();
        tmpForSplitProcessData.setProcessId(processData.getProcessId());
        splitter.splitFastaFile(tmpForSplitProcessData, inputFileForSplitter, entriesPerExec);
        return (List<File>) tmpForSplitProcessData.getItem(FileServiceConstants.POST_SPLIT_INPUT_FILE_LIST);
    }

}
