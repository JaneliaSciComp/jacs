
package org.janelia.it.jacs.compute.service.blast.fastasplit;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.recruitment.FRVSamplingFastaGenerationTask;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.shared.fasta.FastaFile;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Scanner;

/**
 */
public class BlastMultiFastaSplitterFrvSamplingService implements IService {

    public static final String FASTA_SUFFIX = ".fasta";
    private Logger logger;
    private File inputFile;
    private int queryFilePointer = 0;
    private int numQueryFiles = 1000;
    private int totalQueryCount = 0;

    public void execute(IProcessData processData) throws ServiceException {
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            Task task = ProcessDataHelper.getTask(processData);
            ComputeDAO computeDAO = new ComputeDAO(logger);
            FastaFileNode fastaNode = (FastaFileNode) computeDAO.getNodeById(Long.valueOf(task.getParameter(FRVSamplingFastaGenerationTask.PARAM_query)));
            inputFile = new File(fastaNode.getFastaFilePath());
            splitFile();
            recordTheSeqCounts();
        }
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    /**
     * This method splits the uploaded query file into pieces.  The grid will search these pieces in parallel.
     *
     * @throws java.io.IOException  - problem with file io
     * @throws org.janelia.it.jacs.compute.engine.service.ServiceException
     *                              - problem with an underlying service tihs calls
     * @throws InterruptedException - interrpution while working on splitting the file
     */
    private void splitFile() throws IOException, ServiceException, InterruptedException {
        File splitFileDir = new File(inputFile.getParentFile().getAbsolutePath() + File.separator + "split");
        if (!splitFileDir.exists()) {
            boolean dirSuccess = splitFileDir.mkdir();
            if (!dirSuccess) {
                throw new ServiceException("Could not make split dirs.");
            }
        }

        logger.info("Splitting multifasta file... ");
        // Create all the query files
        String filepathPrefix = splitFileDir.getAbsolutePath() + File.separator + "query";
        for (int i = 0; i < numQueryFiles; i++) {
            boolean fileCreated = new File(filepathPrefix + i + FASTA_SUFFIX).createNewFile();
            if (!fileCreated) {
                throw new ServiceException("Unable to create split query files");
            }
        }
        Scanner scanner = new Scanner(inputFile);
        StringBuffer entry = new StringBuffer();
        String line;
        try {
            while (scanner.hasNextLine()) {
                line = scanner.nextLine();
                if (line.startsWith(">")) {
                    flushEntryToFile(entry, filepathPrefix);
                    entry = new StringBuffer(line);
                    entry.append("\n");
                }
                else {
                    entry.append(line).append("\n");
                }
            }
            flushEntryToFile(entry, filepathPrefix);
        }
        finally {
            // This is the last writer.  We need to close it and rename the query file
            scanner.close();
        }
        if (logger.isInfoEnabled()) {
            logger.info("Created segmented fasta files for " + inputFile.getName());
        }
        logger.debug("Total Query Count: " + totalQueryCount);
    }


    /**
     * This method stripes the fasta entries into numQueryFiles files.  This ensures that a genome which has tons
     * of hits will not all be in a single fasta file, taking much longer to search than other pieces.  This splitting
     * up of entries is also extremely slow (file IO) and needs to be refactored.
     *
     * @param entry          - fasta entry to write to the file
     * @param filenamePrefix - the file to write to
     * @throws IOException - error thrown when there is a problem writing the entry to the file
     */
    private void flushEntryToFile(StringBuffer entry, String filenamePrefix) throws IOException {
        if (null != entry && !"".equals(entry.toString())) {
            FileWriter writer = null;
            try {
                writer = new FileWriter(filenamePrefix + queryFilePointer + FASTA_SUFFIX, true);
                writer.write(entry.toString());
                queryFilePointer++;
                queryFilePointer = queryFilePointer % numQueryFiles;
                totalQueryCount++;
            }
            finally {
                if (null != writer) {
                    try {
                        writer.close();
                    }
                    catch (IOException e) {
                        logger.error("Unable to close the query writer stream!");
                    }
                }
            }
        }
    }

    /**
     * This method writes a series of files which contain the number of entries in the fasta files.
     *
     * @throws IOException - thrown when there is a problem reading the fastas or writing the seqCount files.
     */
    private void recordTheSeqCounts() throws IOException {
        File splitDir = new File(inputFile.getParentFile().getAbsolutePath() + File.separator + "split");
        File[] splitFiles = splitDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith("query") && name.endsWith(".fasta");
            }
        });
        for (File splitFile : splitFiles) {
            FastaFile tmpFasta = new FastaFile(splitFile);
            FileWriter writer = null;
            try {
                writer = new FileWriter(new File(splitFile.getAbsolutePath() + ".seqCount"));
                writer.write(Long.toString(tmpFasta.getSize().getEntries()));
                writer.write("\n");
            }
            finally {
                if (null != writer) {
                    writer.close();
                }
            }
        }
    }

}