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

package org.janelia.it.jacs.shared.dma.formatdb;

import org.janelia.it.jacs.shared.dma.DmaFile;
import org.janelia.it.jacs.shared.dma.DmaFiles;
import org.janelia.it.jacs.shared.dma.util.ConnPool;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Comparator;

/**
 * This class represents the list of directories where fasta files are written out by
 * BlastDbCreator.
 *
 * @author Tareq Nabeel
 */
public class BlastDBDirs extends DmaFiles {

    protected long retrieveSequenceCount(DmaFile dmafile) {
        return 0;
    }

    public synchronized long getTargetSeqCount() {
        return targetSeqCount;
    }

    public synchronized void setTargetSeqCount(long count) {
        targetSeqCount = count;
    }

    protected void addDmaFileSize(File file) {
//        totalFilesSize += file.length();
    }

    protected DmaFile createDmaFile(File file) {
        return new BlastDBDir(file);
    }

    /**
     * Grab the name and length of the blastable dataset node from the database
     *
     * @param dmaFile the directory where blastable dataset fastas will be written out to
     */
    protected void initDmaFile(DmaFile dmaFile) {
        Connection conn = ConnPool.getConnection();
        try {
            Statement stmt = conn.createStatement();
            String fastaDirPath = dmaFile.getAbsolutePath();
            long datasetNodeId = Long.parseLong(fastaDirPath.substring(fastaDirPath.lastIndexOf(File.separator) + 1));
            ResultSet rs = stmt.executeQuery("select name, length from node where subclass in ('BlastDatabaseFileNode','DownloadableFastaFileNode') and node_id=" + datasetNodeId);
            String name;
            long oldLength;
            if (rs.next()) {
                name = datasetNodeId + " " + rs.getString("name");
                oldLength = rs.getLong("length");
            }
            else {
                throw new IllegalArgumentException("Could not find node id " + datasetNodeId);
            }
            dmaFile.setName(name);
            ((BlastDBDir) dmaFile).setOldLength(oldLength);
//            dmaLogger.logInfo(dmaFile.getName() + " old length="+((BlastDBDir)dmaFile).getOldLength(), getClass());
            stmt.close();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
        finally {
            ConnPool.releaseConnection(conn);
        }
    }

    protected Comparator<DmaFile> createAscSizeComparator() {
        return new BlastDBDirAscSizeComparator();
    }

    protected Comparator<DmaFile> createDscSizeComparator() {
        return new BlastDBDirDescSizeComparator();
    }


    private class BlastDBDirAscSizeComparator implements Comparator<DmaFile> {
        public int compare(DmaFile o1, DmaFile o2) {
            long thisVal = ((BlastDBDir) o2).getOldLength();
            long anotherVal = ((BlastDBDir) o1).getOldLength();
            return (thisVal < anotherVal ? -1 : (thisVal == anotherVal ? 0 : 1));
        }
    }

    private class BlastDBDirDescSizeComparator implements Comparator<DmaFile> {
        public int compare(DmaFile o1, DmaFile o2) {
            long thisVal = ((BlastDBDir) o1).getOldLength();
            long anotherVal = ((BlastDBDir) o2).getOldLength();
            return (thisVal < anotherVal ? -1 : (thisVal == anotherVal ? 0 : 1));
        }
    }

}
