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

package org.janelia.it.jacs.shared.dma.importer.scratch;

import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.shared.dma.DmaAction;
import org.janelia.it.jacs.shared.dma.DmaArgs;
import org.janelia.it.jacs.shared.dma.DmaFile;
import org.janelia.it.jacs.shared.dma.entity.MutableLong;
import org.janelia.it.jacs.shared.dma.reporter.DmaLogger;
import org.janelia.it.jacs.shared.dma.reporter.Progress;
import org.janelia.it.jacs.shared.dma.reporter.ProgressCapturer;
import org.janelia.it.jacs.shared.dma.util.ConnPool;
import org.janelia.it.jacs.shared.perf.PerfStats;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.*;

/**
 * This is the base class for importing external and internal entities/sequences from external and
 * internal entity/sequence tables respectively. It uses the output from the BaseEntityIdBatchWriter
 * (eeid/ieid files) to do it's imports using multiple-threads (DmaThreads)
 *
 * @author Tareq Nabeel
 */
public abstract class BaseEntityImporter implements DmaAction {
    protected DmaLogger dmaLogger = DmaLogger.getInstance();
    protected static final String ENTITY_TARGET_TABLE = SystemConfigurationProperties.getString("dma.entityTargetTable");
    protected static final String SEQUENCE_TARGET_TABLE = SystemConfigurationProperties.getString("dma.sequenceTargetTable");

    private ProgressCapturer progressCapturer;
    private Connection conn = null;
    private DmaFile idFile;
    private MutableLong insertSeqCount = new MutableLong();
    private MutableLong processedSeqCount = new MutableLong();
    private DmaArgs dmaArgs;

    private long startTimeMillis;

    private MutableLong entityId = new MutableLong(0);
    private MutableLong startEntityId;

    protected abstract boolean getObsoleteFlag(ResultSet sourceDbRs) throws SQLException;

    protected abstract int getSourceId();

    protected abstract String getSourceDbQueryString();

    protected abstract ResultSet getSourceDbRsToImport(Statement sourceStmt, String betweenClause);

    protected abstract String getSourceEntityTable();

    protected abstract String getSourceSequenceTable();

    private static String INSERT_ENTITY_STMT = "INSERT INTO " + ENTITY_TARGET_TABLE + " (entity_id, camera_acc, defline, owner_id, sequence_id, sequence_length, entity_type_code, external_source, external_acc, ncbi_gi_number, comment, assembly_acc, library_acc, sample_acc, organism, taxon_id, locus, protein_acc, orf_acc, dna_acc, dna_begin, dna_end, dna_orientation, translation_table, stop_5_prime, stop_3_prime, trace_acc, template_acc, clear_range_begin, clear_range_end, sequencing_direction, type, strain, source_id, obs_flag) " +
            //entity_id, camera_acc, defline, owner_id, sequence_id, sequence_length, entity_type_code, external_source, external_acc, ncbi_gi_number, comment, assembly_acc, library_acc, sample_acc, organism, taxon_id, locus, protein_acc, orf_acc, dna_acc, dna_begin, dna_end, dna_orientation, translation_table, stop_5_prime, stop_3_prime, trace_acc, template_acc, clear_range_begin, clear_range_end, sequencing_direction, type, strain, source_id, obs_flag) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static String INSERT_SEQUENCE_STMT = "INSERT INTO " + SEQUENCE_TARGET_TABLE + " (sequence_id, sequence_type_code, sequence, sequence_md5,source_id) \n" +
            "    VALUES(?, ?, ?, ?,?)";

    private void init() {
        try {
            startTimeMillis = System.currentTimeMillis();
        }
        catch (Exception e) {
            dmaLogger.logError("Initializing importer " + getName() + " encountered exception", getClass(), e);
            throw new RuntimeException(e);
        }
    }

    protected String getSourceDbAllEntitySequenceColumnsQueryString() {
        String query = "select se.entity_id, se.camera_acc, se.defline, se.owner_id, se.sequence_length, se.entity_type_code, se.external_source, se.external_acc, se.ncbi_gi_number, se.comment, se.assembly_acc, se.library_acc, se.sample_acc, se.organism, se.taxon_id, se.locus, se.protein_acc, se.orf_acc, se.dna_acc, se.dna_begin, se.dna_end, se.dna_orientation, se.translation_table, se.stop_5_prime, se.stop_3_prime, se.trace_acc, se.template_acc, se.clear_range_begin, se.clear_range_end, se.sequencing_direction, se.type, se.strain, se.source_id, se.obs_flag";
        if (getDmaArgs().doSequenceImport()) {
            return query + ", bs.sequence_id, bs.sequence_type_code, bs.sequence, bs.sequence_md5 from " + getSourceEntityTable() + " se inner join " + getSourceSequenceTable() + " bs on se.sequence_id=bs.sequence_id ";
        }
        else {
            return query + ", se.sequence_id from " + getSourceEntityTable() + " se ";
        }
    }

    protected ResultSet getSourceDbRs(Statement sourceStmt, String betweenClause) {
        String getEntitiesAndSequencesQuery = getSourceDbQueryString() + " where se.entity_id between " + betweenClause;
        ResultSet sourceDbRs;
        try {
            if (dmaLogger.isDebugEnabled(getClass())) {
                dmaLogger.logDebug("Executing query: " + getEntitiesAndSequencesQuery, getClass());
            }
            sourceDbRs = sourceStmt.executeQuery(getEntitiesAndSequencesQuery);
        }
        catch (SQLException e) {
            throw new RuntimeException("Query: " + getEntitiesAndSequencesQuery, e);
        }

        return sourceDbRs;

    }

    public void execute() {
        PerfStats.start("BaseEntityImporter.importData");
        this.conn = ConnPool.getConnection();
        BufferedReader reader = null;
        Statement sourceStmt = null;
        PreparedStatement targetEntityPstmt = null;
        PreparedStatement targetSequencePstmt = null;
        try {
            init();
            conn.setAutoCommit(false);
            sourceStmt = conn.createStatement();
            targetEntityPstmt = conn.prepareStatement(INSERT_ENTITY_STMT);
            targetSequencePstmt = conn.prepareStatement(INSERT_SEQUENCE_STMT);

            // FileReader did not block on reader.readline()
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(idFile.getFile())));
            String betweenClause;
            while ((betweenClause = reader.readLine()) != null) {
                int startBracketIdx = betweenClause.indexOf("(");
                int endBracketIdx = betweenClause.indexOf(")");
                long batchSize = Long.parseLong(betweenClause.substring(startBracketIdx + 1, endBracketIdx).trim());
                betweenClause = betweenClause.substring(0, startBracketIdx).trim();
                ResultSet sourceDbRs = getSourceDbRsToImport(sourceStmt, betweenClause);
                processBatch(sourceDbRs, conn, targetEntityPstmt, targetSequencePstmt, entityId);
                processedSeqCount.add(batchSize);
                sourceDbRs.close();
            }
        }
        catch (SQLException e) {
            handleSQLException(e);
        }
        catch (Exception e) {
            handleException(e);
        }
        finally {
            try {
                targetEntityPstmt.close();
                targetSequencePstmt.close();
                reader.close();
                sourceStmt.close();
                ConnPool.releaseConnection(conn);
            }
            catch (Exception e) {
                // Error trying to close the prepared statements
            }
        }
        PerfStats.end("BaseEntityImporter.importData");
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
            conn.rollback();
        }
        catch (SQLException se) {
            se.printStackTrace();
        }
    }

    private void processBatch(ResultSet sourceDbRs, Connection targetConn, PreparedStatement targetEntityPstmt, PreparedStatement targetSequencePstmt, MutableLong entityId) throws SQLException {
        while (sourceDbRs.next()) {
            processRecord(sourceDbRs, targetEntityPstmt, targetSequencePstmt, entityId);
        }
        targetEntityPstmt.executeBatch();
        targetEntityPstmt.clearParameters();
        targetEntityPstmt.clearBatch();
        if (getDmaArgs().doSequenceImport()) {
            targetSequencePstmt.executeBatch();
            targetSequencePstmt.clearParameters();
            targetSequencePstmt.clearBatch();
        }
        targetConn.commit();
    }

    private void processRecord(ResultSet sourceDbRs, PreparedStatement targetEntityPstmt, PreparedStatement targetSequencePstmt, MutableLong entityId) throws SQLException {
        // entity_id, camera_acc, defline, owner_id, sequence_id, sequence_length,
        // entity_type_code, external_source, external_acc, ncbi_gi_number, comment,
        // assembly_acc, library_acc, sample_acc, organism, taxon_id, locus, protein_acc,
        // orf_acc, dna_acc, dna_begin, dna_end, dna_orientation, translation_table,
        // stop_5_prime, stop_3_prime, trace_acc, template_acc, clear_range_begin,
        // clear_range_end, sequencing_direction, type, strain, source_id, obs_flag
        long sourceDbEntityId = sourceDbRs.getLong("entity_id");
        targetEntityPstmt.setLong(1, sourceDbEntityId);
        targetEntityPstmt.setString(2, sourceDbRs.getString("camera_acc"));
        targetEntityPstmt.setString(3, sourceDbRs.getString("defline"));
        targetEntityPstmt.setLong(4, sourceDbRs.getLong("owner_id"));
        targetEntityPstmt.setLong(5, sourceDbRs.getLong("sequence_id"));
        targetEntityPstmt.setInt(6, sourceDbRs.getInt("sequence_length"));
        targetEntityPstmt.setInt(7, sourceDbRs.getInt("entity_type_code"));
        targetEntityPstmt.setString(8, sourceDbRs.getString("external_source"));
        targetEntityPstmt.setString(9, sourceDbRs.getString("external_acc"));
        targetEntityPstmt.setInt(10, sourceDbRs.getInt("ncbi_gi_number"));
        targetEntityPstmt.setString(11, sourceDbRs.getString("comment"));
        targetEntityPstmt.setString(12, sourceDbRs.getString("assembly_acc"));
        targetEntityPstmt.setString(13, sourceDbRs.getString("library_acc"));
        targetEntityPstmt.setString(14, sourceDbRs.getString("sample_acc"));
        targetEntityPstmt.setString(15, sourceDbRs.getString("organism"));
        targetEntityPstmt.setInt(16, sourceDbRs.getInt("taxon_id"));
        targetEntityPstmt.setString(17, sourceDbRs.getString("locus"));
        targetEntityPstmt.setString(18, sourceDbRs.getString("protein_acc"));
        targetEntityPstmt.setString(19, sourceDbRs.getString("orf_acc"));
        targetEntityPstmt.setString(20, sourceDbRs.getString("dna_acc"));
        targetEntityPstmt.setInt(21, sourceDbRs.getInt("dna_begin"));
        targetEntityPstmt.setInt(22, sourceDbRs.getInt("dna_end"));
        targetEntityPstmt.setInt(23, sourceDbRs.getInt("dna_orientation"));
        targetEntityPstmt.setString(24, sourceDbRs.getString("translation_table"));
        targetEntityPstmt.setString(25, sourceDbRs.getString("stop_5_prime"));
        targetEntityPstmt.setString(26, sourceDbRs.getString("stop_3_prime"));
        targetEntityPstmt.setString(27, sourceDbRs.getString("trace_acc"));
        targetEntityPstmt.setString(28, sourceDbRs.getString("template_acc"));
        targetEntityPstmt.setInt(29, sourceDbRs.getInt("clear_range_begin"));
        targetEntityPstmt.setInt(30, sourceDbRs.getInt("clear_range_end"));
        targetEntityPstmt.setString(31, sourceDbRs.getString("sequencing_direction"));
        targetEntityPstmt.setString(32, sourceDbRs.getString("type"));
        targetEntityPstmt.setString(33, sourceDbRs.getString("strain"));
        targetEntityPstmt.setInt(34, getSourceId());
        targetEntityPstmt.setBoolean(35, getObsoleteFlag(sourceDbRs));
        entityId.setValue(sourceDbEntityId);

        if (getDmaArgs().doSequenceImport()) {
            targetSequencePstmt.setLong(1, sourceDbRs.getLong("sequence_id"));
            targetSequencePstmt.setInt(2, sourceDbRs.getInt("sequence_type_code"));
            targetSequencePstmt.setString(3, sourceDbRs.getString("sequence"));
            targetSequencePstmt.setString(4, sourceDbRs.getString("sequence_md5"));
            targetSequencePstmt.setInt(5, getSourceId());
        }
        insertSeqCount.increment();
        targetEntityPstmt.addBatch();
        if (getDmaArgs().doSequenceImport()) {
            targetSequencePstmt.addBatch();
        }
        if (insertSeqCount.getValue() == 1) {
            startEntityId = new MutableLong(entityId.getValue());
        }
    }

    public MutableLong getEntityId() {
        return entityId;
    }

    public MutableLong getStartEntityId() {
        return startEntityId;
    }

    public void setDmaFile(DmaFile dmaFile) {
        idFile = dmaFile;
    }

    public void setConnection(Connection conn) {
        this.conn = conn;
    }

    public long getStartTime() {
        return startTimeMillis;
    }


    public void setProgressCapturer(ProgressCapturer capturer) {
        this.progressCapturer = capturer;
    }

    public DmaFile getDmaFile() {
        return idFile;
    }

    public Progress getProgress() {
        return progressCapturer.capture();
    }

    public long getProcessedSeqCount() {
        if (processedSeqCount != null) {
            return processedSeqCount.getValue();
        }
        else {
            return -1;
        }
    }

    public long getProcessedByteCount() {
        return 0;
    }

    public long getTargetSeqCount() {
        return idFile.getTargetSeqCount();
    }

    public long getTargetByteCount() {
        return 0;
    }

    protected Connection getConn() {
        return conn;
    }

    public String getLabel() {
        return getName();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;

        BaseEntityImporter that = (BaseEntityImporter) o;

        return getName().equals(that.getName());
    }

    public int hashCode() {
        return getName().hashCode();
    }

    public String toString() {
        return getName();
    }

    public String getName() {
        return idFile.getName();
    }

    public void setDmaArgs(DmaArgs dmaArgs) {
        this.dmaArgs = dmaArgs;
    }

    public DmaArgs getDmaArgs() {
        return dmaArgs;
    }


}
