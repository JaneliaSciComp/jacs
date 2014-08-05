/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.service.activeData;

import java.util.List;
import org.janelia.it.jacs.shared.geometricSearch.GeometricIndexManagerModel;

/**
 *
 * @author murphys
 */
public interface ActiveDataServer {

    public List<String> getSignatures() throws Exception;
    
    public List<GeometricIndexManagerModel> getModel(int epochCount) throws Exception;
    
    public List<GeometricIndexManagerModel> getModelForScanner(String signature) throws Exception;

}
