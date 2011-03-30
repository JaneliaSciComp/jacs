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
 * This class is responsible for creating groups of external entity ids for ExternalEntityImporter to process
 * in batches.  The main reason we need this class though is NOT multi-threaded ExternalEntityImporter execution.
 * We need it because we can't hold 120 million entity ids in JDBC resultset.  Moreever, we can't page through
 * 120 million records using (limit/offset) or JDBC maxResults because the queries perform very slow after 500k
 * records
 *
 * @author Tareq Nabeel
 */
public class ExternalEntityIdBatchWriter extends BaseEntityIdBatchWriter {

    private static final String FILE_PREFIX = SystemConfigurationProperties.getString("dma.externalIdsFileExtension");
    private static final int SOURCE_ID = SystemConfigurationProperties.getInt("dma.externalEntitySourceId");
    private static final int BATCH_SIZE = SystemConfigurationProperties.getInt("dma.importExternalEntityBatchSeqCount");
    private static final int NUMBER_OF_MOD_CHUNKS = SystemConfigurationProperties.getInt("dma.externalIdTotalModChunks");
    private static final String SOURCE_ENTTIY_TABLE = SystemConfigurationProperties.getString("dma.externalEntitySourceTable");
    private static final int TOTAL_IDS_PER_FILE = SystemConfigurationProperties.getInt("dma.totalExternalIdsPerBatchFile");


    public static void main(String[] args) throws IOException, ClassNotFoundException, SQLException, InterruptedException {
        ExternalEntityIdBatchWriter batchWriter = new ExternalEntityIdBatchWriter();
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
        return DmaLogger.getInstance().getExternalIdsDir().getAbsolutePath();
    }

    protected String getFilePrefix() {
        return FILE_PREFIX;
    }

    public String getName() {
        return "ExternalIdWriter";
    }

    protected String getSourceEntityTable() {
        return SOURCE_ENTTIY_TABLE;
    }

    protected long getUpperEntityId() {
        if (getDmaArgs().getExternalEntityId() != null) {
            return getDmaArgs().getExternalEntityId();
        }
        else {
            return -1;
        }
    }
}
