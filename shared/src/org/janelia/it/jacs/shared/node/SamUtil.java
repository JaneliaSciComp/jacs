
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
 * Time: 10:57:08 AM
 */
public class SamUtil {

    public static long countSequencesInSamFile(File samFile, File scratchDir, Logger logger) throws Exception {
        SystemCall sc = new SystemCall(null, scratchDir, logger);
        File countFile = new File(scratchDir, samFile.getName() + ".count");
        String countCmd = "grep -v \"^@\" " + samFile.getAbsolutePath() + " | wc -l > " + countFile.getAbsolutePath();
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
