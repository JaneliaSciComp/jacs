
package org.janelia.it.jacs.compute.util;

import org.janelia.it.jacs.compute.engine.data.MissingDataException;

import java.io.File;

/**
 * User: aresnick
 * Date: Jul 10, 2009
 * Time: 12:24:59 PM
 * <p/>
 * <p/>
 * Description:
 */
public class SubjectDBUtils {
    public static File getSubjectDBFile(File dbDir, String fileSuffix) throws MissingDataException {
        for (File file : dbDir.listFiles(new FileSuffixFilter(fileSuffix))) {
            return file;
        }

        // if no db file found, throw exception
        throw new MissingDataException(
                "Can't find subject database file with suffix " + fileSuffix
                        + " in subject db directory " + dbDir);
    }
}
