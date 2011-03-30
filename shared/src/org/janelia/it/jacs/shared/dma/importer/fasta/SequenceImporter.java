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

package org.janelia.it.jacs.shared.dma.importer.fasta;

import org.janelia.it.jacs.model.TimebasedIdentifierGenerator;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.shared.dma.entity.SequenceInfo;
import org.janelia.it.jacs.shared.dma.entity.SequenceInfos;
import org.janelia.it.jacs.shared.dma.reporter.DmaLogger;
import org.janelia.it.jacs.shared.perf.PerfStats;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;

/**
 * This class is responsible for persisting the sequence_entity and bio_sequence records to the
 * database using Sequence Info objects
 *
 * @author Tareq Nabeel
 */
public class SequenceImporter {

    private PreparedStatement insertEntitiesPstmt;
    private PreparedStatement insertSequencesPstmt;
    private DmaLogger dmaLogger = DmaLogger.getInstance();

    private static final int EXTERNAL_SOURCE_ID = SystemConfigurationProperties.getInt("dma.externalEntitySourceId");

    private static final boolean GENERATE_MD5 = SystemConfigurationProperties.getBoolean("dma.generateMD5InFastaImport");
    private static final String ENTITY_TARGET_TABLE = SystemConfigurationProperties.getString("dma.entityTargetTable");
    private static final String SEQUENCE_TARGET_TABLE = SystemConfigurationProperties.getString("dma.sequenceTargetTable");

    private static String INSERT_SEQUENCE_STMT;

    private static final String INSERT_SEQ_ENTITY_STMT = "INSERT INTO " + ENTITY_TARGET_TABLE + " (entity_id,camera_acc,defline,sequence_id,sequence_length,entity_type_code,external_acc,ncbi_gi_number,comment,organism,taxon_id,assembly_acc,source_id,owner_id,external_source,library_acc,sample_acc,locus,protein_acc,orf_acc,dna_acc,dna_begin,dna_end,dna_orientation,translation_table,stop_5_prime,stop_3_prime,trace_acc,template_acc,clear_range_begin,clear_range_end,sequencing_direction,type,strain,obs_flag) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    static {
        if (GENERATE_MD5) {
            INSERT_SEQUENCE_STMT = "insert into " + SEQUENCE_TARGET_TABLE + "  (sequence_id,sequence_type_code,sequence,sequence_md5,source_id) values (?,?,?,md5(?),?)";
        }
        else {
            INSERT_SEQUENCE_STMT = "insert into " + SEQUENCE_TARGET_TABLE + "  (sequence_id,sequence_type_code,sequence,source_id) values (?,?,?,?)";
        }
    }

    /**
     * @param conn
     * @throws SQLException
     */
    public SequenceImporter(Connection conn) throws SQLException {
        this.insertEntitiesPstmt = conn.prepareStatement(INSERT_SEQ_ENTITY_STMT);
        this.insertSequencesPstmt = conn.prepareStatement(INSERT_SEQUENCE_STMT);
    }

    /**
     * This method persists all sequences in sequenceInfos into "dma.entityTargetTable" and "dma.sequenceTargetTable"
     * tables specified in dma.properties.
     *
     * @param sequenceInfos the parsed sequences from fasta file
     * @throws SQLException
     */
    public void importSequences(SequenceInfos sequenceInfos) throws SQLException {
        // insert new sequences
        insertSequences(sequenceInfos);
        insertEntities(sequenceInfos);
    }

    /**
     * This method inserts "dma.sequenceTargetTable" (e.g.bio_sequence) records for all sequences in sequenceInfos
     *
     * @param sequenceInfos the parsed sequences from fasta file
     * @throws SQLException
     */
    private void insertSequences(SequenceInfos sequenceInfos) throws SQLException {
        PerfStats.start(PerfStats.KEY_INSERT_SEQUENCES);
        if (sequenceInfos.getImportSequenceInfos().size() == 0) {
            return;
        }
//        long id = GuiBlockGenerator.getSequenceGuidBlockLowerLimit(sequenceInfos.getSequencesWithoutIds());
//        System.out.println("sequenceInfos.getSequencesWithoutIds()="+sequenceInfos.getSequencesWithoutIds() + " sequenceInfos.getImportSequenceInfos().values()=");
        Iterator<Long> idsIter = TimebasedIdentifierGenerator.generateIdList(sequenceInfos.getSequencesWithoutIds()).iterator();
        for (SequenceInfo seqInfo : sequenceInfos.getImportSequenceInfos().values()) {
            if (seqInfo.getSeqId() < 1) {
                seqInfo.setSeqId(idsIter.next());
            }
            insertSequencesPstmt.setLong(1, seqInfo.getSeqId());
            insertSequencesPstmt.setInt(2, seqInfo.getSeqTypeCode());
            insertSequencesPstmt.setString(3, seqInfo.getSequence());
            if (GENERATE_MD5) {
                insertSequencesPstmt.setString(4, seqInfo.getSequence());
                insertSequencesPstmt.setInt(5, EXTERNAL_SOURCE_ID);
            }
            else {
                insertSequencesPstmt.setInt(4, EXTERNAL_SOURCE_ID);
            }
            insertSequencesPstmt.addBatch();
        }
        insertSequencesPstmt.executeBatch();
        insertSequencesPstmt.clearParameters();
        insertSequencesPstmt.clearBatch();
        dmaLogger.addSequenceInserts(sequenceInfos.getImportSequenceInfos().size());
        PerfStats.end(PerfStats.KEY_INSERT_SEQUENCES);
    }

    /**
     * This method inserts "dma.entityTargetTable" (e.g. sequence_entity) records for all sequences in sequenceInfos
     *
     * @param sequenceInfos the parsed sequences from fasta file
     * @throws SQLException
     */
    private void insertEntities(SequenceInfos sequenceInfos) throws SQLException {
        PerfStats.start(PerfStats.KEY_INSERT_SEQ_ENTITIES);
        if (sequenceInfos.getImportSequenceInfos().size() == 0) {
            return;
        }
//        long id = GuiBlockGenerator.getEntityGuidBlockLowerLimit(sequenceInfos.getEntitiesWithoutIds());
        Iterator<Long> idsIter = TimebasedIdentifierGenerator.generateIdList(sequenceInfos.getEntitiesWithoutIds()).iterator();
        for (SequenceInfo seqInfo : sequenceInfos.getImportSequenceInfos().values()) {
            if (seqInfo.getEntityId() < 1) {
                seqInfo.setEntityId(idsIter.next());
            }
            insertEntitiesPstmt.setLong(1, seqInfo.getEntityId());
            insertEntitiesPstmt.setString(2, seqInfo.getCameraAcc());
            insertEntitiesPstmt.setString(3, seqInfo.getDefline());
            insertEntitiesPstmt.setLong(4, seqInfo.getSeqId());
            insertEntitiesPstmt.setInt(5, seqInfo.getSeqLength());
            insertEntitiesPstmt.setInt(6, seqInfo.getEntityTypeCode());
            insertEntitiesPstmt.setString(7, seqInfo.getExternalAcc());
            if (seqInfo.getGiNumber() > 0) {
                insertEntitiesPstmt.setInt(8, seqInfo.getGiNumber());
            }
            else {
                insertEntitiesPstmt.setObject(8, null);
            }
//            insertEntitiesPstmt.setString(9, seqInfo.getDescription());
            insertEntitiesPstmt.setString(9, null);
            insertEntitiesPstmt.setString(10, seqInfo.getOrganism());
            insertEntitiesPstmt.setInt(11, seqInfo.getTaxonId());
            insertEntitiesPstmt.setString(12, seqInfo.getAssemblyAcc());
            insertEntitiesPstmt.setInt(13, EXTERNAL_SOURCE_ID);
            insertEntitiesPstmt.setLong(14, getExternalDataOwner());
            insertEntitiesPstmt.setString(15, seqInfo.getExternalSource());
            // Seems like there's a bug in postgres driver
            // Numeric types get persisted as 0 instead of null if they're not explicitly set
            insertEntitiesPstmt.setObject(16, null);
            insertEntitiesPstmt.setObject(17, null);
            insertEntitiesPstmt.setObject(18, null);
            insertEntitiesPstmt.setObject(19, null);
            insertEntitiesPstmt.setObject(20, null);
            insertEntitiesPstmt.setObject(21, null);
            insertEntitiesPstmt.setObject(22, null);
            insertEntitiesPstmt.setObject(23, null);
            insertEntitiesPstmt.setObject(24, null);
            insertEntitiesPstmt.setObject(25, null);
            insertEntitiesPstmt.setObject(26, null);
            insertEntitiesPstmt.setObject(27, null);
            insertEntitiesPstmt.setObject(28, null);
            insertEntitiesPstmt.setObject(29, null);
            insertEntitiesPstmt.setObject(30, null);
            insertEntitiesPstmt.setObject(31, null);
            insertEntitiesPstmt.setObject(32, null);
            insertEntitiesPstmt.setObject(33, null);
            insertEntitiesPstmt.setObject(34, null);
            insertEntitiesPstmt.setObject(35, null);
            insertEntitiesPstmt.addBatch();
        }
        // insert new entities
        insertEntitiesPstmt.executeBatch();
        insertEntitiesPstmt.clearParameters();
        insertEntitiesPstmt.clearBatch();
        dmaLogger.addEntityInserts(sequenceInfos.getImportSequenceInfos().size());
        PerfStats.end(PerfStats.KEY_INSERT_SEQ_ENTITIES);
    }

    private long getExternalDataOwner() {
        return 60; //todo make dynamic
    }


}
