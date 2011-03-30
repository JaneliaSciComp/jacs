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

import org.janelia.it.jacs.shared.dma.DmaAction;
import org.janelia.it.jacs.shared.dma.DmaArgs;
import org.janelia.it.jacs.shared.dma.DmaFile;
import org.janelia.it.jacs.shared.dma.entity.MutableLong;
import org.janelia.it.jacs.shared.dma.entity.SequenceInfos;
import org.janelia.it.jacs.shared.dma.reporter.DmaLogger;
import org.janelia.it.jacs.shared.dma.reporter.Progress;
import org.janelia.it.jacs.shared.dma.reporter.ProgressCapturer;
import org.janelia.it.jacs.shared.dma.util.ConnPool;
import org.janelia.it.jacs.shared.perf.PerfStats;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * This class is responsible for importing fasta data into the system
 *
 * @author Tareq Nabeel
 */
public class FastaImporter implements DmaAction {

    private Connection conn = null;
    private DmaFile inputFastaFile;
    private MutableLong processedByteCount = new MutableLong();
    private MutableLong processedSeqCount = new MutableLong();
    private MutableLong processedSeqCharCount = new MutableLong();
    private MutableLong seqErrorCount = new MutableLong();
    private SequenceExtractor sequenceExtractor;
    private SequenceImporter sequenceImporter;
    private long startTimeMillis;
    private DmaLogger dmaLogger = DmaLogger.getInstance();
    private DmaArgs dmaArgs;

    private ProgressCapturer progressCapturer;

    public FastaImporter() {
    }

    private void init() {
        PerfStats.start(PerfStats.KEY_INIT);
        try {
            sequenceExtractor = new SequenceExtractor(conn, inputFastaFile, dmaArgs);
            sequenceImporter = new SequenceImporter(conn);
            startTimeMillis = System.currentTimeMillis();
        }
        catch (Exception e) {
            dmaLogger.logError("Initializing importer " + getName() + " encountered exception", getClass(), e);
            throw new RuntimeException(e);
        }
        finally {
            PerfStats.end(PerfStats.KEY_INIT);
        }

    }

    public void importData() throws SQLException {
        SequenceInfos parsedSequenceInfos = null;
        conn = ConnPool.getConnection();
        conn.setAutoCommit(false);
        init();
        do {
            try {
                parsedSequenceInfos = extractSequenceInfos();

                // Don't need this right now.  Will need it when we have new tags in panda
//                importNewTags(parsedSequenceInfos);

                importNewSequences(parsedSequenceInfos);

                // Don't need this right now.  Will need it again when we have tags
                // that don't follow the "select entity_id from dma_tag_entity where tag_id=this.id" approach
//                createTagLinksForNewSequences(parsedSequenceInfos);

                importAssemblies(parsedSequenceInfos);

                conn.commit();
            }
            catch (SQLException e) {
                handleSQLException(e, parsedSequenceInfos);
            }
            catch (Exception e) {
                handleException(e, parsedSequenceInfos);
            }

        }
        while (sequenceExtractor.hasMore());

        ConnPool.releaseConnection(conn);
    }

    public Progress getProgress() {
        return progressCapturer.capture();
    }

    private SequenceInfos extractSequenceInfos() throws IOException, SQLException {
        return sequenceExtractor.extractSequenceInfos(processedByteCount, processedSeqCount, processedSeqCharCount, seqErrorCount);
    }

    private void importNewSequences(SequenceInfos parsedSequenceInfos) throws SQLException {
        sequenceImporter.importSequences(parsedSequenceInfos);
    }

    private void importAssemblies(SequenceInfos parsedSequenceInfos) throws SQLException {
        AssemblyImporter.importAssemblies(conn, parsedSequenceInfos);
    }

    private void handleSQLException(SQLException e, SequenceInfos parsedSequenceInfos) {
        handleException(e, parsedSequenceInfos);
        SQLException nextException = e;
        while ((nextException = nextException.getNextException()) != null) {
            dmaLogger.logError("", getClass(), nextException);
        }
    }

    private void handleException(Exception e, SequenceInfos parsedSequenceInfos) {
        dmaLogger.logError("Executing importer " + getName() + " encountered exception at processedSeqCount:" + processedSeqCount + " processedSeqCharCount:" + processedSeqCharCount, this.getClass(), e);
        dmaLogger.logSequencesInError(this.inputFastaFile, parsedSequenceInfos);
        seqErrorCount.add(parsedSequenceInfos.getParsedSequenceInfos().size());
        try {
            conn.rollback();
        }
        catch (SQLException se) {
            se.printStackTrace();
        }
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;

        FastaImporter that = (FastaImporter) o;

        return getName().equals(that.getName());

    }

    public int hashCode() {
        return getName().hashCode();
    }

    public void setDmaFile(DmaFile dmaFile) {
        inputFastaFile = dmaFile;
    }

    public void setConnection(Connection conn) {
        this.conn = conn;
    }


    public void setProgressCapturer(ProgressCapturer capturer) {
        this.progressCapturer = capturer;
    }

    public void execute() {
        try {
            importData();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public long getStartTime() {
        return startTimeMillis;
    }

    public String getName() {
        return inputFastaFile.getName();
    }

    public String getLabel() {
        return getName();
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
        if (processedByteCount != null) {
            return processedByteCount.getValue();
        }
        else {
            return -1;
        }
    }

    public long getTargetSeqCount() {
        return inputFastaFile.getTargetSeqCount();
    }

    public long getTargetByteCount() {
        return inputFastaFile.getJavaFileSize();
    }

    public long getSeqErrorCount() {
        if (seqErrorCount != null) {
            return seqErrorCount.getValue();
        }
        else {
            return -1;
        }
    }

    public DmaFile getDmaFile() {
        return inputFastaFile;
    }


    public void setDmaArgs(DmaArgs dmaArgs) {
        this.dmaArgs = dmaArgs;
    }

    public DmaArgs getDmaArgs() {
        return dmaArgs;
    }

    public String toString() {
        return getName();
    }
}
