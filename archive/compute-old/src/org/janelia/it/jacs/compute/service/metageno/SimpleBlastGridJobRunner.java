
package org.janelia.it.jacs.compute.service.metageno;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Apr 22, 2009
 * Time: 10:10:40 AM
 */
public class SimpleBlastGridJobRunner extends SimpleGridJobRunner {
    public SimpleBlastGridJobRunner(File workingDir, String command, String queue, String projectCode, int timeoutMinutes, int maxRetry, Long taskId) throws Exception {
        super(workingDir, command, queue, projectCode, timeoutMinutes, maxRetry, taskId);
    }

    protected void checkResults() throws Exception {
        File stderrFile = new File(scriptFile.getAbsolutePath() + ".err");
        if (stderrFile.exists() && stderrFile.length() > 0L) {
            BufferedReader reader = new BufferedReader(new FileReader(stderrFile));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().startsWith("[blastall] WARNING")) {
                    //skip
                }
                else {
                    logger.error("Found legitimate blast error line in file " + stderrFile.getAbsolutePath() + " : " + line);
                    reader.close();
                    super.checkResults();
                }
            }
        }
    }

}
