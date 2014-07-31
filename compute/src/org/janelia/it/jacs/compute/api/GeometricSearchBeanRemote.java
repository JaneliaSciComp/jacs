/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.api;

import java.util.List;
import javax.ejb.Remote;
import org.janelia.it.jacs.shared.geometricSearch.GeometricIndexManagerModel;

/**
 *
 * @author murphys
 */
@Remote
interface GeometricSearchBeanRemote {
    
    List<GeometricIndexManagerModel> getManagerModel(int maxRecordCountPerScannerSignature) throws ComputeException;
    List<GeometricIndexManagerModel> getManagerModelForScanner(String scannerSignature) throws ComputeException;
    
}
