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
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.shared.fasta.FastaFile;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * This service is responsible for splitting a multi fasta file into multiple query files.
 *
 * @author Tareq Nabeel
 */
public class MultiFastaSplitterService implements IService {

    protected Logger logger;

    private File inputFile;
    private int maxResultsPerJob;
    private int maxOutputFileSize;
    private int maxInputEntriesPerJob;
    private int outputAdditionalSize;
    private int perInputEntrySizeMultiplier;

    private static final long MAX_WAIT_FOR_LOCK = 5 * 60 * 1000; // 5 min
    private static final long WAIT_FOR_LOCK_CHECK_INTERVAL = 1000; // one second

    public void execute(IProcessData processData) throws ServiceException {
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            this.inputFile = (File) processData.getItem(FileServiceConstants.INPUT_FILE);
            this.maxResultsPerJob = (Integer) processData.getItem(FileServiceConstants.MAX_RESULTS_PER_JOB);
            this.maxOutputFileSize = (Integer) processData.getItem(FileServiceConstants.MAX_OUTPUT_SIZE);
            this.maxInputEntriesPerJob = (Integer) processData.getItem(FileServiceConstants.MAX_INPUT_ENTRIES_PER_JOB);
            this.outputAdditionalSize = (Integer) processData.getItem(FileServiceConstants.OUTPUT_ADDITIONAL_SIZE);
            this.perInputEntrySizeMultiplier = (Integer) processData.getItem(FileServiceConstants.PER_INPUT_ENTRY_SIZE_MULTIPLIER);
            List<File> inputFiles = getInputFiles();
            processData.putItem(FileServiceConstants.POST_SPLIT_INPUT_FILE_LIST, inputFiles);
        }
        catch (ServiceException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    /**
     * Return the splits as a the list of query files
     *
     * @return returns the splits as the list of query files
     * @throws ServiceException     there was a problem with the service
     * @throws IOException          there was a problem accessing a file
     * @throws InterruptedException there was a problem with the code actions
     */
    private List<File> getInputFiles() throws ServiceException, IOException, InterruptedException {
        return splitFile(inputFile,
                maxResultsPerJob,
                maxInputEntriesPerJob,
                perInputEntrySizeMultiplier,
                outputAdditionalSize,
                maxOutputFileSize,
                logger);
    }

    private static synchronized File aquireSplitServiceLock(String fileName) throws IOException {
        if (FileUtil.fileExists(fileName))
            return null;
        else {
            File lock = new File(fileName);
            Writer writer = new OutputStreamWriter(new FileOutputStream(lock));
            writer.write("busy...");
            writer.flush();
            writer.close();
            return lock;
        }
    }

    private static synchronized void releaseLock(File lock) {
        if (lock != null)
            lock.delete();
    }

    public static synchronized List<File> splitFile(
            File multiFastafile,
            int maxResultsPerJob,
            int maxInputEntriesPerJob,
            int perInputEntrySizeMultiplier,
            int outputAdditionalSize,
            int maxOutputFileSize,
            Logger logger
    ) throws IOException, ServiceException, InterruptedException {
        List<File> splitParts;

        File dataDir = multiFastafile.getParentFile();
        // The split directory, which may be re-used, depends on several variables, all of which must be included
        // in its name to ensure consistent behavior. Note that the previous blast-only implementation used the
        // single variable equivalent to 'maxResultsPerJob' because the other vars were hard-coded for blast. For
        // the general case, we must use everything which could effect the split outcome.
        String splitFileDirName = "al-" + maxResultsPerJob + "-" + maxInputEntriesPerJob + "-" + perInputEntrySizeMultiplier + "-" +
                outputAdditionalSize + "-" + maxOutputFileSize;
        File splitFileDir = new File(dataDir.getAbsolutePath(), splitFileDirName);
        // check if the file has been split before
        String lockFileName = splitFileDir.getAbsolutePath() + "splitting.now";
        File lock = aquireSplitServiceLock(lockFileName);
        if (lock == null) {
            logger.info("Lock returned null, implying lock file already exists - assuming sister process is splitting");
        }
        else {
            logger.info("Returned non-null lock file");
            File splitFileDirCheck = FileUtil.ensureDirExists(dataDir.getAbsolutePath(), splitFileDirName, false);
            if (!splitFileDirCheck.getAbsolutePath().equals(splitFileDir.getAbsolutePath())) {
                throw new ServiceException("SplitFileDir " + splitFileDir.getAbsolutePath() + " does not match SplitFileDirCheck " +
                        splitFileDirCheck.getAbsolutePath());
            }
        }
        String FASTA_PARTITION_EXT = "^q\\d+_q\\d+$";
//        String FASTA_PARTITION_EXT = "^q[0-9]+_q[0-9]+$";

        long maxRetries = MAX_WAIT_FOR_LOCK / WAIT_FOR_LOCK_CHECK_INTERVAL;
        if (maxRetries < 3)
            maxRetries = 3;
        long retry = 0;
        Date waitStartTime = new Date();
        try {
            while (lock == null) // can't aquire
            {
                logger.info("lock is null - waiting to acquire");
                if (retry >= maxRetries) {
                    logger.error("Unable to obtain lock to split input fasta file after max retry count of " + retry);
                    throw new ServiceException("Unable to split user input file");
                }
                Thread.sleep(WAIT_FOR_LOCK_CHECK_INTERVAL);
                logger.info("Trying to acquire lock, try index = " + retry);
                lock = aquireSplitServiceLock(lockFileName);
                retry++;
                if (lock != null) {
                    logger.info("lock is non-null - succeeded in getting lock");
                }
            }

            // obtained lock - see if split is required
            logger.info("MultiFastaSplitterService obtained lock on " + splitFileDir.getAbsolutePath()
                    + " after " + (new Date().getTime() - waitStartTime.getTime()) + " msecs of wait");

            //get a pre-existing file splits
            File[] files = splitFileDir.listFiles(new SplitFastaFilenameFilter(FASTA_PARTITION_EXT));

            if (files.length > 0) {
                StringBuffer names = new StringBuffer();
                splitParts = new ArrayList<File>();
                for (File f : files) {
                    splitParts.add(f);
                    names.append(splitFileDirName).append(File.separator).append(f.getName()).append(" ");
                }
                logger.info("Using previously created fasta files " + names.toString());
            }
            else {
                logger.info("Splitting multifasta file... ");

                splitParts = splitFileByNucleotideCount(multiFastafile,
                        splitFileDir,
                        maxInputEntriesPerJob,
                        maxResultsPerJob,
                        perInputEntrySizeMultiplier,
                        maxOutputFileSize,
                        outputAdditionalSize,
                        logger);
            }

            releaseLock(lock);
        }
        catch (Throwable t) {
            // clean up
            if (lock != null) {
                // delete partitions here
                try {
                    File[] files = dataDir.listFiles(new SplitFastaFilenameFilter(FASTA_PARTITION_EXT));
                    for (File f : files) {
                        f.delete();
                    }
                }
                catch (Throwable t1) {
                    // this is really bad - File system went south...
                }
                releaseLock(lock);
            }
            if (t instanceof IOException)
                throw (IOException) t;
            else if (t instanceof ServiceException)
                throw (ServiceException) t;
            else if (t instanceof InterruptedException)
                throw (InterruptedException) t;
            else {
                logger.error("Unknown ERROR", t);
                throw new ServiceException(t);
            }
        }

        return splitParts;

    }

    /**
     * Creates the splits based on nucleotide count.  Modify the method splitFile if you only
     * need to change the algorithm upon which the splits is based.
     *
     * @param multiFastafile the original multi-fasta file
     * @param targetDir      directory files will be created into
     * @return returns a list of split files
     * @throws IOException          there was a problem accessing the files
     * @throws InterruptedException there was a problem with the code actions
     * @throws ServiceException     there was a problem with the service
     */
    private static synchronized List<File> splitFileByNucleotideCount(File multiFastafile,
                                                                      File targetDir,
                                                                      int maxInputEntriesPerJob,
                                                                      int maxResultsPerJob,
                                                                      int perInputEntrySizeMultiplier,
                                                                      int maxOutputFileSize,
                                                                      int outputAdditionalSize,
                                                                      Logger logger) throws IOException, ServiceException, InterruptedException {
        validateParameters(multiFastafile, logger);
        // The number of nucleotides to be contained in each query file
        List<File> fastaFiles = new ArrayList<File>();
        int totalQueryCount = 0;
        long nucleotideCountInCurrentSplit = 0;
        int queryCountInCurrentSplit = 0;
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(multiFastafile)));
        File queryFile = createQueryFile(multiFastafile, targetDir, totalQueryCount);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(queryFile)));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(">")) {

                    if (splitFile(queryCountInCurrentSplit,
                            nucleotideCountInCurrentSplit,
                            maxInputEntriesPerJob,
                            maxOutputFileSize,
                            maxResultsPerJob,
                            perInputEntrySizeMultiplier,
                            outputAdditionalSize)) {
                        // We have exeeded the maximum nucleotide count.  Close the previous writer
                        writer.close();
                        // Rename the query file to contain the range of queries (e.g. nucleotide.fasta.q11-q50)
                        renameQueryFileAndWriteSeqCount(queryFile, fastaFiles, totalQueryCount, queryCountInCurrentSplit);
                        // Create the next query file (e.g. nucleotide.fasta.q51)
                        queryFile = createQueryFile(multiFastafile, targetDir, totalQueryCount);
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
     * @throws IOException there was a problem accessing a file
     */
    private static synchronized void renameQueryFileAndWriteSeqCount(File queryFile, List<File> fastaFiles, int totalQueryCount, int queryCountInCurrentSplit) throws IOException {
        File newQueryFile = renameQueryFile(queryFile, totalQueryCount);
        fastaFiles.add(newQueryFile);
        writeSeqCount(newQueryFile, queryCountInCurrentSplit);
    }

    /**
     * Renames the query file to contain the range of queries (e.g. nucleotide.fasta.q11-q50)
     *
     * @param queryFile       The splitted query file containing start range but no end range e.g. nucleotide.fasta.q11
     * @param totalQueryCount the running query total
     * @return
     * @throws IOException
     */
    private static synchronized File renameQueryFile(File queryFile, int totalQueryCount) throws IOException {
        String extensionToAdd = "q" + totalQueryCount;
        File newFile = queryFile;
        if (!queryFile.getName().endsWith(extensionToAdd)) {     // we don't want q21-q21
            newFile = new File(queryFile.getAbsolutePath() + "_" + extensionToAdd);
            queryFile.renameTo(newFile);
        }
        return newFile;
    }

    /**
     * Writes the sequence count to a file beside the query file. The file name will be the same as the query file
     * name with ".seqCount" appended to it
     *
     * @param newQueryFile
     * @param queryCountInCurrentSplit
     * @throws IOException
     */
    private static synchronized void writeSeqCount(File newQueryFile, int queryCountInCurrentSplit) throws IOException {
        PrintWriter pw = new PrintWriter(new FileOutputStream(newQueryFile.getAbsolutePath() + ".seqCount"));
        try {
            pw.println(queryCountInCurrentSplit);
        }
        finally {
            pw.close();
        }
    }

    private static synchronized File createQueryFile(File fastaFile, File targetDir, int queryFileIndex) throws IOException {
        return FileUtil.ensureFileExists(
                targetDir.getAbsolutePath() + File.separator + fastaFile.getName() + ".q" + (queryFileIndex + 1));
    }


    // NOTE: if any additional factors are added to this method which could determine split outcome, they must be added to the
    // cached filename determination mechanism in the 'List<File> splitFile()' method above.
    private static synchronized boolean splitFile(int queryCountInCurrentSplit,
                                                  long nucleotideCountInCurrentSplit,
                                                  int maxInputEntriesPerJob,
                                                  int maxOutputFileSize,
                                                  int maxResultsPerJob,
                                                  int perInputEntrySizeMultiplier,
                                                  int outputAdditionalSize) throws ServiceException {
        if (queryCountInCurrentSplit >= maxInputEntriesPerJob) {
            return true;
        }
        else if (nucleotideCountInCurrentSplit > 0) {
            long outputPerQuerySize = perInputEntrySizeMultiplier * getAvgQueryLength(queryCountInCurrentSplit,
                    nucleotideCountInCurrentSplit) + outputAdditionalSize;
            long approximateBlastOutputFileSize = queryCountInCurrentSplit * outputPerQuerySize * maxResultsPerJob;
            boolean maxBlastOutputExceeded = approximateBlastOutputFileSize > maxOutputFileSize;
            boolean hitsPerQueryTooLow = isHitsPerQueryTooLow(queryCountInCurrentSplit, maxResultsPerJob); // todo Blast fails because numberOfHitsToKeep ends up being zero
            return maxBlastOutputExceeded || hitsPerQueryTooLow;
        }
        else {
            return false; // first query
        }
    }


    private static synchronized void validateParameters(File multiFastafile, Logger logger) throws IOException, InterruptedException, ServiceException {
        FastaFile multiFastaHelper = new FastaFile(multiFastafile);
        int totalQueryCount = (int) multiFastaHelper.getSize().getEntries();
        if (1 == totalQueryCount) {
            logger.debug("In MultiFastaSplitterService:validateParameters(), totalQueryCount=1 so letting it ride...");
//            return;
        }
//        int totalNucleotideCount = (int) multiFastaHelper.getSize().getBases();
//        float avgQueryLength = totalNucleotideCount / totalQueryCount;
//        float avgOutputSizePerQuery = perInputEntrySizeMultiplier * avgQueryLength + outputAdditionalSize;
//        float avgQueryCountInSplit = maxOutputFileSize / (avgOutputSizePerQuery * maxResultsPerJob);
//        float totalSplits = totalQueryCount / avgQueryCountInSplit;
//        float totalSplitsBasedOnQueriesOnly = totalQueryCount / maxInputEntriesPerJob;
//        if (totalSplitsBasedOnQueriesOnly > totalSplits) totalSplits=totalSplitsBasedOnQueriesOnly;
//        float totalGridJobs = totalSplits * partitionListSize;
//        if (totalGridJobs > maxNumberOfJobs) {
//            if (totalSplitsBasedOnQueriesOnly == totalSplits) {
//                int maxQueries = maxInputEntriesPerJob * partitionListSize;
//                throw new ServiceException("Requested number of queries cannot exceed " + maxQueries + " for dbAlignments value=" +
//                        maxResultsPerJob + ". The number of queries or dbAlignment value must be reduced");
//            } else {
//                int maxDbAlignments = (int) ((float) maxOutputFileSize / (avgOutputSizePerQuery *
//                        (totalQueryCount / ((float) maxNumberOfJobs / partitionListSize))));
//                throw new ServiceException("Requested DB Alignments cannot exceed " + maxDbAlignments + " for this search");
//            }
//        }
    }

    protected static synchronized long getAvgQueryLength(int queryCountInCurrentSplit, long nucleotideCountInCurrentSplit) {
        return nucleotideCountInCurrentSplit / queryCountInCurrentSplit;
    }

    /**
     * Needed to fix: Blast fails because numberOfHitsToKeep ends up being zero
     *
     * @param queryCountInCurrentSplit
     * @return
     */
    private static synchronized boolean isHitsPerQueryTooLow(int queryCountInCurrentSplit, int maxResultsPerJob) {
        int hitsPerQuery = calculateActualResultsPerInputEntry(queryCountInCurrentSplit, maxResultsPerJob, maxResultsPerJob);
        return hitsPerQuery < 1;
    }

    public static synchronized int calculateActualResultsPerInputEntry(int numberOfInputEntries, int desiredResultsPerInputEntry, int maxResultsPerJob) {
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

    private static class SplitFastaFilenameFilter implements FilenameFilter {
        private String extensionPattern;

        public SplitFastaFilenameFilter(String pattern) {
            extensionPattern = pattern;
        }

        public boolean accept(File dir, String name) {
            // split extention off
            int periodIdx = name.lastIndexOf('.');
            if (name.length() > periodIdx + 1) {
                String extension = name.substring(periodIdx + 1);
                if (extension.matches(extensionPattern))
                    return true;
            }
            return false;
        }
    }
}
