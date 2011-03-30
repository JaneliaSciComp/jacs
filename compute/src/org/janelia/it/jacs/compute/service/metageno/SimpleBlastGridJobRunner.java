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
