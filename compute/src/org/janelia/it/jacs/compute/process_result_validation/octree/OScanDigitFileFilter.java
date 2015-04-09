/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.process_result_validation.octree;

import java.io.File;
import java.io.FileFilter;

/**
 * File filter to keep only the all-digit-named subdirectories.  Said
 * directories are nodes in an octree.
 * 
 * @author fosterl
 */
public class OScanDigitFileFilter implements FileFilter {

    public boolean accept(File candidate) {
        boolean rtnVal = true;
        if (!candidate.isDirectory()) {
            rtnVal = false;
        } else {
            for (char ch : candidate.getName().toCharArray()) {
                if (!Character.isDigit(ch)) {
                    rtnVal = false;
                    break;
                }
            }
        }
        return rtnVal;
    }
}


