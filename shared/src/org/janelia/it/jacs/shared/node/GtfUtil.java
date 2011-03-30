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
