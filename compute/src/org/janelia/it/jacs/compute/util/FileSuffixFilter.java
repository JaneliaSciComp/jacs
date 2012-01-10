
package org.janelia.it.jacs.compute.util;

import java.io.File;
import java.io.FilenameFilter;

/**
 * User: aresnick
 * Date: Jul 10, 2009
 * Time: 12:28:36 PM
 * <p/>
 * <p/>
 * Description:
 */
class FileSuffixFilter implements FilenameFilter {
    private String fileSuffix;

    public FileSuffixFilter(String fileSuffix) {
        this.fileSuffix = fileSuffix;
    }

    public boolean accept(File dir, String name) {
        return name.endsWith(fileSuffix);
    }
}
