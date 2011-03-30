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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

/**
 * This class is responsible for importing entities/sequences from external entity/sequence scratch tables.
 * It uses the output from the ExternalEntityIdBatchWriter (eeid files) to do it's imports
 * using multiple-threads (DmaThreads)
 *
 * @author Tareq Nabeel
 */
public class ExternalEntityImporter extends BaseEntityImporter {

    private static final int SOURCE_ID = SystemConfigurationProperties.getInt("dma.externalEntitySourceId");
    private static final String SOURCE_ENTTIY_TABLE = SystemConfigurationProperties.getString("dma.externalEntitySourceTable");
    private static final String SOURCE_SEQUENCE_TABLE = SystemConfigurationProperties.getString("dma.externalSequenceSourceTable");

    protected int getSourceId() {
        return SOURCE_ID;
    }

    protected boolean getObsoleteFlag(ResultSet sourceDbRs) throws SQLException {
        return true;
    }

    protected String getSourceDbQueryString() {
        return "select se.entity_id, se.camera_acc from " + getSourceEntityTable() + " se";
    }

    protected String getSourceEntityTable() {
        return SOURCE_ENTTIY_TABLE;
    }

    protected String getSourceSequenceTable() {
        return SOURCE_SEQUENCE_TABLE;
    }

    protected ResultSet getSourceDbRsToImport(Statement sourceStmt, String betweenClause) {
        ResultSet sourceDbRs = getSourceDbRs(sourceStmt, betweenClause);
        String query = null;
        try {
            StringBuilder cameraAccsBuff = new StringBuilder("('");
            Set<Long> entityIdsToFetchFromSourceDb = new HashSet<Long>();
            while (sourceDbRs.next()) {
                cameraAccsBuff.append(sourceDbRs.getString("camera_acc"));
                cameraAccsBuff.append("','");
                entityIdsToFetchFromSourceDb.add(sourceDbRs.getLong("entity_id"));
            }
            if (entityIdsToFetchFromSourceDb.size() > 0) {
                cameraAccsBuff.setLength(cameraAccsBuff.length() - 3);
            }
            else {
                throw new IllegalArgumentException("Entity Ids " + betweenClause + " do not exist in source database");
            }
            cameraAccsBuff.append("')");

            Statement findEntitiesStmt = getConn().createStatement();
            query = "select se.entity_id from " + ENTITY_TARGET_TABLE + "  se where se.camera_acc in " + cameraAccsBuff.toString();
            if (dmaLogger.isDebugEnabled(getClass())) {
                dmaLogger.logDebug("Executing query: " + query, getClass());
            }
            ResultSet rs = findEntitiesStmt.executeQuery(query);
            Set<Long> existingEntityIds = new HashSet<Long>();
            while (rs.next()) {
                existingEntityIds.add(rs.getLong("entity_id"));
            }
            entityIdsToFetchFromSourceDb.removeAll(existingEntityIds);
            if (existingEntityIds.size() > 0) {
                if (dmaLogger.isDebugEnabled(getClass())) {
                    dmaLogger.logDebug("\n************************\n" + getName() + " existingEntityIds.size()=" + existingEntityIds.size() + "\n************************\n", getClass());
                }
            }

            StringBuilder entityIdsToFetchBuff = new StringBuilder("(");
            for (Long entityIdToFetch : entityIdsToFetchFromSourceDb) {
                entityIdsToFetchBuff.append(entityIdToFetch);
                entityIdsToFetchBuff.append(",");
            }
            if (entityIdsToFetchFromSourceDb.size() > 0) {
                entityIdsToFetchBuff.setLength(entityIdsToFetchBuff.length() - 1);
            }
            else {
                entityIdsToFetchBuff.append(-1);
            }
            entityIdsToFetchBuff.append(")");
            query = getSourceDbAllEntitySequenceColumnsQueryString() + " where se.entity_id in " + entityIdsToFetchBuff.toString();
            if (dmaLogger.isDebugEnabled(getClass())) {
                dmaLogger.logDebug("Executing query: " + query, getClass());
            }
            ResultSet rsr = sourceStmt.executeQuery(query);
            if (dmaLogger.isDebugEnabled(getClass())) {
                dmaLogger.logDebug("Query: " + query + " completed", getClass());
            }
            return rsr;
        }
        catch (SQLException e) {
            throw new RuntimeException("query=" + query, e);
        }
    }

    @Override
    public long getSeqErrorCount() {
        return -1;
    }
}
