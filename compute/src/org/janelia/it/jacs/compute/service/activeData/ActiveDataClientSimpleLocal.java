/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.service.activeData;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.janelia.it.jacs.shared.geometric_search.GeometricIndexManagerModel;

/**
 *
 * @author murphys
 */
public class ActiveDataClientSimpleLocal implements ActiveDataClient {
    
    ActiveDataServerSimpleLocal server = (ActiveDataServerSimpleLocal)ActiveDataServerSimpleLocal.getInstance();

    @Override
    public ActiveDataRegistration registerScanner(String signature) throws Exception {
        return server.registerScanner(signature);
    }

    @Override
    public Long getNext(String signature) throws Exception {
        return server.getNext(signature);
    }

    @Override
    public ActiveDataScanStatus getScanStatus(String signature) throws Exception {
        return server.getScanStatus(signature);
    }

    @Override
    public int getEntityStatus(String signature, long entityId) throws Exception {
        return server.getEntityStatus(signature, entityId);
    }

    @Override
    public void setEntityStatus(String signature, long entityId, int statusCode) throws Exception {
        server.setEntityStatus(signature, entityId, statusCode);
    }

    @Override
    public void addEntityEvent(String signature, long entityId, String eventDescriptor) throws Exception {
        server.addEntityEvent(signature, entityId, eventDescriptor);
    }

    @Override
    public List<ActiveDataEntityEvent> getEntityEvents(String signature, long entityId) throws Exception {
        return server.getEntityEvents(signature, entityId);
    }

    @Override
    public void clearEntityEvents(String signature, long entityId) throws Exception {
        server.clearEntityEvents(signature, entityId);
    }

    @Override
    public void injectEntity(long entityId) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<ScanEpochRecord> getEpochHistory(String signature) throws Exception {
        return server.getEpochHistory(signature);
    }
    
    @Override
    public void advanceEpoch(String signature) throws Exception {
        server.advanceEpoch(signature);
    }

    @Override
    public File getScanDirectory(String scannerSignature) throws Exception {
        return server.getScanDirectory(scannerSignature);
    }

    @Override
    public void lock(String lockString) throws Exception {
        server.lock(lockString);
    }

    @Override
    public void release(String lockString) throws Exception {
        server.release(lockString);
    }

    @Override
    public Map<Long, List<ActiveDataEntityEvent>> getEventMap(String signature) { return server.getEventMap(signature); }


}
