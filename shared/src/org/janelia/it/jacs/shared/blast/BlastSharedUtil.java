
package org.janelia.it.jacs.shared.blast;


import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

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
     * clustered app server environment but we don't have to worry about that in our cluster grid environment.
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
//        FileLock fl = null;
        long newBlastHits;
        try {
            totalBlastHitsFile = new RandomAccessFile(totalBlastHitsFilePath, "rw");
//            fl = FileUtil.lockFile(totalBlastHitsFile, totalBlastHitsFilePath, 100);
            long oldBlastHits = retrieveTotalBlastHitsCount(totalBlastHitsFile);
            newBlastHits = oldBlastHits + parsedBlastResultCollection.size();
            totalBlastHitsFile.setLength(0);
            // Used writeUTF to get the most "readable" number
            totalBlastHitsFile.writeUTF(String.valueOf(newBlastHits));
        }
        finally {
//            if (fl != null) fl.release();
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

    public static synchronized void writeLongValueToNewFile(File file, long value) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.setLength(0);
            raf.writeUTF(String.valueOf(value));
        }
    }

    public static synchronized long readLongValueFromFile(File file) throws IOException {
        long value = 0L;
        if (file.length() > 0) {
            try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                value = Long.parseLong(raf.readUTF());
            }
        }
        return value;
    }

}
