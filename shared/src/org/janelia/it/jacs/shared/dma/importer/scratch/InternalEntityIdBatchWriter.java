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
import org.janelia.it.jacs.shared.dma.reporter.DmaLogger;

import java.io.IOException;
import java.sql.SQLException;

/**
 * @author Tareq Nabeel
 */
public class InternalEntityIdBatchWriter extends BaseEntityIdBatchWriter {

    private static final String FILE_PREFIX = SystemConfigurationProperties.getString("dma.internalIdsFileExtension");
    private static final int SOURCE_ID = SystemConfigurationProperties.getInt("dma.internalEntitySourceId");
    private static final int BATCH_SIZE = SystemConfigurationProperties.getInt("dma.importInternalEntityBatchSeqCount");
    private static final int NUMBER_OF_MOD_CHUNKS = SystemConfigurationProperties.getInt("dma.internalIdTotalModChunks");
    private static final String SOURCE_ENTTIY_TABLE = SystemConfigurationProperties.getString("dma.internalEntitySourceTable");
    private static final int TOTAL_IDS_PER_FILE = SystemConfigurationProperties.getInt("dma.totalInternalIdsPerBatchFile");

    public static void main(String[] args) throws IOException, ClassNotFoundException, SQLException, InterruptedException {
        InternalEntityIdBatchWriter batchWriter = new InternalEntityIdBatchWriter();
        batchWriter.execute();
    }

    protected int getNumberOfModChunks() {
        return NUMBER_OF_MOD_CHUNKS;
    }

    protected int getBatchSize() {
        return BATCH_SIZE;
    }

    protected int getSourceId() {
        return SOURCE_ID;
    }

    protected int getTotalRecordsPerFile() {
        return TOTAL_IDS_PER_FILE;
    }

    protected String getOutputDirPath() {
        return DmaLogger.getInstance().getInternalIdsDir().getAbsolutePath();
    }

    protected String getFilePrefix() {
        return FILE_PREFIX;
    }

    public String getName() {
        return "InternalIdWriter";
    }

    protected String getSourceEntityTable() {
        return SOURCE_ENTTIY_TABLE;
    }

    protected long getUpperEntityId() {
//        return 1310004004;
        return 1391925008;
//        return 1434816004;
//        return 1435611004;
//        return 1437600004;
    }

}
