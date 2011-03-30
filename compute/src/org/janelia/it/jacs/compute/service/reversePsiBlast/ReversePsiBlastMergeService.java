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

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.file.FileServiceConstants;
import org.janelia.it.jacs.compute.service.common.grid.submit.SubmitJobException;
import org.janelia.it.jacs.model.tasks.psiBlast.ReversePsiBlastTask;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.reversePsiBlast.ReversePsiBlastDatabaseNode;
import org.janelia.it.jacs.model.user_data.reversePsiBlast.ReversePsiBlastResultNode;

import java.io.*;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Nov 5, 2008
 * Time: 10:43:26 AM
 */
public class ReversePsiBlastMergeService implements IService {

    private List<File> queryFiles;
    private Map<File, List<File>> inputOutputFileListMap;
    private ReversePsiBlastResultNode resultFileNode;
    protected ComputeDAO computeDAO;
    private String queryName;
    private String databaseName;
    private Logger logger;

    public ReversePsiBlastMergeService() {
    }

    public void execute(IProcessData processData) throws SubmitJobException {
        try {
            init(processData);
            logger.debug("ReversePsiBlastMergeService start");
            mergeResultFiles();
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new SubmitJobException(e);
        }
        logger.debug("ReversePsiBlastMergeService end");
    }

    protected void init(IProcessData processData) throws MissingDataException, IOException {
        logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
        computeDAO = new ComputeDAO(logger);
        queryFiles = (List<File>) processData.getMandatoryItem(FileServiceConstants.POST_SPLIT_INPUT_FILE_LIST);
        ReversePsiBlastTask rpsblastTask = (ReversePsiBlastTask) ProcessDataHelper.getTask(processData);
        inputOutputFileListMap = (Map<File, List<File>>) processData.getMandatoryItem(FileServiceConstants.INPUT_OUTPUT_FILE_LIST_MAP);
        resultFileNode = (ReversePsiBlastResultNode) ProcessDataHelper.getResultFileNode(processData);

        String queryNodeIdString = rpsblastTask.getParameter(ReversePsiBlastTask.PARAM_query_node_id);
        String dbNodeIdString = rpsblastTask.getParameter(ReversePsiBlastTask.PARAM_subjectDatabases);
        Long queryNodeId = new Long(queryNodeIdString);
        Long dbNodeId = new Long(dbNodeIdString);
        try {
            FastaFileNode queryNode = (FastaFileNode) computeDAO.genericLoad(FastaFileNode.class, queryNodeId);
            queryName = getNameFromNode(queryNode);
            ReversePsiBlastDatabaseNode dbNode = (ReversePsiBlastDatabaseNode) computeDAO.genericLoad(ReversePsiBlastDatabaseNode.class, dbNodeId);
            databaseName = getNameFromNode(dbNode);
        }
        catch (Exception e) {
            throw new MissingDataException("Could not obtain queryNode or dbNode information");
        }

    }

    private static String getNameFromNode(Node node) {
        String name = node.getName();
        if (name == null || name.trim().length() == 0) {
            name = node.getDescription();
            if (name == null || name.trim().length() == 0) {
                name = "Node_" + node.getObjectId();
            }
        }
        return name;
    }

    private void mergeResultFiles() throws Exception {
        File resultFile = new File(resultFileNode.getFilePathByTag(ReversePsiBlastResultNode.TAG_TEXT_OUTPUT));
        Writer writer = new BufferedWriter(new FileWriter(resultFile));
        boolean initialFile = true;
        long hitCount = 0L;
        for (File queryFile : queryFiles) {
            List<File> outputFiles = inputOutputFileListMap.get(queryFile);
            if (outputFiles == null)
                throw new Exception("Unexpectedly found no output files in processData hash for query file=" + queryFile.getAbsolutePath());
            for (File outputFile : outputFiles) {
                hitCount += processOutputFile(writer, outputFile, initialFile);
                if (initialFile) initialFile = false;
            }
        }
        writer.close();
        resultFileNode.setHitCount(hitCount);
        computeDAO.saveOrUpdate(resultFileNode);
    }

    private long processOutputFile(Writer writer, File outputFile, boolean initialFile) throws Exception {
        if (!outputFile.exists())
            throw new Exception("Could not find initial output file=" + outputFile.getAbsolutePath());
        if (outputFile.length() == 0L)
            throw new Exception("Initial output file unexpectedly has zero size=" + outputFile.getAbsolutePath());
        BufferedReader reader = new BufferedReader(new FileReader(outputFile));
        long hitCount = 0L;
        try {
            String line;
            int lineCount = 0;
            boolean initDone = false;
            boolean initStarted = false;
            boolean inScoreListMode = false;
            while ((line = reader.readLine()) != null) {
                if (lineCount > 20 && !initDone)
                    throw new Exception("Could not parse beginning of initial output file=" + outputFile.getAbsolutePath());
                if (!initStarted && line.startsWith("HMMER 2.3")) {
                    initStarted = true;
                    if (initialFile)
                        writer.write(line + "\n");
                }
                else if (initStarted && !initDone) {
                    if (initialFile) {
                        if (line.startsWith(("HMM file:"))) {
                            writer.write("HMM file:                 " + databaseName + "\n");
                        }
                        else if (line.startsWith(("Sequence file:"))) {
                            writer.write("Sequence file:            " + queryName + "\n");
                        }
                        else if (line.startsWith(("Query sequence:"))) {
                            initDone = true;
                            writer.write(line + "\n");
                        }
                    }
                    else {
                        if (line.startsWith("Sequence file:")) {
                            initDone = true;
                        }
                    }
                }
                else if (line.startsWith("Scores for sequence family classification")) {
                    if (inScoreListMode) {
                        throw new Exception("Could not parse score section of file=" + outputFile.getAbsolutePath());
                    }
                    inScoreListMode = true;
                    writer.write(line + "\n");
                }
                else if (inScoreListMode) {
                    String[] entryArr = line.split("\\s+");
                    if (entryArr[0].equals("Model") && entryArr[1].equals("Description")) {
                        // OK, keep going
                    }
                    else if (entryArr[0].startsWith("---") && entryArr[1].startsWith("---")) {
                        // OK, still keep going
                    }
                    else if (entryArr.length > 4) {
                        // We will assume this is a score
                        hitCount++;
                    }
                    else {
                        // We must be out of score list
                        inScoreListMode = false;
                    }
                    // We still must write the line no matter what
                    writer.write(line + "\n");
                }
                else {
                    writer.write(line + "\n");
                }
                lineCount++;
            }
        }
        finally {
            reader.close();
        }
        return hitCount;
    }

}