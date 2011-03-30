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
import org.janelia.it.jacs.model.genomics.Assembly;
import org.janelia.it.jacs.shared.dma.entity.SequenceInfos;
import org.janelia.it.jacs.shared.dma.reporter.DmaLogger;
import org.janelia.it.jacs.shared.dma.util.ConnPool;
import org.janelia.it.jacs.shared.perf.PerfStats;

import java.sql.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This class is responsible for persisting Assembly objects created by FastaImporter
 * into assembly table
 *
 * @author Tareq Nabeel
 */
public class AssemblyImporter {

    private static DmaLogger dmaLogger = DmaLogger.getInstance();

    private static final String INSERT_ASSEMBLIES_STMT = "INSERT INTO assembly (assembly_id, description, " +
            "assembly_acc, taxon_id, status) \n" +
            "VALUES (?, ?, ?, ?, ?)";

    private static Map<String, Assembly> existingAssemblies;
    private static Map<String, Assembly> newAssemblies;
    private static Map<String, Assembly> errorAssemblies;

    static {
        load();
    }

    /**
     * Loads existing assemblies into memory for better performance
     */
    private static void load() {
        PerfStats.start(PerfStats.KEY_LOAD_ALL_TAGS_IN_CACHE);
        Connection conn = ConnPool.getConnection();
        try {
            existingAssemblies = new HashMap<String, Assembly>();
            newAssemblies = new HashMap<String, Assembly>();
            errorAssemblies = new HashMap<String, Assembly>();

            conn.setAutoCommit(false);
            Statement findAllAssemblyStmt = conn.createStatement();
            ResultSet rs = findAllAssemblyStmt.executeQuery("SELECT assembly_id, assembly_acc FROM assembly");
            while (rs.next()) {
                long id = rs.getLong("assembly_id");
                String assemblyAcc = rs.getString("assembly_acc");
                Assembly assembly = new Assembly();
                assembly.setAssemblyId(id);
                assembly.setAssemblyAcc(assemblyAcc);
                existingAssemblies.put(assemblyAcc, assembly);
            }
            dmaLogger.logInfo("existingAssemblies size=" + existingAssemblies.size(), AssemblyImporter.class);
            findAllAssemblyStmt.close();
            ConnPool.releaseConnection(conn);
        }
        catch (SQLException e) {
            throw new ExceptionInInitializerError(e);
        }
        finally {
            ConnPool.releaseConnection(conn);
        }
        PerfStats.end(PerfStats.KEY_LOAD_ALL_TAGS_IN_CACHE);
    }

    /**
     * Imports assemblies that have been parsed by SequenceExtractor
     *
     * @param conn
     * @param sequenceInfos
     * @throws SQLException
     */
    public static synchronized void importAssemblies(Connection conn, SequenceInfos sequenceInfos) throws SQLException {
        // This method is synchronized because the same parsed assembly could exist in sequenceInfos
        // of more than one concurrent thread
        PerfStats.start(PerfStats.KEY_INSERT_ASSEMBLIES);
        try {
            addAssemblies(sequenceInfos);
            newAssemblies.values().removeAll(errorAssemblies.values());
            if (newAssemblies.size() == 0) {
                return;
            }
            PreparedStatement insertAssemblyPstmt = conn.prepareStatement(INSERT_ASSEMBLIES_STMT);
            Iterator<Long> idsIter = TimebasedIdentifierGenerator.generateIdList(newAssemblies.size()).iterator();
            for (Assembly assembly : newAssemblies.values()) {
                assembly.setAssemblyId(idsIter.next());
                insertAssemblyPstmt.setLong(1, assembly.getAssemblyId());
                insertAssemblyPstmt.setString(2, assembly.getDescription());
                insertAssemblyPstmt.setString(3, assembly.getAssemblyAcc());
                insertAssemblyPstmt.setInt(4, assembly.getTaxonId());
                insertAssemblyPstmt.setString(5, assembly.getStatus());
                insertAssemblyPstmt.addBatch();
            }
            insertAssemblyPstmt.executeBatch();
            insertAssemblyPstmt.clearParameters();
            insertAssemblyPstmt.clearBatch();
            releaseUnneededObjects(newAssemblies);
            existingAssemblies.putAll(newAssemblies);
            dmaLogger.addAssemblyInserts(newAssemblies.size());
            newAssemblies.clear();
        }
        catch (SQLException e) {
            errorAssemblies.putAll(newAssemblies);
            throw e;
        }
        PerfStats.end(PerfStats.KEY_INSERT_ASSEMBLIES);
    }

    /**
     * Since number of existing assemblies held in memory is needed only to filter out unncessary
     * imports of parsed assemblies and the size could grow, we want to keep only what we need
     * in memory
     *
     * @param newAssemblies
     */
    private static synchronized void releaseUnneededObjects(Map<String, Assembly> newAssemblies) {
        for (Assembly assembly : newAssemblies.values()) {
            assembly.setDescription(null);
            assembly.setStatus(null);
        }
    }

    /**
     * Add parsed assemblies to newAssemblies if they don't already exist
     *
     * @param sequenceInfos
     */
    private static synchronized void addAssemblies(SequenceInfos sequenceInfos) {
        //  We need to synchronize bececause the same parsed assembly could exist in sequenceInfos
        // of more than one conurrent thread
        for (String assemblyAcc : sequenceInfos.getParsedAssemblies().keySet()) {
            Assembly assembly = existingAssemblies.get(assemblyAcc);
            if (assembly == null) {
                if ((assembly = newAssemblies.get(assemblyAcc)) == null) {
                    assembly = sequenceInfos.getParsedAssemblies().get(assemblyAcc);
                    newAssemblies.put(assembly.getAssemblyAcc(), assembly);
                }
            }
        }
        sequenceInfos.getParsedAssemblies().clear();
    }


}
