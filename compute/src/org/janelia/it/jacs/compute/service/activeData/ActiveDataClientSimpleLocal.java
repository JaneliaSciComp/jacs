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
import org.apache.log4j.Logger;
import org.janelia.it.jacs.shared.geometricSearch.GeometricIndexManagerModel;

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
    public List<ScanEpochRecord> getEpochHistory() throws Exception {
        return getEpochHistory(entityScanner.getSignature());
    }
    
    @Override
    public void advanceEpoch() throws Exception {
        advanceEpoch(entityScanner.getSignature());
    }
    
    @Override
    public List<String> getSignatures() throws Exception {
        return getServerSignatures();
    }
    
    @Override
    public List<GeometricIndexManagerModel> getModel(int epochCount) throws Exception {
        return getServerModel(epochCount);
    }   
    
    @Override
    public List<GeometricIndexManagerModel> getModelForScanner(String signature) throws Exception {
        return getServerModelForScanner(signature);
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
        ActiveDataScan scan = scanMap.get(signature);
        if (scan == null) {
            scan = new ActiveDataScan();
            scan.setEntityScannerClassname(entityScannerClassName);
            scan.updateIdList();
            scan.setStatusDescriptor(ActiveDataScan.SCAN_STATUS_PROCESSING);
            logger.info("registerScanner - created new Scan entry for " + signature + " with idList count=" + scan.getIdCount());
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
    
    private List<ScanEpochRecord> getEpochHistory(String signature) throws Exception {
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
    
    private static List<GeometricIndexManagerModel> getServerModel(int epochCount) throws Exception {
       List<GeometricIndexManagerModel> modelList=new ArrayList<>();
       for (String signature : scanMap.keySet()) {
           modelList.addAll(getServerModelForScanner(signature, epochCount));
        }
        Collections.sort(modelList);
        return modelList;
    }
    
    private static List<GeometricIndexManagerModel> getServerModelForScanner(String signature) throws Exception {
        ActiveDataScan scan=scanMap.get(signature);
        int scanCount=scan.getEpochHistory().size()+1;
        return getServerModelForScanner(signature, scanCount);
        
    }
    
    private static List<GeometricIndexManagerModel> getServerModelForScanner(String signature, int epochCount) throws Exception {
        List<GeometricIndexManagerModel> modelList=new ArrayList<>();
        ActiveDataScan scan = scanMap.get(signature);
        // First, handle current epoch
        GeometricIndexManagerModel model = new GeometricIndexManagerModel();
        ActiveDataScanStatus scanStatus = scan.getStatus();
        model.setActiveScannerCount(scanStatus.getCurrentEpochNumProcessing());
        model.setStartTime(scanStatus.getStartTimestamp());
        model.setScannerSignature(signature);
        model.setErrorCount(scanStatus.getCurrentEpochNumError());
        model.setSuccessfulCount(scanStatus.getCurrentEpochNumSuccessful());
        model.setTotalIdCount(scanStatus.getCurrentEpochIdCount());
        model.setEndTime(null);
        modelList.add(model);
        // Next, handle most recent N epochs
        List<ScanEpochRecord> epochs = scan.getEpochHistory();
        for (int i = 0; i < epochCount; i++) {
            int index = epochs.size() - 1 - i;
            if (index > -1) {
                ScanEpochRecord scanRecord = epochs.get(index);
                GeometricIndexManagerModel m2 = new GeometricIndexManagerModel();
                m2.setStartTime(scanRecord.getStartTimestamp());
                m2.setScannerSignature(signature);
                m2.setErrorCount(scanRecord.getErrorCount());
                m2.setSuccessfulCount(scanRecord.getSuccessfulCount());
                m2.setTotalIdCount(scanRecord.getTotalIdCount());
                m2.setEndTime(scanRecord.getEndTimestamp());
                modelList.add(m2);
            }
        }
        Collections.sort(modelList);
        return modelList;
    }

    
}
