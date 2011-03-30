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

package org.janelia.it.jacs.shared.dma;

import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.shared.dma.entity.MutableLong;
import org.janelia.it.jacs.shared.dma.formatdb.BlastDBDirs;
import org.janelia.it.jacs.shared.dma.formatdb.BlastDbCreator;
import org.janelia.it.jacs.shared.dma.formatdb.BlastDbNodeLoader;
import org.janelia.it.jacs.shared.dma.importer.fasta.FastaFiles;
import org.janelia.it.jacs.shared.dma.importer.fasta.FastaImporter;
import org.janelia.it.jacs.shared.dma.importer.scratch.*;
import org.janelia.it.jacs.shared.dma.reporter.DmaLogger;
import org.janelia.it.jacs.shared.dma.util.ConnPool;
import org.janelia.it.jacs.shared.dma.util.SqlExecutor;
import org.janelia.it.jacs.shared.perf.PerfStats;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class serves as the facade to the the DMA module.  It uses DmaArgs, DmaThreads, and instances of DmaFiles
 * and DmaAction to do its work.
 *
 * @author Tareq Nabeel
 */
public class DmaManager {

    private static final int MAX_PANDA_IMPORT_THREADS = SystemConfigurationProperties.getInt("dma.maxPandaImportThreads");
    private static final int MAX_EXTERNAL_ENTITY_IMPORT_THREADS = SystemConfigurationProperties.getInt("dma.maxExternalEntityImportThreads");
    private static final int MAX_INTERNAL_ENTITY_IMPORT_THREADS = SystemConfigurationProperties.getInt("dma.maxInternalEntityImportThreads");
    private static final int MAX_BLAST_DB_CREATION_THREADS = SystemConfigurationProperties.getInt("dma.maxBlastDbCreationThreads");

    private DmaFiles fastaFiles;
    private DmaFiles externalIdFiles;
    private DmaFiles internalIdFiles;


    private boolean statusLoggingStarted;
    private long startTimeMillis;

    private DmaThreads fastaImporterThreads;
    private DmaThreads internalIdsBatchWriterThread;
    private DmaThreads internalIdImporterThreads;
    private DmaThreads externalIdsBatchWriterThread;
    private DmaThreads externalIdImporterThreads;
    private DmaThreads blastDBCreationThreads;
    private DmaThreads internalEntityImporterThread;
    private DmaThreads internalSequenceImporterThread;

    private DmaArgs dmaArgs;
    private DmaLogger dmaLogger;

    /**
     * Gets called by dma.sh with various arguments
     *
     * @param args command line args
     * @see DmaArgs
     */
    public static void main(String[] args) {
        try {
            PerfStats.start("****TOTAL");
            DmaManager dmaManager = new DmaManager();
            dmaManager.init(args);
            dmaManager.execute();
            ConnPool.closeConnections();
            PerfStats.end("****TOTAL");
            PerfStats.printStatsToStdOut();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * You could potentially call this method after configuring a DmaManager instance
     * with DmaArgs
     */
    public void execute() throws IOException {
        startTimeMillis = System.currentTimeMillis();

        // This blows away the internal_sequence, internal_entity, external_sequence, and external_entity
        // scratch tables and recreates them from data in sequence_entity and bio_sequence tables
        // We do this only if requested in DmaArgs
        createScratchTables();

        // Execute any other prep commands in init.sql
        SqlExecutor.executeCommandsInFile("init.sql");

        // Truncate any tables requested in DmaArgs
        truncateTables();

        // Drop indexes if requested in DmaArgs
        dropIndexes();

        // Import external entities and sequences from fasta files (e.g. panda files) and then
        // import old external entities and sequences from scratch tables (external_entity and external_sequence)
        importExternalEntities();

        // Import internal entities and sequences from scratch tables (internal_entity and internal_sequence)
        importInternalEntities();

        // Generate the blastable datasets if requested in DmaArgs
        generateDataSets();

        // Recreate indexes that were dropped in dropIndexes() if requested in DmaArgs
        createIndexes();

        // Run any other clean up sql commands
        SqlExecutor.executeCommandsInFile("cleanup.sql");
    }

    /**
     * This blows away the internal_sequence, internal_entity, external_sequence, and external_entity
     * scratch tables and recreates them from data in sequence_entity and bio_sequence tables
     * We do this only if requested in DmaArgs
     */
    private void createScratchTables() {
        if (dmaArgs.createScratchTables()) {
            // Spin off the internal and external entity table creations
            DmaThreads internalEntityScratchCreator = new DmaThreads("Internal Entity Scratch Creator", InternalEntityScratchCreator.class.getName(), this, false, false);
            internalEntityScratchCreator.execute();
            DmaThreads externalEntityScratchCreator = new DmaThreads("External Entity Scratch Creator", ExternalEntityScratchCreator.class.getName(), this, false, false);
            externalEntityScratchCreator.execute();
            if (dmaArgs.doSequenceImport()) {
                // Spin off the internal and external sequence table creations
                DmaThreads internalSequenceScratchCreator = new DmaThreads("Internal Sequence Scratch Creator", InternalSequenceScratchCreator.class.getName(), this, false, false);
                internalSequenceScratchCreator.execute();
                DmaThreads externalSequenceScratchCreator = new DmaThreads("External Sequence Scratch Creator", ExternalSequenceScratchCreator.class.getName(), this, false, false);
                externalSequenceScratchCreator.execute();

                // Wait for the internal and external sequence table creations to complete
                internalSequenceScratchCreator.waitForCompletion();
                externalSequenceScratchCreator.waitForCompletion();
            }
            // Wait for the internal and external entity table creations to complete
            internalEntityScratchCreator.waitForCompletion();
            externalEntityScratchCreator.waitForCompletion();
        }
    }

    /**
     * Import external entities and sequences from fasta files (e.g. panda files) and then
     * import old external entities and sequences from scratch tables (external_entity
     * and external_sequence)
     */
    private void importExternalEntities() {
        // Prepare the fasta files for import
        getFastaFilesForImport();
        // Spin off fasta file (e.g. panda data) import
        spinOffFastaImport();
        // Spin off retrieval of external entity ids for import
        spinOffExternalIdBatchWriter();

        waitForFastaImportCompletion();   // MUST happen before we call spinOffExternalEntityImports
        waitForExternalIdBatchWriterCompletion(); // MUST happen before we call getExternalIdFilesForImport
        getExternalIdFilesForImport();
        spinOffExternalEntityImports();
        waitForExternalEntityImportsCompletion();
    }


    /**
     * Import internal entities and sequences from scratch tables (internal_entity and internal_sequence)
     * Do the imports in separate threads
     */
    private void importInternalEntities() {
        spinOffInternalEntityImport();
        spinOffInternalSequenceImport();
        waitForInternalEntityImportCompletion();
        waitForInternalSequenceImportCompletion();
    }

    /**
     * Spin off import of internal sequences from internal_sequence scratch table
     */
    private void spinOffInternalSequenceImport() {
        if (dmaArgs.doInternalEntityImport() && dmaArgs.doSequenceImport()) {
            internalSequenceImporterThread = new DmaThreads("Interal Entity Import", InternalEntitySQLImporter.class.getName(), this, false, false);
            internalSequenceImporterThread.execute();
        }
    }

    /**
     * Spin off import of internal entities from internal_entity scratch table
     */
    private void spinOffInternalEntityImport() {
        if (dmaArgs.doInternalEntityImport()) {
            internalEntityImporterThread = new DmaThreads("Internal Sequence Import", InternalSequenceSQLImporter.class.getName(), this, false, false);
            internalEntityImporterThread.execute();
        }
    }

    /**
     * Wait for import of internal sequences from internal_sequence scratch table to complete
     */
    private void waitForInternalSequenceImportCompletion() {
        if (dmaArgs.doInternalEntityImport() && dmaArgs.doSequenceImport()) {
            internalSequenceImporterThread.waitForCompletion();
        }
    }

    /**
     * Wait for import of internal entities from internal_entity scratch table to complete
     */
    private void waitForInternalEntityImportCompletion() {
        if (dmaArgs.doInternalEntityImport()) {
            internalEntityImporterThread.waitForCompletion();
        }
    }

    /**
     * Generate the blastable datasets if requested in DmaArgs
     */
    private void generateDataSets() {
        spinOffBlastDBCreation();
        waitForBlastDBCreationCompletion();
    }

    /**
     * Spin off blastable dataset creation in multiple threads
     */
    private void spinOffBlastDBCreation() {
        if (dmaArgs.doBlastDBCreation()) {
            try {
                BlastDBDirs blastDBDirs = BlastDbNodeLoader.loadBlastDBFileNodes(dmaArgs);
                blastDBCreationThreads = new DmaThreads(dmaArgs.doBlastDBCreation() ? "BlastDBCreation" : "DownloadableCreation", blastDBDirs, BlastDbCreator.class.getName(), this, MAX_BLAST_DB_CREATION_THREADS, true, false);
                blastDBCreationThreads.execute();
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
            catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void waitForBlastDBCreationCompletion() {
        if (dmaArgs.doBlastDBCreation()) {
            blastDBCreationThreads.waitForCompletion();
        }
    }

    /**
     * Truncate tables requested in DmaArgs
     */
    private void truncateTables() {
        if (dmaArgs.getTruncateTables() != null) {
            for (String truncateTable : dmaArgs.getTruncateTables()) {
                SqlExecutor.execute("truncate table " + truncateTable);
            }
        }
    }

    /**
     * Wait for all external entity thread imports to complete
     */
    private void waitForExternalEntityImportsCompletion() {
        if (dmaArgs.doExternalEntityImport()) {
            externalIdImporterThreads.waitForCompletion();
        }
    }

    /**
     * Spin of external entity imports in multiple threads if requested in DmaArgs
     */
    private void spinOffExternalEntityImports() {
        if (dmaArgs.doExternalEntityImport()) {
            externalIdImporterThreads = new DmaThreads("EE Import", externalIdFiles, ExternalEntityImporter.class.getName(), this, MAX_EXTERNAL_ENTITY_IMPORT_THREADS, true, true);
            externalIdImporterThreads.execute();
        }
    }

    /**
     * Spin off fasta file (e.g. panda data) imports in multiple threads
     */
    private void spinOffFastaImport() {
        if (dmaArgs.doFastaImport()) {
            fastaImporterThreads = new DmaThreads("Fasta Import", fastaFiles, FastaImporter.class.getName(), this, MAX_PANDA_IMPORT_THREADS, true, true);
            fastaImporterThreads.execute();
        }
    }

    /**
     * Wait for fasta file (e.g. panda data) imports to complete
     */
    private void waitForFastaImportCompletion() {
        if (dmaArgs.doFastaImport()) {
            fastaImporterThreads.waitForCompletion();
        }
    }

    /**
     * Spin off external entity id retrieval (from external_entity scratch table)
     */
    private void spinOffExternalIdBatchWriter() {
        if (dmaArgs.doExternalEntityImport()) {
            externalIdsBatchWriterThread = new DmaThreads("EEId BatchWriter", ExternalEntityIdBatchWriter.class.getName(), this, false, false);
            externalIdsBatchWriterThread.execute();
        }
    }

    /**
     * Wait for external entity id retrievals (from external_entity scratch table)
     * to complete
     */
    private void waitForExternalIdBatchWriterCompletion() {
        if (dmaArgs.doExternalEntityImport()) {
            externalIdsBatchWriterThread.waitForCompletion();
        }
    }

    /**
     * Prepare fasta files (e.g. panda data) for import
     */
    private void getFastaFilesForImport() {
        if (dmaArgs.doFastaImport()) {
            String[] fastaInputs = dmaArgs.getFastaInputs();
            List<File> fastaInputList = new ArrayList<File>();
            try {
                for (String input : fastaInputs) {
                    fastaInputList.add(FileUtil.checkFileExists(input));
                }
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
            fastaFiles = new FastaFiles(fastaInputList, new String[]{".niaa", ".fasta"}, getDmaArgs());
        }
    }

    /**
     * Get the external entity ids written out by externalIdsBatchWriterThread and
     * prepare the EntityIdFiles instance using it ... for the ExternalEntityImporter
     */
    private void getExternalIdFilesForImport() {
        if (dmaArgs.doExternalEntityImport()) {
            externalIdFiles = new EntityIdFiles(dmaLogger.getExternalIdsDir(), new String[]{SystemConfigurationProperties.getString("dma.externalIdsFileExtension")}, getDmaArgs());
        }
    }

    /**
     * Status logging is executed in a separate thread using StatusRunnable
     */
    public void startStatusLogging() {
        if (!statusLoggingStarted) {
            Thread statusThread = new Thread(new StatusRunnable());
            statusThread.start();
            statusLoggingStarted = true;
        }
    }

    /**
     * Used to log Dma progress status in a separate thread
     * We need access to all the DmaThread instances, so we need this to be a
     * tightly coupled inner class
     */
    private class StatusRunnable implements Runnable {
        public void run() {
            try {
                while (getLiveThreadCount() > 0) {
                    logStatus();
                    Thread.sleep(SystemConfigurationProperties.getInt("dma.progressLogRate") * 1000);
                }
                dmaLogger.cleanup();
            }
            catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void logStatus() {
        StringBuilder finalReport = new StringBuilder();
        appendReports(finalReport);
        dmaLogger.logImportStatus(finalReport, startTimeMillis);
    }

    /**
     * Appends the statuses of all the DmaThread instances to the final report to be logged
     * to console and file system
     *
     * @param finalReport
     */
    private void appendReports(StringBuilder finalReport) {
        appendReport(finalReport, this.fastaImporterThreads, true);
        appendReport(finalReport, this.internalIdsBatchWriterThread, false);
        appendReport(finalReport, this.externalIdsBatchWriterThread, false);
        appendReport(finalReport, this.internalIdImporterThreads, false);
        appendReport(finalReport, this.externalIdImporterThreads, false);
        appendReport(finalReport, this.blastDBCreationThreads, true);
    }

    private void appendReport(StringBuilder finalReport, DmaThreads dmaThreads, boolean writeFullReportUnconditionally) {
        if (dmaThreads != null) {
            if (writeFullReportUnconditionally || dmaArgs.allReport()) {
                finalReport.append(dmaThreads.getFullReport());
            }
            else {
                finalReport.append(dmaThreads.getTotalReport());
            }
        }
    }

    private long getLiveThreadCount() {
        MutableLong threadCount = new MutableLong();
        addLiveThreadCount(threadCount, fastaImporterThreads);
        addLiveThreadCount(threadCount, internalIdsBatchWriterThread);
        addLiveThreadCount(threadCount, internalIdImporterThreads);
        addLiveThreadCount(threadCount, externalIdsBatchWriterThread);
        addLiveThreadCount(threadCount, externalIdImporterThreads);
        addLiveThreadCount(threadCount, blastDBCreationThreads);
        return threadCount.getValue();
    }

    private void addLiveThreadCount(MutableLong count, DmaThreads dmaThreads) {
        if (dmaThreads != null) {
            count.add(dmaThreads.getLiveThreadCount());
        }
    }

    private void init(String[] args) throws IOException {
        dmaArgs = new DmaArgs(args);
        dmaLogger = DmaLogger.getInstance(dmaArgs);
    }

    public DmaArgs getDmaArgs() {
        return dmaArgs;
    }

    /**
     * Drop indexes if requested in DmaArgs
     */
    private void dropIndexes() throws IOException {
        if (dmaArgs.recreateIndexes()) {
            SqlExecutor.executeCommandsInFile("drop_indexes.sql");
        }
    }

    /**
     * Recreate indexes that were dropped in dropIndexes() if requested in DmaArgs
     */
    private void createIndexes() throws IOException {
        if (dmaArgs.recreateIndexes()) {
            SqlExecutor.executeCommandsInFile("create_indexes.sql");
        }
    }
}
