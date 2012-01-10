
package org.janelia.it.jacs.shared.node;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 22, 2010
 * Time: 11:24:10 AM
 */
public class GtfUtil {

    public static long countSequencesInGtfFile(File gtfFile, File scratchDir, Logger logger) throws Exception {
        SystemCall sc = new SystemCall(null, scratchDir, logger);
        File countFile = new File(scratchDir, gtfFile.getName() + ".count");
        String countCmd = "cut -f 1 " + gtfFile.getAbsolutePath() + " | sort | uniq | wc -l > " + countFile.getAbsolutePath();
        sc.execute(countCmd, false);
        FileReader fr = new FileReader(countFile);
        BufferedReader br = new BufferedReader(fr);
        String countString;
        try {
            countString = br.readLine();
        }
        finally {
            br.close();
        }
        Long count = new Long(countString.trim());
        sc.cleanup();
        return count;
    }

}
