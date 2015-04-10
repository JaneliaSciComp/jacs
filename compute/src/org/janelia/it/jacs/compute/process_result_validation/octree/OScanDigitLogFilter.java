/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.process_result_validation.octree;

import java.io.File;
import java.io.FileFilter;

/**
 * File filter to keep only the all-digit-named log files.
 * 
 * @author fosterl
 */
public class OScanDigitLogFilter implements FileFilter {

    @Override
    public boolean accept(File candidate) {
        boolean rtnVal = true;
        final String candidateFileName = candidate.getName();
        final int periodIndex = candidateFileName.indexOf('.');
        if (!candidate.isFile()  ||  periodIndex == -1   ||  !candidateFileName.endsWith(".log")) {
            rtnVal = false;
        } else {
            String justFileName = candidateFileName.substring(0, periodIndex);
            for (char ch : justFileName.toCharArray()) {
                if (!Character.isDigit(ch)) {
                    rtnVal = false;
                    break;
                }
            }
        }
        return rtnVal;
    }
}


