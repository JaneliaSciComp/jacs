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

package org.janelia.it.jacs.shared.dma.formatdb;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.dma.Tag;
import org.janelia.it.jacs.model.user_data.DownloadableFastaFileNode;
import org.janelia.it.jacs.model.user_data.blast.BlastDatabaseFileNode;
import org.janelia.it.jacs.shared.dma.DmaAction;
import org.janelia.it.jacs.shared.dma.DmaArgs;
import org.janelia.it.jacs.shared.dma.DmaFile;
import org.janelia.it.jacs.shared.dma.entity.EntityIdRange;
import org.janelia.it.jacs.shared.dma.entity.MutableLong;
import org.janelia.it.jacs.shared.dma.importer.scratch.EntityIdFiles;
import org.janelia.it.jacs.shared.dma.reporter.DmaLogger;
import org.janelia.it.jacs.shared.dma.reporter.Progress;
import org.janelia.it.jacs.shared.dma.reporter.ProgressCapturer;
import org.janelia.it.jacs.shared.dma.util.ConnPool;
import org.janelia.it.jacs.shared.dma.util.IdRangeWriter;
import org.janelia.it.jacs.shared.dma.util.SqlExecutor;
import org.janelia.it.jacs.shared.fasta.FastaFile;
import org.janelia.it.jacs.shared.hibernate.HibernateSessionSource;
import org.janelia.it.jacs.shared.perf.PerfStats;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * This class is responsible for generating fasta files representing blastable datasets as well
 * as using formatdb to produce the final dataset
 *
 * @author Tareq Nabeel
 */
public class BlastDbCreator implements DmaAction {

    private static final int BATCH_SIZE = SystemConfigurationProperties.getInt("dma.importExternalEntityBatchSeqCount");
    private static final int NUMBER_OF_MOD_CHUNKS = SystemConfigurationProperties.getInt("dma.blastDbGenModChunks");

    private static int SEQ_CHARS_PER_LINE = 61;

    private DmaLogger dmaLogger = DmaLogger.getInstance();

    private Connection conn;
    private MutableLong processedRecords = new MutableLong(0);
    private MutableLong processedSeqByteCount = new MutableLong(0);
    private MutableLong errorCount = new MutableLong(0);
    private ProgressCapturer progressCapturer;
    private long startTimeMillis;
    private MutableLong targetSeqCount = new MutableLong(0);
    private MutableLong targetSeqLength = new MutableLong(0);
    private DmaFile fastaFile;
    private DmaArgs dmaArgs;
    private String fastaDirPath;
    private File fastaSourceFile;
    private File downloadableFile;
    private DownloadableFastaFileNode downloadableFastaFileNode;

    private long datasetNodeId;
    private long TOTAL_RECORDS_PER_FILE = 10000000;

    private static final String ENTITY_TARGET_TABLE = SystemConfigurationProperties.getString("dma.entityBlastGenSourceTable");
    private static final String SEQUENCE_TARGET_TABLE = SystemConfigurationProperties.getString("dma.sequenceBlastGenSourceTable");
    private static final String SELECT_ENTITY_INFO = "select se.defline, bs.sequence,bs.sequence_type_code" +
            " from " + ENTITY_TARGET_TABLE + "  se, " + SEQUENCE_TARGET_TABLE + "  bs where bs.sequence_id=se.sequence_id\n" +
            " and se.entity_id in ";

    public static void main(String[] args) throws IOException, ClassNotFoundException, SQLException {
        BlastDbCreator inputCreator = new BlastDbCreator();
        inputCreator.setDmaFile(new DmaFile(FileUtil.ensureDirExists("C:\\dev\\Camera-0913\\Camera\\1015438972416950624")));
        inputCreator.execute();
    }

    public void execute() {
        try {
            PerfStats.start(PerfStats.KEY_FASTA_CREATOR_CREATE);

            init();
            if (!blastsetQueryDefined()) {
                return;
            }
            creatFastaFileForFormatDb();

            Session session = HibernateSessionSource.getOrCreateSession();
            Transaction tx = session.beginTransaction();

            // do this after long running creatFastaFileForFormatDb();
            downloadableFastaFileNode = (DownloadableFastaFileNode) session.createQuery("from DownloadableFastaFileNode dffn where dffn.objectId=:id").setLong("id", this.datasetNodeId).uniqueResult();
            updateNodeLengthAndSequenceCount();

            // Fetch the blastable dataset node from database
            // Run format db against the output from creatFastaFileForFormatDb and update blastDbFileNode state
            if (downloadableFastaFileNode instanceof BlastDatabaseFileNode) {
                formatDb();
            }

            // Create and save a DownloadableFastaFileNode object. Initialize its state based on downloadableFastaFileNode
            createDownloadableFastaFileAndInfoFile();

            session.save(downloadableFastaFileNode);

            // Create a readme text following Leonid's suggested format
            // We need the node id for the info file contents so this has to take place after session.save
            tx.commit();
        }
        catch (SQLException e) {
            handleSQLException(e);
        }
        catch (Exception e) {
            handleException(e);
        }
        finally {
            ConnPool.releaseConnection(conn);
            PerfStats.end(PerfStats.KEY_FASTA_CREATOR_CREATE);
        }
    }

    private boolean blastsetQueryDefined() {
        // Log error message but no stack trace (as requested by Leonid).  Set error count as usual
        String sqlQuery = SqlExecutor.getSingleResultStringValue("select blastsetsql(" + datasetNodeId + ")", "blastsetsql");
        if (sqlQuery == null || sqlQuery.trim().equals("")) {
            dmaLogger.logError("No query defined for blastable dataset node id:" + datasetNodeId, getClass());
            setErrorCount();
            return false;
        }
        else {
            return true;
        }
    }

    private void init() throws IOException {
        startTimeMillis = System.currentTimeMillis();
        conn = ConnPool.getConnection();
        fastaDirPath = this.getDmaFile().getAbsolutePath();
        // create the entire FASTA file from scratch
        FileUtil.cleanDirectory(fastaDirPath);
        fastaSourceFile = FileUtil.createNewFile(fastaDirPath + File.separator + this.datasetNodeId + ".fasta");
        datasetNodeId = Long.parseLong(fastaDirPath.substring(fastaDirPath.lastIndexOf(File.separator) + 1));
    }

    /**
     * Creates the input to fasta db using the output from writeEntityIdFiles();
     *
     * @throws IOException
     * @throws SQLException
     */
    private void creatFastaFileForFormatDb() throws IOException, SQLException {
        writeEntityIdFiles();
        BufferedWriter writer = new BufferedWriter(new FileWriter(fastaSourceFile));
        EntityIdFiles idFiles = new EntityIdFiles(getDmaFile().getFile(), new String[]{"ids"}, getDmaArgs());
        try {
            for (DmaFile dmaFile : idFiles.getDmaFileList()) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(dmaFile.getFile())));
                try {
                    String betweenClause;
                    while ((betweenClause = reader.readLine()) != null) {
                        int bracketIdx = betweenClause.indexOf("(");
                        if (bracketIdx != -1) {
                            betweenClause = betweenClause.substring(0, bracketIdx).trim();
                        }
                        writeSequenceInfos(betweenClause, writer);
                    }
                }
                finally {
                    reader.close();
                }
            }
        }
        finally {
            writer.flush();
            writer.close();
        }
    }


    /**
     * This method uses the fasta file produced by creatFastaFileForFormatDb() to create the blastable dataset
     * on the filesystem
     *
     * @throws IOException
     * @throws InterruptedException
     */
    private void formatDb() throws IOException, InterruptedException {
        CreateBlastDatabaseFromFastaTool pf = new CreateBlastDatabaseFromFastaTool(Logger.getLogger(this.getClass()));
        pf.setFastaFilePath(fastaSourceFile.getAbsolutePath());
        pf.setResidueType(downloadableFastaFileNode.getSequenceType());
//        pf.setOutputPath(bdfn.getDirectoryPath());
        File outputDir = FileUtil.ensureDirExists(SystemConfigurationProperties.getString("dma.blastDBFormatDbPath"),
                String.valueOf(this.datasetNodeId), true);
        FileUtil.cleanDirectory(outputDir);
        FastaFile fastaFile = new FastaFile(fastaSourceFile.getAbsolutePath());
        fastaFile.setSize(processedSeqByteCount.getValue(), processedRecords.getValue());
        pf.setFastaFile(fastaFile);
        pf.setOutputPath(outputDir.getAbsolutePath());
        pf.setPartitionPrefix(BlastDatabaseFileNode.PARTITION_PREFIX);
        pf.setPartitionSize(SystemConfigurationProperties.getLong("BlastServer.PartitionSize")); // experimental
        pf.setPartitionEntries(SystemConfigurationProperties.getLong("BlastServer.PartitionEntries"));
        Properties prop = new Properties();
        prop.setProperty(FormatDBTool.FORMATDB_PATH_PROP,
                SystemConfigurationProperties.getString(FormatDBTool.FORMATDB_PATH_PROP));
        prop.setProperty(SystemCall.SCRATCH_DIR_PROP,
                SystemConfigurationProperties.getString(SystemCall.SCRATCH_DIR_PROP));
        prop.setProperty(SystemCall.SHELL_PATH_PROP,
                SystemConfigurationProperties.getString(SystemCall.SHELL_PATH_PROP));
        prop.setProperty(SystemCall.STREAM_DIRECTOR_PROP,
                SystemConfigurationProperties.getString(SystemCall.STREAM_DIRECTOR_PROP));
        pf.setProperties(prop);
        pf.partition();
        ((BlastDatabaseFileNode) downloadableFastaFileNode).setPartitionCount(new Integer("" + pf.getNumPartitions()));
        checkForFormatDbErrors(outputDir);
    }

    private void updateNodeLengthAndSequenceCount() {
        downloadableFastaFileNode.setLength(processedSeqByteCount.getValue());
        downloadableFastaFileNode.setSequenceCount((int) processedRecords.getValue());
    }

    /**
     * This method copies over the fasta file that was input to formatDb to a location set by dma.properties
     * for downloading files
     *
     * @throws IOException
     */
    private void createDownloadableFastaFileAndInfoFile() throws IOException {
        if (dmaArgs.keepFasta()) {
            downloadableFile = FileUtil.createNewFile(downloadableFastaFileNode.createDownloadableFilePath(datasetNodeId));
            if (downloadableFile.getParent() != null) {
                FileUtil.ensureDirExists(downloadableFile.getParent());
            }
            // Copy the fasta file to downloadable directory
            //        FileUtil.copyFile(fastaSourceFile, this.downloadableNodeFile);
            FileUtil.moveFileUsingSystemCall(fastaSourceFile, this.downloadableFile);

            createDownloadableInfoFile();
        }
        else {
            boolean deleteSuccess = fastaSourceFile.delete();
            if (!deleteSuccess){
                System.err.println("Unable to delete fasta source file.");
            }
        }
    }

    /**
     * Needed because paging through JDBC resultsets slows down after 500,000 records.  So we write out
     * the ids to filesystem and process in junks
     *
     * @throws SQLException
     */
    private void writeEntityIdFiles() throws SQLException {
        PerfStats.start(getClass() + ".writeEntityIdFiles");
        String sqlStmt;
        try {
            Statement sourceStmt = conn.createStatement();
            sqlStmt = "create table stage_blast_set_" + datasetNodeId + " as select blastset as entity_id from blastset(" + datasetNodeId + ")";
            dmaLogger.logInfo("creating:  " + datasetNodeId, getClass());
            sourceStmt.execute(sqlStmt);
            sqlStmt = "create unique index stage_blast_set_" + datasetNodeId + "_ix_entity_id on stage_blast_set_" + datasetNodeId + "(entity_id)";
            dmaLogger.logInfo("indexing " + datasetNodeId, getClass());
            sourceStmt.execute(sqlStmt);
            sqlStmt = "analyze stage_blast_set_" + datasetNodeId;
            dmaLogger.logInfo("analyzing " + datasetNodeId, getClass());
            sourceStmt.execute(sqlStmt);
            sqlStmt = "select min(entity_id) as min_id, max(entity_id) as max_id, trunc((entity_id-1)::float/" + NUMBER_OF_MOD_CHUNKS + ".) as blk_id, count(entity_id) as num_id from (select entity_id from stage_blast_set_" + datasetNodeId + ")x group by blk_id order by min_id";
            dmaLogger.logInfo("chunking " + datasetNodeId, getClass());
            ResultSet sourceDbChunksRs = sourceStmt.executeQuery(sqlStmt);
            List<EntityIdRange> entityIdChunkRanges = new ArrayList<EntityIdRange>();
            while (sourceDbChunksRs.next()) {
                long startId = sourceDbChunksRs.getLong("min_id");
                long endId = sourceDbChunksRs.getLong("max_id");
//            long seqLength = sourceDbChunksRs.getLong("seq_length");
                int count = sourceDbChunksRs.getInt("num_id");
                entityIdChunkRanges.add(new EntityIdRange(startId, endId, count));
//                if (targetSeqCount.getValue() <= 1) {
//                    startEntityId = new MutableLong(startId);
//                }
                targetSeqCount.add(count);
//            targetSeqLength.add(seqLength);
            }

            IdRangeWriter.writeEntities(entityIdChunkRanges, new MutableLong(), new MutableLong(), fastaDirPath, "ids", TOTAL_RECORDS_PER_FILE, BATCH_SIZE);
            sourceStmt.close();
        }
        finally {
            PerfStats.end(getClass() + ".writeEntityIdFiles");
        }
    }

    /**
     * Used by creatFastaFileForFormatDb() to write out all the sequences for a given range in the ids file
     *
     * @param betweenClause entity range
     * @param writer        points to fasta file
     * @throws IOException
     * @throws SQLException
     */
    private void writeSequenceInfos(String betweenClause, BufferedWriter writer) throws IOException, SQLException {
//        dmaLogger.logInfo("Writing record:" + processedRecords, getClass());
        //todo switch to targetConn after testing
        Statement retrieveSeqInfoStmt = conn.createStatement();
        String sql = SELECT_ENTITY_INFO + " (select entity_id from stage_blast_set_" + this.datasetNodeId + " where entity_id between " + betweenClause + ")";
        try {
            ResultSet rs = retrieveSeqInfoStmt.executeQuery(sql);
            processedRecords.getValue();
            while (rs.next()) {
                writeSequence(writer, rs);
                processedRecords.increment();
            }
//            dmaLogger.logInfo(betweenClause + " count="+(processedRecords.getValue()-previousCount)+" processedRecords="+processedRecords,getClass());
            rs.close();
        }
        catch (SQLException e) {
            dmaLogger.logError("sql=" + sql, getClass(), e);
            throw e;
        }
    }

    /**
     * Used by writeSequenceInfos()
     *
     * @param writer
     * @param rs
     * @throws java.io.IOException
     * @throws java.sql.SQLException
     */
    private void writeSequence(BufferedWriter writer, ResultSet rs) throws SQLException, IOException {
        String defline = rs.getString("defline");
        if (!defline.startsWith(">"))
            writer.write(">");
        writer.write(defline);
        writer.newLine();
        String sequence = rs.getString("sequence");
        processedSeqByteCount.add(sequence.length());
        for (int i = 0; i < sequence.length(); i++) {
            if (i % SEQ_CHARS_PER_LINE == 0 && i > 0) {
                writer.newLine();
            }
            writer.write(sequence.charAt(i));
        }
        writer.newLine();
    }

    public String getName() {
        return this.getDmaFile().getName();
    }

    public Progress getProgress() {
        return progressCapturer.capture();
    }

    public long getProcessedSeqCount() {
        return processedRecords.getValue();
    }

    public long getProcessedByteCount() {
        return processedSeqByteCount.getValue();
    }

    public long getTargetSeqCount() {
        return targetSeqCount.getValue();
    }

    public long getTargetByteCount() {
        return targetSeqLength.getValue();
    }

    public long getSeqErrorCount() {
        return errorCount.getValue();
    }

    public void setProgressCapturer(ProgressCapturer progressCapturer) {
        this.progressCapturer = progressCapturer;
    }

    public long getStartTime() {
        return startTimeMillis;
    }

    public void setDmaFile(DmaFile dmaFile) {
        this.fastaFile = dmaFile;
    }

    public DmaFile getDmaFile() {
        return fastaFile;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getLabel() {
        return getName();
    }

    public void setDmaArgs(DmaArgs dmaArgs) {
        this.dmaArgs = dmaArgs;
    }

    public DmaArgs getDmaArgs() {
        return dmaArgs;
    }

    public String toString() {
        return getName();
    }

    private void handleSQLException(SQLException e) {
        handleException(e);
        SQLException nextException = e;
        while ((nextException = nextException.getNextException()) != null) {
            dmaLogger.logError("", this.getClass(), nextException);
        }
    }

    private void handleException(Exception e) {
        dmaLogger.logError("Executing importer " + getName() + " encountered exception:", this.getClass(), e);
        try {
            setErrorCount();
            conn.rollback();
        }
        catch (SQLException se) {
            se.printStackTrace();
        }
    }

    private void setErrorCount() {
        if (targetSeqCount.getValue() > 0) {
            errorCount.add(targetSeqCount);
        }
        else {
            // targetSeqCount hasn't been retrieved. Better indicate that something went
            // wrong even though the errorCount will be incorrect
            errorCount.increment();
        }
    }

    /**
     * Creates an info/readme file that describes the fasta file that was copied off the input to formatDb.
     * The location will be adjacent to the fasta file and is set in dma.properties
     *
     * @throws IOException
     */
    private void createDownloadableInfoFile() throws IOException {
        File downloadableReadmeFile = FileUtil.createNewFile(downloadableFastaFileNode.createDownloadableInfoFilePath(datasetNodeId));
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(downloadableReadmeFile));
        bufferedWriter.write(downloadableFile.getName());
        bufferedWriter.newLine();
        bufferedWriter.write("\tName: ");
        bufferedWriter.write(downloadableFastaFileNode.getName());
        bufferedWriter.newLine();
        bufferedWriter.write("\tDescription: ");
        bufferedWriter.write(downloadableFastaFileNode.getDescription());
        bufferedWriter.newLine();
        bufferedWriter.write("\tSequence Count: ");
        bufferedWriter.write(String.valueOf(downloadableFastaFileNode.getSequenceCount()));
        bufferedWriter.newLine();
        bufferedWriter.write("\tSequence Type: ");
        bufferedWriter.write(downloadableFastaFileNode.getSequenceType());
        bufferedWriter.newLine();
        bufferedWriter.write("\tLength: ");
        bufferedWriter.write(getFastaFileMBLength());
        bufferedWriter.newLine();
        bufferedWriter.write("\tTags: ");
        Set<Tag> tags = downloadableFastaFileNode.getDmaTags();
        int i = 0;
        for (Tag tag : tags) {
            if (i != 0) {
                bufferedWriter.write(",");
            }
            bufferedWriter.write(tag.getName());
            i++;
        }
        bufferedWriter.newLine();
        bufferedWriter.write("\tDataset Node Id: ");
        bufferedWriter.write(String.valueOf(downloadableFastaFileNode.getObjectId()));
        bufferedWriter.newLine();
        bufferedWriter.newLine();
        bufferedWriter.close();
    }

    private String getFastaFileMBLength() {
        NumberFormat numFormat = NumberFormat.getNumberInstance();
        double mbLength = downloadableFastaFileNode.getLength() / 1000000.00;
        numFormat.setMaximumFractionDigits(2);
        numFormat.setMinimumFractionDigits(2);
        return numFormat.format(mbLength) + " MB";
    }

    private void checkForFormatDbErrors(File blastDBOutputDir) throws IOException, InterruptedException {
        // grep 'ERROR' /db/cameradb/dma/system/**/formatdb.log | wc -l
        String formatdbLogPath = blastDBOutputDir.getAbsolutePath() + File.separator + "formatdb.log";
        int count = FileUtil.getCountUsingUnixCall("grep 'ERROR' " + formatdbLogPath + " | wc -l");
        dmaLogger.logInfo(this.downloadableFastaFileNode.getObjectId() + " formatdb error count=" + count, getClass());
        if (count > 0) {
            throw new RuntimeException("formatdb failed");
        }
    }
}
