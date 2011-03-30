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

import org.janelia.it.jacs.shared.dma.DmaAction;
import org.janelia.it.jacs.shared.dma.DmaArgs;
import org.janelia.it.jacs.shared.dma.DmaFile;
import org.janelia.it.jacs.shared.dma.entity.EntityIdRange;
import org.janelia.it.jacs.shared.dma.entity.MutableLong;
import org.janelia.it.jacs.shared.dma.reporter.DmaLogger;
import org.janelia.it.jacs.shared.dma.reporter.Progress;
import org.janelia.it.jacs.shared.dma.reporter.ProgressCapturer;
import org.janelia.it.jacs.shared.dma.util.ConnPool;
import org.janelia.it.jacs.shared.dma.util.IdRangeWriter;
import org.janelia.it.jacs.shared.perf.PerfStats;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is responsible for creating groups of entity ids for BaseEntityImporter to process in batches.  The main
 * reason we need this class though is NOT multi-threaded BaseEntityImporter execution.  We need it because
 * we can't hold 120 million entity ids in JDBC resultset.  Moreever, we can't page through 120 million records
 * using (limit/offset) or JDBC maxResults because the queries perform very slow after 500k records
 *
 * @author Tareq Nabeel
 */
public abstract class BaseEntityIdBatchWriter implements DmaAction {

    private DmaLogger dmaLogger = DmaLogger.getInstance();
    private MutableLong processedRecords = new MutableLong(0);
    private MutableLong entityId = new MutableLong(0);
    private MutableLong startEntityId;
    private Connection conn;
    private ProgressCapturer progressCapturer;
    private long startTimeMillis;
    private MutableLong targetSeqCount = new MutableLong(0);
    private DmaArgs dmaArgs;

    protected abstract int getNumberOfModChunks();

    protected abstract int getBatchSize();

    protected abstract int getSourceId();

    protected abstract int getTotalRecordsPerFile();

    protected abstract String getOutputDirPath();

    protected abstract String getFilePrefix();

    public abstract String getName();

    protected abstract String getSourceEntityTable();

    protected abstract long getUpperEntityId();

    public void execute() {
        startTimeMillis = System.currentTimeMillis();
        try {
            this.conn = ConnPool.getConnection();
            List<EntityIdRange> entityIdChunkRanges = getEntityIdChunks();
            IdRangeWriter.writeEntities(entityIdChunkRanges, processedRecords, entityId, getOutputDirPath(), getFilePrefix(), getTotalRecordsPerFile(), getBatchSize());
            ConnPool.releaseConnection(conn);
        }
        catch (Exception e) {
            throw new RuntimeException("BaseEntityIdBatchWriter createFiles failed at processedRecord " + processedRecords, e);
        }
    }

    private List<EntityIdRange> getEntityIdChunks() throws SQLException {
        PerfStats.start(getClass() + ".getEntityIdChunks");
        Statement sourceStmt = conn.createStatement();
        long upperEntityId = getUpperEntityId();
        if (upperEntityId > 0) {

        }
        String getChunksQuery = "select min(entity_id) as min_id, max(entity_id) as max_id, trunc((entity_id-1)::float/" + getNumberOfModChunks() + ".) as blk_id, count(entity_id) as num_id from " + getSourceEntityTable() + (getUpperEntityId() > 0 ? " where entity_id >=" + getUpperEntityId() : "") + " group by blk_id order by min_id";
        dmaLogger.logInfo("Executing query:  " + getChunksQuery, getClass());
        ResultSet sourceDbChunksRs = sourceStmt.executeQuery(getChunksQuery);
        List<EntityIdRange> entityIdChunkRanges = new ArrayList<EntityIdRange>();
        while (sourceDbChunksRs.next()) {
            long startId = sourceDbChunksRs.getLong("min_id");
            long endId = sourceDbChunksRs.getLong("max_id");
            int count = sourceDbChunksRs.getInt("num_id");
            entityIdChunkRanges.add(new EntityIdRange(startId, endId, count));
            if (targetSeqCount.getValue() <= 1) {
                startEntityId = new MutableLong(startId);
            }
            targetSeqCount.add(count);
        }
        sourceStmt.close();
        PerfStats.end(getClass() + ".getEntityIdChunks");
        PerfStats.printStatsToStdOut();
        PerfStats.clear();
        return entityIdChunkRanges;
    }

    public Progress getProgress() {
        return progressCapturer.capture();
    }

    public MutableLong getProcessedRecords() {
        return processedRecords;
    }

    public MutableLong getEntityId() {
        return entityId;
    }

    public MutableLong getStartEntityId() {
        return startEntityId;
    }

    public MutableLong getTotalSeqCount() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public long getProcessedSeqCount() {
        if (processedRecords != null) {
            return processedRecords.getValue();
        }
        else {
            return -1;
        }
    }

    public long getProcessedByteCount() {
        return 0;
    }

    public long getTargetSeqCount() {
        return targetSeqCount.getValue();
    }

    public long getTargetByteCount() {
        return 0;
    }

    public long getSeqErrorCount() {
        return 0;
    }

    public void setProgressCapturer(ProgressCapturer progressCapturer) {
        this.progressCapturer = progressCapturer;
    }

    public long getStartTime() {
        return startTimeMillis;
    }


    public void setDmaFile(DmaFile dmaFile) {

    }

    public DmaFile getDmaFile() {
        return null;
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
}
