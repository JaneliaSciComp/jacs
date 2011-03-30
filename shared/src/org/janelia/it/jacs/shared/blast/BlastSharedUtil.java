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

package org.janelia.it.jacs.shared.blast;

import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Apr 3, 2008
 * Time: 4:24:37 PM
 */
public class BlastSharedUtil {

    /**
     * This isn't needed at the moment but will become important when we off-load merge to grid and if we were
     * to execute multiple merge operations in parallel.  Of course, writing out to file instead of db is a no-no in a
     * clustered app server environment but we don't have to worry about that in our cluster grid F environment.
     *
     * @param parsedBlastResultCollection - collection object of blast results
     * @param totalBlastHitsFilePath      - path to file
     * @return long of new blast hits
     * @throws IOException - problem accessing the hits file
     */
    public static synchronized long writeTotalBlastHits(ParsedBlastResultCollection parsedBlastResultCollection,
                                                        String totalBlastHitsFilePath) throws IOException {
        // Using RandomAccessFile because it offers read and write lock
        RandomAccessFile totalBlastHitsFile = null;
        FileLock fl = null;
        long newBlastHits;
        try {
            totalBlastHitsFile = new RandomAccessFile(totalBlastHitsFilePath, "rw");
            fl = FileUtil.lockFile(totalBlastHitsFile, totalBlastHitsFilePath, 100);
            long oldBlastHits = retrieveTotalBlastHitsCount(totalBlastHitsFile);
            newBlastHits = oldBlastHits + parsedBlastResultCollection.size();
            totalBlastHitsFile.setLength(0);
            // Used writeUTF to get the most "readable" number
            totalBlastHitsFile.writeUTF(String.valueOf(newBlastHits));
        }
        finally {
            if (fl != null) fl.release();
            if (null != totalBlastHitsFile) {
                totalBlastHitsFile.close(); // releases the lock
            }
        }
        return newBlastHits;
    }

    /**
     * Reads and returns the count from filesystem
     *
     * @param totalBlastHitsFile - file to read
     * @return long value of the number of hits
     * @throws IOException - problem reading the file
     */
    public static synchronized long retrieveTotalBlastHitsCount(RandomAccessFile totalBlastHitsFile) throws IOException {
        long totalBlastHits = 0;
        if (totalBlastHitsFile.length() > 0) {
            totalBlastHits = Long.parseLong(totalBlastHitsFile.readUTF());
        }
        return totalBlastHits;
    }

    public static synchronized void writeLongValueToSharedFile(File file, long value) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        FileLock fl = null;
        try {
            fl = FileUtil.lockFile(raf, file.getAbsolutePath(), 100);
            raf.setLength(0);
            raf.writeUTF(String.valueOf(value));
            fl.release();
        }
        finally {
            if (fl != null) fl.release();
            raf.close();
        }
    }

    public static synchronized void writeLongValueToNewFile(File file, long value) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        try {
            raf.setLength(0);
            raf.writeUTF(String.valueOf(value));
        }
        finally {
            raf.close();
        }
    }

    public static synchronized long readLongValueFromFile(File file) throws IOException {
        long value = 0L;
        if (file.length() > 0) {
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            try {
                value = Long.parseLong(raf.readUTF());
            }
            finally {
                raf.close();
            }
        }
        return value;
    }

}
