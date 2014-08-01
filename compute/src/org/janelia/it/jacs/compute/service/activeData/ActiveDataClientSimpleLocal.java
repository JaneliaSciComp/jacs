/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.service.activeData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;

/**
 *
 * @author murphys
 */
public class ActiveDataClientSimpleLocal extends ActiveDataClient implements ActiveDataServer {
    private static Logger logger = Logger.getLogger(ActiveDataClientSimpleLocal.class);
    
    private static final Long LOCK_CHECK_INTERVAL_MS = 5000L; // 5 seconds
    private static final Long LOCK_MAX_WAIT_MS = 1000L * 60L * 60L * 6L; // 6 hours
    
    private static Map<String, ActiveDataScan> scanMap = new HashMap<>();  
    private static Map<String, Long> lockMap = new HashMap<>();

    @Override
    public ActiveDataRegistration registerScanner() throws Exception {
        return registerScanner(entityScanner.getSignature(), entityScanner.getClass().getName());
    }

    @Override
    public Long getNext() throws Exception {
        return getNext(entityScanner.getSignature());
    }

    @Override
    public ActiveDataScanStatus getScanStatus() throws Exception {
        return getScanStatus(entityScanner.getSignature());
    }

    @Override
    public int getEntityStatus(long entityId) throws Exception {
        return getEntityStatus(entityScanner.getSignature(), entityId);
    }

    @Override
    public void setEntityStatus(long entityId, int statusCode) throws Exception {
        setEntityStatus(entityScanner.getSignature(), entityId, statusCode);
    }

    @Override
    public void addEntityEvent(long entityId, String eventDescriptor) throws Exception {
        addEntityEvent(entityScanner.getSignature(), entityId, eventDescriptor);
    }

    @Override
    public List<ActiveDataEntityEvent> getEntityEvents(long entityId) throws Exception {
        return getEntityEvents(entityScanner.getSignature(), entityId);
    }

    @Override
    public void clearEntityEvents(long entityId) throws Exception {
        clearEntityEvents(entityScanner.getSignature(), entityId);
    }

    @Override
    public void injectEntity(long entityId) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Long> getEpochHistory() throws Exception {
        return getEpochHistory(entityScanner.getSignature());
    }
    
    @Override
    public void advanceEpoch() throws Exception {
        advanceEpoch(entityScanner.getSignature());
    }
    
    @Override
    public List<String> getSignatures() {
        return getServerSignatures();
    }
    
    @Override
    public void lock(String lockString) throws Exception {
        serverLock(lockString);
    }

    @Override
    public void release(String lockString) throws Exception {
        serverRelease(lockString);
    }
    
    /////////////////////////////////////////////////////////////////////////////////////////////////
    
    private static synchronized ActiveDataRegistration registerScanner(String signature, String entityScannerClassName) throws Exception {
        Class c = Class.forName(entityScannerClassName);
        EntityScanner es = (EntityScanner) c.newInstance();
        ActiveDataScan scan = scanMap.get(signature);
        if (scan == null) {
            scan = new ActiveDataScan();
            long[] idArray = null;
            try {
                idArray = es.generateIdList(null /* data resource */);
            } catch (Exception ex) {
                scan.setStatusDescriptor(ActiveDataScan.SCAN_STATUS_ERROR);
                logger.error("Error generating ID list", ex);
                throw ex;
            }
            scan.setIdArray(idArray);
            scan.setStatusDescriptor(ActiveDataScan.SCAN_STATUS_PROCESSING);
            logger.info("registerScanner - created new Scan entry for " + signature + " with idList count=" + idArray.length);
            scanMap.put(signature, scan);
        } else {
            logger.info("Found scan entry for " + signature + " with idList count=" + scan.getIdCount());
        }
        if (scan.getStatusDescriptor().equals(ActiveDataScan.SCAN_STATUS_ERROR)) {
            throw new Exception("Could not register scanner for " + signature + " because status is ERROR");
        } else {
            ActiveDataScannerStats scanStats = new ActiveDataScannerStats();
            long nextScannerIndex = scan.scannerCount();
            scan.addScanner(nextScannerIndex, scanStats);
            ActiveDataScanStatus scanStatus = scan.getStatus();
            ActiveDataRegistration registration = new ActiveDataRegistration(nextScannerIndex, scanStatus);
            return registration;
        }
    }
    
    private static Long getNext(String signature) throws Exception {
        ActiveDataScan scan=getAndValidateScan(signature);
        return scan.getNextId();
    }
    
    private static ActiveDataScanStatus getScanStatus(String signature) throws Exception {
        ActiveDataScan scan=getAndValidateScan(signature);
        return scan.getStatus();
    }
    
    private int getEntityStatus(String signature, long entityId) throws Exception {
        ActiveDataScan scan=getAndValidateScan(signature);
        return scan.getEntityStatus(entityId);
    }
    
    private static ActiveDataScan getAndValidateScan(String signature) throws Exception {
        ActiveDataScan scan=scanMap.get(signature);
        if (scan==null) {
            throw new Exception("Could not find scan for signature="+signature);
        }
        return scan;
    }
    
    private static void setEntityStatus(String signature, long entityId, int statusCode) throws Exception {
        ActiveDataScan scan=getAndValidateScan(signature);
        scan.setEntityStatus(entityId, statusCode);
    }
    
    private static void addEntityEvent(String signature, long entityId, String descriptor) throws Exception {
        ActiveDataScan scan=getAndValidateScan(signature);
        scan.addEntityEvent(entityId, descriptor);
    }
    
    private static List<ActiveDataEntityEvent> getEntityEvents(String signature, long entityId) throws Exception {
        ActiveDataScan scan=getAndValidateScan(signature);
        return scan.getEntityEvents(entityId);
    }
    
    private static void clearEntityEvents(String signature, long entityId) throws Exception {
        ActiveDataScan scan=getAndValidateScan(signature);
        scan.clearEntityEvents(entityId);
    }

    private static void advanceEpoch(String signature) throws Exception {
        ActiveDataScan scan=getAndValidateScan(signature);
        scan.advanceEpoch();
    }
    
    private List<Long> getEpochHistory(String signature) throws Exception {
        ActiveDataScan scan=getAndValidateScan(signature);
        return scan.getEpochHistory();
    }
    
    private static List<String> getServerSignatures() {
        List<String> sortedSignatures = new ArrayList<>();
        sortedSignatures.addAll(scanMap.keySet());
        Collections.sort(sortedSignatures);
        return sortedSignatures;
    }

    private static void serverLock(String lockString) throws Exception {
        long startTime=new Date().getTime();
        long currentTime=startTime;
        boolean haveLock=false;
        while(!haveLock) {
            synchronized(lockMap) {
                Long lockTime=lockMap.get(lockString);
                if (lockTime==null) {
                    lockMap.put(lockString, currentTime);
                    haveLock=true;
                }
            }
            if (!haveLock) {
                logger.info("Waiting to obtain lock for="+lockString);
                Thread.sleep(LOCK_CHECK_INTERVAL_MS);
                currentTime=new Date().getTime();
                if (currentTime-startTime > LOCK_MAX_WAIT_MS) {
                    throw new Exception("Exceeded max wait time to obtain lock for="+lockString);
                }
            }
        }
    }
    
    private static void serverRelease(String lockString) throws Exception {
       synchronized(lockMap) {
           Long lockTime = lockMap.get(lockString);
           if (lockTime==null) {
               throw new Exception("No lock to release for="+lockString);
           }
           lockMap.remove(lockString);
       } 
    }
    
}
