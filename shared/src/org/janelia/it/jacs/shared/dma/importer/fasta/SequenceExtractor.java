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

import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.shared.dma.DmaArgs;
import org.janelia.it.jacs.shared.dma.DmaFile;
import org.janelia.it.jacs.shared.dma.entity.MutableLong;
import org.janelia.it.jacs.shared.dma.entity.SequenceInfo;
import org.janelia.it.jacs.shared.dma.entity.SequenceInfos;
import org.janelia.it.jacs.shared.dma.reporter.DmaLogger;
import org.janelia.it.jacs.shared.perf.PerfStats;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

/**
 * This class is responsible for parsing a fasta file and creating Sequence Info objects for
 * the fasta package importers
 *
 * @author Tareq Nabeel
 */
public class SequenceExtractor {

    private static final int BATCH_SEQ_CHAR_COUNT = SystemConfigurationProperties.getInt("dma.fastaBatchSeqCharCount");
    private static final int BATCH_SEQ_COUNT = SystemConfigurationProperties.getInt("dma.fastaBatchSeqCount");
    private static final int READ_AHEAD_LIMIT = SystemConfigurationProperties.getInt("dma.readAheadLimit");
    private static final String SOURCE_ENTTIY_TABLE = SystemConfigurationProperties.getString("dma.externalEntitySourceTable");
    private static final String SOURCE_SEQUENCE_TABLE = SystemConfigurationProperties.getString("dma.externalSequenceSourceTable");

    private ReaderState readerState;
    private DmaFile inputFastaFile;
    private BufferedReader reader;
    private Connection conn;
    private DmaArgs dmaArgs;
    private DmaLogger dmaLogger = DmaLogger.getInstance();
    private static final String ENTITY_TARGET_TABLE = SystemConfigurationProperties.getString("dma.entityTargetTable");
//    private static final String SEQUENCE_TARGET_TABLE = SystemConfigurationProperties.getString("dma.sequenceTargetTable");

    public SequenceExtractor(Connection conn, DmaFile inputFastaFile, DmaArgs dmaArgs) throws IOException {
//        dmaLogger.logInfo("Reading fasta file:" + inputFastaFile.getAbsolutePath(),getClass());
        this.inputFastaFile = inputFastaFile;
        reader = createReader();
        readerState = ReaderState.OPEN;
        this.dmaArgs = dmaArgs;
        this.conn = conn;
    }

    public boolean hasMore() {
        return readerState == ReaderState.OPEN;
    }

    public SequenceInfos extractSequenceInfos(MutableLong totalByteCount, MutableLong totalSeqCount, MutableLong totalSeqCharCount, MutableLong sequencesInError) throws IOException, SQLException {
        PerfStats.start(PerfStats.KEY_EXTRACT_SEQUENCE_INFOS);
        SequenceInfos sequenceInfos = new SequenceInfos();
        String line;
        int recordCount = 0;
        long seqCharCount = 0;
        StringBuilder sequenceBuilder = new StringBuilder();
        String defline = null;
        try {
            while (true) {
                PerfStats.start(PerfStats.KEY_READLINE);
                line = reader.readLine();
                PerfStats.end(PerfStats.KEY_READLINE);
                if (line == null) {
                    break;
                }
                if (line.startsWith(">")) {
                    if (recordCount > 0) {
                        addSequenceInfo(defline, sequenceBuilder, sequenceInfos, totalSeqCount, sequencesInError);
                        if (seqCharCount > BATCH_SEQ_CHAR_COUNT || recordCount == BATCH_SEQ_COUNT) {
//                            dmaLogger.logDebug("****Splitting on "+(recordCount == BATCH_SEQ_COUNT ? "record:"+recordCount : "char:"+seqCharCount), getClass());
                            reader.reset();
                            readerState = ReaderState.OPEN;
                            return sequenceInfos;
                        }
                    }
                    defline = line;
                    recordCount++;
                    totalSeqCount.increment();
                }
                else {
                    seqCharCount += line.length();
                    totalSeqCharCount.add(line.length());
                    sequenceBuilder.append(line);
                }
                totalByteCount.add(line.length() + 1); // add 1 for "\n"
                reader.mark(READ_AHEAD_LIMIT);
            }
            addSequenceInfo(defline, sequenceBuilder, sequenceInfos, totalSeqCount, sequencesInError);
            readerState = ReaderState.CLOSED;
        }
        finally {
            if (dmaArgs.checkForExistingEntitiesDuringFastaImport() &&
                    sequenceInfos.getParsedSequenceInfos().size() > 0) {
                setupExistingSequenceInfos(sequenceInfos);
            }
            if (sequenceInfos.getParsedSequenceInfos().size() > 0) {
                assignEntityAndSequenceIds(sequenceInfos);
            }
//            if (sequenceInfos.getParsedAssemblies().size()>0) {
//                setupAssemblies(sequenceInfos);
//            }
            PerfStats.end(PerfStats.KEY_EXTRACT_SEQUENCE_INFOS);
        }

        return sequenceInfos;
    }

    private void addSequenceInfo(String defline, StringBuilder sequenceBuilder, SequenceInfos sequenceInfos, MutableLong totalSeqCount, MutableLong sequencesInError) {
        PerfStats.start(PerfStats.KEY_ADD_SEQUENCE_INFO);
        try {
            SequenceInfo seqInfo = new SequenceInfo(defline, sequenceBuilder.toString());
            sequenceBuilder.setLength(0);
            sequenceInfos.add(seqInfo);
        }
        catch (Exception e) {
            sequencesInError.increment();
            dmaLogger.logError("Encountered exception processing " + this.inputFastaFile.getName() + " at totalSeqCount:" + totalSeqCount, this.getClass(), e);
            dmaLogger.logSequenceInError(inputFastaFile, defline, sequenceBuilder.toString());
        }
        PerfStats.end(PerfStats.KEY_ADD_SEQUENCE_INFO);
    }

    private void assignEntityAndSequenceIds(SequenceInfos sequenceInfos) throws SQLException {
        PerfStats.start(PerfStats.KEY_FETCH_SOURCE_DB_SEQUENCE_INFOS);
        Statement findEntitiesStmt = conn.createStatement();
        ResultSet rs = findEntitiesStmt.executeQuery("select se.entity_id,se.camera_acc,se.external_acc,bs.sequence_id from " + SOURCE_ENTTIY_TABLE + " se inner join " + SOURCE_SEQUENCE_TABLE + " bs on se.sequence_id=bs.sequence_id where se.camera_acc in " + sequenceInfos.getParsedAccessionsStr());
        while (rs.next()) {
            String cameraAcc = rs.getString("camera_acc");
            long entityId = rs.getLong("entity_id");
            long seqId = rs.getLong("sequence_id");
            sequenceInfos.assignImportId(cameraAcc, entityId, seqId);
        }
        PerfStats.end(PerfStats.KEY_FETCH_SOURCE_DB_SEQUENCE_INFOS);
    }

    private void setupExistingSequenceInfos(SequenceInfos sequenceInfos) throws SQLException {
        PerfStats.start(PerfStats.KEY_SETUP_EXISTING_SEQUENCE_INFOS);

        Statement findEntitiesStmt = conn.createStatement();
        ResultSet rs = findEntitiesStmt.executeQuery("select se.entity_id,se.camera_acc,se.external_acc from " + ENTITY_TARGET_TABLE + "  se where se.camera_acc in " + sequenceInfos.getParsedAccessionsStr());
        Set<SequenceInfo> existingInfos = new HashSet<SequenceInfo>();
        while (rs.next()) {
            SequenceInfo seqInfo = new SequenceInfo(rs.getLong("entity_id"), rs.getString("camera_acc"));
            existingInfos.add(seqInfo);
        }
        sequenceInfos.filterOutImportSequenceInfos(existingInfos);

        sequenceInfos.filterOutExistingSequenceInfos(sequenceInfos.getImportSequenceInfos().values());

        // Set entityId for existing sequence infos that were parsed.
        for (SequenceInfo info : existingInfos) {
            SequenceInfo seqInfo = sequenceInfos.getExistingSequenceInfos().get(info.getCameraAcc());
            seqInfo.setEntityId(info.getEntityId());
        }
        PerfStats.end(PerfStats.KEY_SETUP_EXISTING_SEQUENCE_INFOS);
    }

//    private void setupAssemblies(SequenceInfos sequenceInfos) throws SQLException {
//        PerfStats.start(PerfStats.KEY_SETUP_ASSEMBLIES);
//        Statement findEntitiesStmt = targetConn.createStatement();
//        String sql = "SELECT assembly_id, description, assembly_acc, organism, taxon_id, sample_acc, status FROM assembly where assembly_acc in " + sequenceInfos.getParsedAssemblyAccessionsStr();
//        ResultSet rs = findEntitiesStmt.executeQuery(sql);
//        Set<Assembly> existingAssemblies = new HashSet<Assembly>();
//        while (rs.next()) {
//            Assembly assembly = new Assembly();
//            assembly.setAssemblyId(rs.getLong("assembly_id"));
//            assembly.setDescription(rs.getString("description"));
//            assembly.setAssemblyAcc(rs.getString("assembly_acc"));
//            assembly.setOrganism(rs.getString("organism"));
//            assembly.setTaxonId(rs.getInt("taxon_id"));
//            assembly.setSampleAcc(rs.getString("sample_acc"));
//            assembly.setStatus(rs.getString("status"));
//            existingAssemblies.add(assembly);
//        }
//        sequenceInfos.filterOutImportAssemblies(existingAssemblies);
//
//        sequenceInfos.filterOutUpdateAssemblies(sequenceInfos.getImportAssemblies().values());
//
//        for (Assembly existingAssembly : existingAssemblies) {
//            Assembly updateAssembly = sequenceInfos.getUpdateAssemblies().get(existingAssembly.getAssemblyAcc());
//            if (updateAssembly.getOrganism().equals(existingAssembly.getOrganism())) {
//               sequenceInfos.getUpdateAssemblies().remove(updateAssembly.getAssemblyAcc());
//            } else {
//                if (existingAssembly.getAlternateOrgNames()!=null) {
//                    // Doing like or contain searches is not sufficient as ",xx" would match ",xxx"
//                   String[] alternateOrgNames = existingAssembly.getAlternateOrgNames().split(",");
//                   int idx = Arrays.binarySearch(alternateOrgNames,updateAssembly.getAssemblyAcc());
//                    if (idx!=-1) {
//                        // organism name already exists but as an alternate name
//                        sequenceInfos.getUpdateAssemblies().remove(updateAssembly.getAssemblyAcc());
//                    } else {
//                        // organism name does not exist; we need to update this existing assembly
//                        // and add the parsed organism name as an alternate organism name
//                        String newAlternateOrgNames = existingAssembly.getAlternateOrgNames() + ","+ updateAssembly.getAssemblyAcc();
//                        updateAssembly.setAlternateOrgNames(newAlternateOrgNames);
//                        updateAssembly.setAssemblyId(existingAssembly.getAssemblyId());
//                    }
//                } else {
//                     // organism name does not exist; we need to update this existing assembly
//                    // and add the parsed organism name as an alternate organism name
//                    updateAssembly.setAlternateOrgNames(updateAssembly.getAssemblyAcc());
//                    updateAssembly.setAssemblyId(existingAssembly.getAssemblyId());
//                }
//            }
//        }
//        PerfStats.end(PerfStats.KEY_SETUP_ASSEMBLIES);
//    }

    private BufferedReader createReader() throws IOException {
        return new BufferedReader(new FileReader(inputFastaFile.getFile()), READ_AHEAD_LIMIT);
    }

    private enum ReaderState {
        OPEN, CLOSED
    }


//    private void createSubset(int records) throws IOException {
//        BufferedReader reader = createReader();
//        BufferedWriter writer = new BufferedWriter(new FileWriter(FileUtil.ensureFileExists(inputFastaFile.getAbsolutePath() + records)));
//        String line;
//        MutableLong totalRecordCount = new MutableLong();
//        while ((line = reader.readLine()) != null) {
//            if (line.startsWith(">")) {
//                if (totalRecordCount.getValue() >= records) {
//                    break;
//                }
//                totalRecordCount.increment();
//            }
//            writer.write(line);
//            writer.newLine();
//        }
//        writer.close();
//        reader.close();
////        System.out.println("totalRecordCount: " + totalRecordCount);
//    }
}
