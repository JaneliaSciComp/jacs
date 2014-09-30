/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.api;

import java.io.File;
import java.util.List;
import javax.ejb.Remote;
import org.janelia.it.jacs.shared.geometric_search.GeometricIndexManagerModel;

/**
 *
 * @author murphys
 */
@Remote
public interface GeometricSearchBeanRemote {
    
    public List<GeometricIndexManagerModel> getManagerModel(int maxRecordCountPerScannerSignature) throws ComputeException;
    public List<GeometricIndexManagerModel> getManagerModelForScanner(String scannerSignature) throws ComputeException;
    public Long getModifiedTimestamp(String scannerSignature) throws ComputeException;
    public File getScanDirectory(String scannerSignature) throws ComputeException;

}
