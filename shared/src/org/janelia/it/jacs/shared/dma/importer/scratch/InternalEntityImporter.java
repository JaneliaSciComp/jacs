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

/**
 * This class is responsible for importing entities/sequences from external entity/sequence scratch tables.
 * It uses the output from the InternalEntityIdBatchWriter (eeid files) to do it's imports
 * using multiple-threads (DmaThreads)
 * <p/>
 * NOTE: We're no longer using this class.  Instead we're using InternalEntitySQLImporter and
 * InternalSequenceSQLImporter because unlike ExternalEntityImporter, we don't need to perform
 * any business logic on data.  A blind insert using SQL commands would perform better.
 *
 * @author Tareq Nabeel
 */
public class InternalEntityImporter extends BaseEntityImporter {

    private static final int SOURCE_ID = SystemConfigurationProperties.getInt("dma.internalEntitySourceId");
    private static final String SOURCE_ENTTIY_TABLE = SystemConfigurationProperties.getString("dma.internalEntitySourceTable");
    private static final String SOURCE_SEQUENCE_TABLE = SystemConfigurationProperties.getString("dma.internalSequenceSourceTable");

    protected int getSourceId() {
        return SOURCE_ID;
    }

    protected boolean getObsoleteFlag(ResultSet sourceDbRs) throws SQLException {
        return sourceDbRs.getBoolean("obs_flag");
    }

    protected String getSourceDbQueryString() {
        return getSourceDbAllEntitySequenceColumnsQueryString();
    }

    protected ResultSet getSourceDbRsToImport(Statement sourceStmt, String betweenClause) {
        return getSourceDbRs(sourceStmt, betweenClause);
    }

    protected String getSourceEntityTable() {
        return SOURCE_ENTTIY_TABLE;
    }

    protected String getSourceSequenceTable() {
        return SOURCE_SEQUENCE_TABLE;
    }


    @Override
    public long getSeqErrorCount() {
        return 0;
    }
}
