/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.api;

import java.io.File;
import java.util.List;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.service.activeData.ActiveDataServer;
import org.janelia.it.jacs.compute.service.activeData.ActiveDataServerSimpleLocal;
import org.janelia.it.jacs.shared.geometric_search.GeometricIndexManagerModel;
import org.jboss.annotation.ejb.PoolClass;
import org.jboss.annotation.ejb.TransactionTimeout;
import org.jboss.ejb3.StrictMaxPool;

/**
 *
 * @author murphys
 */
@Stateless(name = "GeometricSearchEJB")
@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
@TransactionTimeout(432000)
//@Interceptors({UsageInterceptor.class})
@PoolClass(value = StrictMaxPool.class, maxSize = 100, timeout = 10000)
public class GeometricSearchBeanImpl implements GeometricSearchBeanLocal, GeometricSearchBeanRemote {
	
    private static final Logger logger = Logger.getLogger(GeometricSearchBeanImpl.class);

    @Override
    public List<GeometricIndexManagerModel> getManagerModel(int maxRecordCountPerScannerSignature) throws ComputeException {
        try {
            ActiveDataServer activeData = ActiveDataServerSimpleLocal.getInstance();
            return activeData.getModel(maxRecordCountPerScannerSignature);
        } catch (Exception ex) {
            throw new ComputeException(ex);
        }
    }

    @Override
    public List<GeometricIndexManagerModel> getManagerModelForScanner(String scannerSignature) throws ComputeException {
       try {
            ActiveDataServer activeData = ActiveDataServerSimpleLocal.getInstance();
            return activeData.getModelForScanner(scannerSignature);
        } catch (Exception ex) {
            throw new ComputeException(ex);
        }
    }

    @Override
    public Long getModifiedTimestamp(String scannerSignature) throws ComputeException {
        try {
            ActiveDataServer activeData = ActiveDataServerSimpleLocal.getInstance();
            return activeData.getModifiedTimestamp(scannerSignature);
        } catch (Exception ex) {
            throw new ComputeException(ex);
        }
    }

    @Override
    public File getScanDirectory(String scannerSignature) throws ComputeException {
        try {
            ActiveDataServerSimpleLocal activeData = (ActiveDataServerSimpleLocal)ActiveDataServerSimpleLocal.getInstance();
            return activeData.getScanDirectory(scannerSignature);
        } catch (Exception ex) {
            throw new ComputeException(ex);
        }
    }


}
