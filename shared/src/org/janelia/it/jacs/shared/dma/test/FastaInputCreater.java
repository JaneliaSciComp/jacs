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

package org.janelia.it.jacs.shared.dma.test;

import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.genomics.EntityType;
import org.janelia.it.jacs.model.genomics.SequenceType;
import org.janelia.it.jacs.shared.dma.entity.SequenceInfo;
import org.janelia.it.jacs.shared.dma.reporter.DmaLogger;
import org.janelia.it.jacs.shared.dma.util.ConnPool;
import org.janelia.it.jacs.shared.perf.PerfStats;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * This class was temporarily used to create a fasta file based on sample references
 *
 * @author Tareq Nabeel
 */
public class FastaInputCreater {

    private static final String inputDirPath = "C:\\dev\\Camera-0913\\Camera";

    private static int BATCH_SIZE = 15000;
    private static int SEQ_CHARS_PER_LINE = 61;
    private DmaLogger dmaLogger = DmaLogger.getInstance();
    private static final String ENTITY_TARGET_TABLE = SystemConfigurationProperties.getString("dma.entityTargetTable");
    private static final String SEQUENCE_TARGET_TABLE = SystemConfigurationProperties.getString("dma.sequenceTargetTable");

    public static void main(String[] args) throws SQLException, ClassNotFoundException, IOException {
        FastaInputCreater inputCreator = new FastaInputCreater();
        inputCreator.exportFastaInputForSample("JCVI_SMPL_1103283000058", "other1:read,other2:GOS,other3:move858");
    }

    private void exportFastaInputForSample(String sampleAcc, String otherTags) throws SQLException, ClassNotFoundException, IOException {
        PerfStats.start("exportFastaInputForSample");
        int nextRow = 1;
        Connection conn = ConnPool.getConnection();
        PreparedStatement pstmt = conn.prepareStatement("select se.camera_acc, se.sequence_length, se.entity_type_code, se.locus, se.external_acc, se.ncbi_gi_number, se.comment, se.organism, se.taxon_id, bs.sequence,bs.sequence_type_code \n" +
                "from " + ENTITY_TARGET_TABLE + "  se\n" +
                "inner join " + SEQUENCE_TARGET_TABLE + "  bs on se.sequence_id=bs.sequence_id\n" +
                "where se.sample_acc=? limit ? offset ?", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        BufferedWriter writer = new BufferedWriter(new FileWriter(FileUtil.createFile(inputDirPath, sampleAcc + ".fasta")));
        while (true) {
            pstmt.clearParameters();
            pstmt.setString(1, sampleAcc);
            pstmt.setInt(2, BATCH_SIZE);
            pstmt.setInt(3, nextRow);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                nextRow = processRecord(writer, rs, nextRow, otherTags);
            }
            else {
                break;
            }
            while (rs.next()) {
                nextRow = processRecord(writer, rs, nextRow, otherTags);
            }
            if (dmaLogger.isInfoEnabled(getClass())) {
                dmaLogger.logInfo("RecordCount: " + nextRow, getClass());
            }
        }
        pstmt.close();
        ConnPool.releaseConnection(conn);
        writer.close();
        if (dmaLogger.isInfoEnabled(getClass())) {
            dmaLogger.logInfo("Processed " + nextRow + " records", getClass());
        }
        PerfStats.end("exportFastaInputForSample");
        dmaLogger.logInfo(PerfStats.getStatsByValues(), getClass());
    }

    private int processRecord(BufferedWriter writer, ResultSet rs, int count, String otherTags) throws SQLException, IOException {
        writer.write(">");
        writer.write(rs.getString("camera_acc"));
        writer.write("/ACCESSION=");
        writeStringValue(writer, rs, "external_acc");
        writer.write("/GI=");
        writeStringValue(writer, rs, "ncbi_gi_number");
        writer.write("/ORGANISM=");
        writeStringValue(writer, rs, "organism");
        writer.write("/LOCUS=");
        writeStringValue(writer, rs, "locus");
        writer.write("/TISSUE=");
//        writeStringValue(writer,rs,"tissue");
        writer.write("/TAXON_ID=");
        writeStringValue(writer, rs, "taxon_id");
        writer.write("/DESCRIPTION=");
        writeStringValue(writer, rs, "comment");
        writer.write("/LENGTH=");
        writeStringValue(writer, rs, "sequence_length");
        writer.write("/KEYWORDS=\"");
        writeAssemblyStatusAndDataType(writer, rs.getInt("entity_type_code"), rs.getInt("sequence_type_code"));
        writer.write(", taxon_group:");
        writer.write(", project:,");
        writer.write(otherTags);
        writer.write("\"");
        writer.newLine();
        String sequence = rs.getString("sequence");
        for (int i = 0; i < sequence.length(); i++) {
            if (i % SEQ_CHARS_PER_LINE == 0 && i > 0) {
                writer.newLine();
            }
            writer.write(sequence.charAt(i));
        }
        writer.newLine();
        return ++count;
    }

    private void writeAssemblyStatusAndDataType(BufferedWriter writer, int entityTypeCode, int sequenceTypeCode) throws IOException {
        String assemblyStatus = "";
        String dataType = null;
        switch (sequenceTypeCode) {
            case SequenceType.SEQTYPE_CODE_NUCLEIC_ACID:
                dataType = SequenceInfo.DATA_TYPE_GENOMIC;
                switch (entityTypeCode) {
                    case EntityType.ENTITY_CODE_NUCLEOTIDE:
                        break;
                    case EntityType.ENTITY_CODE_CHROMOSOME:
                        assemblyStatus = SequenceInfo.ASSEMBLY_STATUS_FINISHED;
                        break;
                    case EntityType.ENTITY_CODE_SCAFFOLD:
                        assemblyStatus = SequenceInfo.ASSEMBLY_STATUS_DRAFT;
                        break;
                }
                break;
            case SequenceType.SEQTYPE_CODE_AMINO_ACID:
                dataType = SequenceInfo.DATA_TYPE_PROTEIN;
                break;
            default:
        }
        writer.write("assembly_status:");
        writer.write(assemblyStatus);
        writer.write(", data_type:");
        writer.write(dataType);
    }

    private void writeStringValue(BufferedWriter writer, ResultSet rs, String columnName) throws SQLException, IOException {
        String val = rs.getString(columnName);
        if (val != null) {
            writer.write(val);
        }
    }

    private void writeIntValue(BufferedWriter writer, ResultSet rs, String columnName) throws SQLException, IOException {
        Object val = rs.getObject(columnName);
        if (val != null) {
            writer.write((Integer) val);
        }
    }
}
