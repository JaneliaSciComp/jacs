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
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.List;


/**
 * This service is responsible for splitting a multi fasta file into multiple query files.  This service does not use
 * file locking or other games, but splits the fatsa file into parts inside the blast result directory and the individual
 * pipelines are responsible to clean up afterward.
 *
 * @author Tareq Nabeel
 */
public abstract class LockLessMultiFastaSplitterService implements IService {

    protected Logger logger;

    protected File inputFile;
    protected ComputeDAO computeDAO;
    private int maxResultsPerJob;
    private int maxOutputFileSize;
    private int maxInputEntriesPerJob;
    private int outputAdditionalSize;
    private int perInputEntrySizeMultiplier;
    private FileNode _resultNode;

    public void execute(IProcessData processData) throws ServiceException {
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            this.inputFile = (File) processData.getItem(FileServiceConstants.INPUT_FILE);
            this.maxResultsPerJob = (Integer) processData.getItem(FileServiceConstants.MAX_RESULTS_PER_JOB);
            this.maxOutputFileSize = (Integer) processData.getItem(FileServiceConstants.MAX_OUTPUT_SIZE);
            this.maxInputEntriesPerJob = (Integer) processData.getItem(FileServiceConstants.MAX_INPUT_ENTRIES_PER_JOB);
            this.outputAdditionalSize = (Integer) processData.getItem(FileServiceConstants.OUTPUT_ADDITIONAL_SIZE);
            this.perInputEntrySizeMultiplier = (Integer) processData.getItem(FileServiceConstants.PER_INPUT_ENTRY_SIZE_MULTIPLIER);
            this._resultNode = ProcessDataHelper.getResultFileNode(processData);
            List<File> inputFiles = splitFile(inputFile);
            processData.putItem(FileServiceConstants.POST_SPLIT_INPUT_FILE_LIST, inputFiles);
        }
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    protected File getInputFileFromTask(String queryNodeId) throws ServiceException, IOException, InterruptedException {
        Long inputNodeId = Long.parseLong(queryNodeId);
        File inputFile;
        try {
            FastaFileNode inputNode = (FastaFileNode) computeDAO.genericGet(FastaFileNode.class, inputNodeId);
            if (inputNode == null) {
                logger.info("FastaFileNode with inputNodeId:" + inputNodeId + " does not exist");
                throw new ServiceException("Could not find the FASTA file required.");
            }
            else {
                inputFile = new File(inputNode.getFastaFilePath());
            }
        }
        catch (ClassCastException e) {
            throw new ServiceException(e.getMessage(), e);
        }
        return inputFile;
    }


    /**
     * This method splits the uploaded query file into pieces and deposits them into the blast result directory
     *
     * @param multiFastafile - file to be split
     * @return returns a list of file handles to the query pieces
     * @throws java.io.IOException  - problem with file io
     * @throws org.janelia.it.jacs.compute.engine.service.ServiceException
     *                              - problem with an underlying service tihs calls
     * @throws InterruptedException - interrpution while working on splitting the file
     */
    private List<File> splitFile(File multiFastafile) throws IOException, ServiceException, InterruptedException {
        List<File> splitParts;
        File copiedFastaFile = new File(_resultNode.getDirectoryPath() + File.separator + multiFastafile.getName());
        // Copy the file over before working on it.
        FileUtil.copyFile(multiFastafile, copiedFastaFile);
        // The split directory, which no longer may be re-used, depends on several variables, all of which must be included
        // in its name to ensure consistent behavior. Note that the previous blast-only implementation used the
        // single variable equivalent to 'maxResultsPerJob' because the other vars were hard-coded for blast. For
        // the general case, we must use everything which could effect the split outcome.
        String splitFileDirName = "al-" + maxResultsPerJob + "-" + maxInputEntriesPerJob + "-" + perInputEntrySizeMultiplier + "-" +
                outputAdditionalSize + "-" + maxOutputFileSize;
        File splitFileDir = new File(_resultNode.getDirectoryPath(), splitFileDirName);

        logger.info("Splitting multifasta file... ");
        splitParts = splitFileByNucleotideCount(copiedFastaFile, splitFileDir);
        return splitParts;
    }

    /**
     * Creates the splits based on nucleotide count.  Modify the method splitFile if you only
     * need to change the algorithm upon which the splits is based.
     *
     * @param multiFastafile the original multi-fasta file
     * @param targetDir      directory files will be created into
     * @return returns a list of split files
     * @throws java.io.IOException  there was a problem accessing the files
     * @throws InterruptedException there was a problem with the code actions
     * @throws org.janelia.it.jacs.compute.engine.service.ServiceException
     *                              there was a problem with the service
     */
    private List<File> splitFileByNucleotideCount(File multiFastafile, File targetDir) throws IOException, ServiceException, InterruptedException {
        // The number of nucleotides to be contained in each query file
        List<File> fastaFiles = new ArrayList<File>();
        int totalQueryCount = 0;
        long nucleotideCountInCurrentSplit = 0;
        int queryCountInCurrentSplit = 0;
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(multiFastafile)));
        File queryFile = FileUtil.ensureFileExists(
                targetDir.getAbsolutePath() + File.separator + multiFastafile.getName() + ".q" + (totalQueryCount + 1));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(queryFile)));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(">")) {
                    if (shouldSplitFile(queryCountInCurrentSplit, nucleotideCountInCurrentSplit)) {
                        // We have exeeded the maximum nucleotide count.  Close the previous writer
                        writer.close();
                        // Rename the query file to contain the range of queries (e.g. nucleotide.fasta.q11-q50)
                        renameQueryFileAndWriteSeqCount(queryFile, fastaFiles, totalQueryCount, queryCountInCurrentSplit);
                        // Create the next query file (e.g. nucleotide.fasta.q51)
                        queryFile = FileUtil.ensureFileExists(
                                targetDir.getAbsolutePath() + File.separator + multiFastafile.getName() + ".q" +
                                        (totalQueryCount + 1));
                        // Create the writer for the next query file
                        writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(queryFile)));
                        // Reset the counts in split
                        nucleotideCountInCurrentSplit = 0;
                        queryCountInCurrentSplit = 0;
                    }
                    totalQueryCount++;
                    queryCountInCurrentSplit++;
                }
                else {
                    nucleotideCountInCurrentSplit += line.length();
                }
                writer.write(line);
                writer.newLine();
            }
        }
        finally {
            // This is the last writer.  We need to close it and rename the query file
            writer.close();
            reader.close();
        }
        renameQueryFileAndWriteSeqCount(queryFile, fastaFiles, totalQueryCount, queryCountInCurrentSplit);
        if (logger.isInfoEnabled()) {
            logger.info("Created fasta files " + multiFastafile.getName() + ".[q1-q" + totalQueryCount +
                    "] in " + multiFastafile.getParentFile().getAbsolutePath());
        }
        return fastaFiles;
    }

    /**
     * Renames the query file to contain the range of queries (e.g. nucleotide.fasta.q11-q50)
     * It also adds the newly created query file to the list of fasta file splits we're going to return from this service
     * It also writes the sequence count to the filesystem (approved by Leonid)
     *
     * @param queryFile                The splitted query file containing start range but no end range e.g. nucleotide.fasta.q11
     * @param fastaFiles               the list of splits
     * @param totalQueryCount          the running query total
     * @param queryCountInCurrentSplit the max number of queries in each split file
     * @throws java.io.IOException there was a problem accessing a file
     */
    private void renameQueryFileAndWriteSeqCount(File queryFile, List<File> fastaFiles, int totalQueryCount, int queryCountInCurrentSplit)
            throws IOException, ServiceException {
        File newQueryFile = renameQueryFile(queryFile, totalQueryCount);
        fastaFiles.add(newQueryFile);
        // Writes the sequence count to a file beside the query file. The file name will be the same as the query file
        // name with ".seqCount" appended to it
        PrintWriter pw = new PrintWriter(new FileOutputStream(newQueryFile.getAbsolutePath() + ".seqCount"));
        try {
            pw.println(queryCountInCurrentSplit);
        }
        finally {
            pw.close();
        }
    }

    /**
     * Renames the query file to contain the range of queries (e.g. nucleotide.fasta.q11-q50)
     *
     * @param queryFile       The splitted query file containing start range but no end range e.g. nucleotide.fasta.q11
     * @param totalQueryCount the running query total
     * @return
     * @throws java.io.IOException
     */
    private File renameQueryFile(File queryFile, int totalQueryCount) throws IOException, ServiceException {
        String extensionToAdd = "q" + totalQueryCount;
        File newFile = queryFile;
        if (!queryFile.getName().endsWith(extensionToAdd)) {     // we don't want q21-q21
            newFile = new File(queryFile.getAbsolutePath() + "_" + extensionToAdd);
            boolean renameSuccessful = queryFile.renameTo(newFile);
            if (!renameSuccessful) {
                throw new ServiceException("Unable to rename the query file to contain query range.");
            }
        }
        return newFile;
    }

    // NOTE: if any additional factors are added to this method which could determine split outcome, they must be added to the
    // cached filename determination mechanism in the 'List<File> shouldSplitFile()' method above.
    protected boolean shouldSplitFile(int queryCountInCurrentSplit, long nucleotideCountInCurrentSplit) throws ServiceException {
        if (queryCountInCurrentSplit >= maxInputEntriesPerJob) {
            return true;
        }
        else if (nucleotideCountInCurrentSplit > 0) {
            long outputPerQuerySize = perInputEntrySizeMultiplier * getAvgQueryLength(queryCountInCurrentSplit,
                    nucleotideCountInCurrentSplit) + outputAdditionalSize;
            long approximateBlastOutputFileSize = queryCountInCurrentSplit * outputPerQuerySize * maxResultsPerJob;
            boolean maxBlastOutputExceeded = approximateBlastOutputFileSize > maxOutputFileSize;
            boolean hitsPerQueryTooLow = (calculateActualResultsPerInputEntry(queryCountInCurrentSplit, maxResultsPerJob) < 1); // todo Blast fails because numberOfHitsToKeep ends up being zero
            return maxBlastOutputExceeded || hitsPerQueryTooLow;
        }
        else {
            return false; // first query
        }
    }


    protected long getAvgQueryLength(int queryCountInCurrentSplit, long nucleotideCountInCurrentSplit) {
        return nucleotideCountInCurrentSplit / queryCountInCurrentSplit;
    }

    private int calculateActualResultsPerInputEntry(int numberOfInputEntries, int desiredResultsPerInputEntry) {
        int actualResultsPerInputEntry = desiredResultsPerInputEntry;
        if (maxResultsPerJob > 0) {
            if ((numberOfInputEntries * desiredResultsPerInputEntry) >= maxResultsPerJob) {
                actualResultsPerInputEntry = maxResultsPerJob / numberOfInputEntries;
            }
            if (actualResultsPerInputEntry <= 0)
                actualResultsPerInputEntry = 1;
        }
        return actualResultsPerInputEntry;
    }

}