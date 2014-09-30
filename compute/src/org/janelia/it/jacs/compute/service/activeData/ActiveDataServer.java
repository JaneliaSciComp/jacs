/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.service.activeData;

import java.io.File;
import java.util.List;

import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.shared.geometric_search.GeometricIndexManagerModel;

/**
 *
 * @author murphys
 */
public interface ActiveDataServer {

    public List<String> getSignatures() throws Exception;
    
    public List<GeometricIndexManagerModel> getModel(int epochCount) throws Exception;
    
    public List<GeometricIndexManagerModel> getModelForScanner(String signature) throws Exception;

    public Long getModifiedTimestamp(String signature) throws Exception;

    public void spawnPreEpoch(ActiveDataScan scan) throws Exception;

    public void spawnPostEpoch(ActiveDataScan scan) throws Exception;

}
