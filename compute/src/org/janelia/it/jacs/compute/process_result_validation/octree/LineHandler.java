/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.process_result_validation.octree;

/**
 * Implement this to handle whatever type of line you want from a digit-log.
 * @author fosterl
 */
public interface LineHandler {
    void handleLine( String inline ) throws Exception;
}
