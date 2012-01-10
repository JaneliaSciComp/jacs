
package org.janelia.it.jacs.web.gwt.common.client.util;

/**
 */
public class FileSizeUtils {
    private static final String[] MILL_ABBREV = new String[]{"B", "KB", "MB", "GB"};
    private static final double BYTES_PER_MEGABYTE = 1024 * 1024L;

    /**
     * Returns the abbreviated file size in B, KB, MB or GB
     *
     * @param size file size
     * @return string of appreviated file size
     */
    public static String abbreviateFileSize(long size) {
        String suffixMill = "B";
        double returnSize = size;
        for (int i = 0; i < 4; i++) {
            // Test: is this mantissa a presentable size?
            if (returnSize < 1000) {
                suffixMill = MILL_ABBREV[i];
                break;
            }

            // Iteratively reduce the mantissa by 1000.
            returnSize /= 1000;
        }

        String returnSizeStr = "" + returnSize;
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < 2; i++) {
            char theChar = returnSizeStr.charAt(i);
            buf.append(theChar);
            // Include one character after the period, if encountered.
            if (theChar == '.') {
                buf.append(returnSizeStr.charAt(i + 1));
            }
        }
        if (returnSize > 100) {
            buf.append("0");
        }

        return buf.toString() + " " + suffixMill;
    }

    /**
     * Abbreviates the size in MB, with 1 fixed decimal place.  Value returned is "<0.1MB" if value is < 1.MB.
     */
    public static String abbreviateFileSizeMB(long size) {
        double sizeMB = (double) size / BYTES_PER_MEGABYTE;
        double stripped = ((int) (sizeMB * 10)) / (double) 10;

        String sizeString = stripped + " MB";

        // Special cases - add comma if > 3 digits, or "<" if < 0.1
        if (stripped > 1000.0) // can't be > 10,000 since 10,000MB = 10GB, which is larger than long
            sizeString = sizeString.charAt(0) + "," + sizeString.substring(1);
        else if (stripped < 0.1)
            sizeString = "<0.1 MB";

        return sizeString;
    }
}
