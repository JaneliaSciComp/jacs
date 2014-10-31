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
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.shared.geometric_search.GeometricIndexManagerModel;

/**
 *
 * @author murphys
 */
public class ActiveDataServerSimpleLocal implements ActiveDataClient, ActiveDataServer {

    private final int PRE_AND_POST_THREAD_POOL_SIZE = SystemConfigurationProperties.getInt("ActiveData.PreAndPostThreadPoolSize");

    private static final Logger logger = Logger.getLogger(ActiveDataClientSimpleLocal.class);    
    private static final Long LOCK_CHECK_INTERVAL_MS = 5000L; // 5 seconds
    private static final Long LOCK_MAX_WAIT_MS = 1000L * 60L * 60L * 6L; // 6 hours
    private static ActiveDataServerSimpleLocal theInstance=null;
    
    private final Map<String, ActiveDataScan> scanMap = new HashMap<>();  
    private final Map<String, Long> lockMap = new HashMap<>();

    private final ScheduledThreadPoolExecutor preAndPostPool=new ScheduledThreadPoolExecutor(PRE_AND_POST_THREAD_POOL_SIZE);

    public static ActiveDataServer getInstance() {
        if (theInstance==null) {
            theInstance=new ActiveDataServerSimpleLocal();
        }
        return theInstance;
    }
    
    private ActiveDataServerSimpleLocal() {}
    
    @Override
    public void injectEntity(long entityId) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ActiveDataRegistration registerScanner(String signature) throws Exception {
        synchronized (scanMap) {
            ActiveDataScan scan = scanMap.get(signature);
            if (scan == null) {
                scan = new ActiveDataScan();
                scan.setSignature(signature);
                scan.updateIdList();
                scan.setStatusDescriptor(ActiveDataScan.SCAN_STATUS_INITIAL);
                logger.info("registerScanner - created new Scan entry for " + signature + " with idList count=" + scan.getIdCount());
                scanMap.put(signature, scan);
            } else {
                logger.info("Found scan entry for " + signature + " with idList count=" + scan.getIdCount());
            }
            if (scan.getStatusDescriptor().equals(ActiveDataScan.SCAN_STATUS_ERROR)) {
                throw new Exception("Could not register scanner for " + signature + " because status is ERROR");
            }
            ActiveDataScannerStats scanStats = new ActiveDataScannerStats();
            long nextScannerIndex = scan.scannerCount();
            scan.addScanner(nextScannerIndex, scanStats);
            ActiveDataScanStatus scanStatus = scan.getStatus();
            ActiveDataRegistration registration = new ActiveDataRegistration(nextScannerIndex, scanStatus);
            return registration;
        }
    }

    
    @Override
    public Long getNext(String signature) throws Exception {
        ActiveDataScan scan = getScan(signature);
        if (scan != null) {
            synchronized (scan) {
                if (scan.getStatusDescriptor().equals(ActiveDataScan.SCAN_STATUS_INITIAL)) {
                    scan.setStatusDescriptor(ActiveDataScan.SCAN_STATUS_PRE_EPOCH);
                    spawnPreEpoch(scan);
                    return ActiveDataScan.ID_CODE_WAIT;
                } else if (scan.getStatusDescriptor().equals(ActiveDataScan.SCAN_STATUS_PROCESSING)) {
                    long nextId = scan.getNextId();
                    if (nextId==ActiveDataScan.ID_CODE_EPOCH_COMPLETED_SUCCESSFULLY) {
                        scan.setStatusDescriptor(ActiveDataScan.SCAN_STATUS_SCANS_COMPLETED);
                        spawnPostEpoch(scan);
                        return ActiveDataScan.ID_CODE_WAIT;
                    } else {
                        return nextId;
                    }
                } else if (scan.getStatusDescriptor().equals(ActiveDataScan.SCAN_STATUS_SCANS_COMPLETED) ||
                           scan.getStatusDescriptor().equals(ActiveDataScan.SCAN_STATUS_PRE_EPOCH) ||
                           scan.getStatusDescriptor().equals(ActiveDataScan.SCAN_STATUS_POST_EPOCH)) {
                    return ActiveDataScan.ID_CODE_WAIT;
                } else if (scan.getStatusDescriptor().equals(ActiveDataScan.SCAN_STATUS_EPOCH_COMPLETED)) {
                    return ActiveDataScan.ID_CODE_EPOCH_COMPLETED_SUCCESSFULLY;
                } else {
                    return ActiveDataScan.ID_CODE_SCAN_ERROR;
                }
            }
        }
        return null;
    }
    
    @Override
    public ActiveDataScanStatus getScanStatus(String signature) throws Exception {
        ActiveDataScan scan=getScan(signature);
        if (scan!=null) {
            return scan.getStatus();
        }
        return null;
    }
    
    @Override
    public int getEntityStatus(String signature, long entityId) throws Exception {
        ActiveDataScan scan=getScan(signature);
        if (scan==null) {
            throw new Exception("scan does not exist for signature="+signature);
        }
        return scan.getEntityStatus(entityId);
    }
    
    public ActiveDataScan getScan(String signature) {
        ActiveDataScan scan=scanMap.get(signature);
        return scan;
    }
    
    @Override
    public void setEntityStatus(String signature, long entityId, int statusCode) throws Exception {
        ActiveDataScan scan=getScan(signature);
        synchronized(scan) {
            scan.setEntityStatus(entityId, statusCode);
        }
    }
    
    @Override
    public void addEntityEvent(String signature, long entityId, String descriptor, Object data) throws Exception {
        ActiveDataScan scan=getScan(signature);
        synchronized(scan) {
            scan.addEntityEvent(entityId, descriptor, data);
        }
    }
    
    @Override
    public List<ActiveDataEntityEvent> getEntityEvents(String signature, long entityId) throws Exception {
        ActiveDataScan scan=getScan(signature);
        return scan.getEntityEvents(entityId);
    }
    
    @Override
    public void clearEntityEvents(String signature, long entityId) throws Exception {
        ActiveDataScan scan=getScan(signature);
        synchronized(scan) {
            scan.clearEntityEvents(entityId);
        }
    }

    public void advanceEpoch(String signature) throws Exception {
        ActiveDataScan scan=getScan(signature);
        synchronized(scan) {
            scan.advanceEpoch();
        }
    }
    
    @Override
    public List<ScanEpochRecord> getEpochHistory(String signature) throws Exception {
        ActiveDataScan scan=getScan(signature);
        return scan.getEpochHistory();
    }
    
    @Override
    public List<String> getSignatures() {
        List<String> sortedSignatures = new ArrayList<>();
        sortedSignatures.addAll(scanMap.keySet());
        Collections.sort(sortedSignatures);
        return sortedSignatures;
    }

    @Override
    public File getScanDirectory(String scannerSignature) {
        ActiveDataScan scan=getScan(scannerSignature);
        return scan.getScanDirectory();
    }

    @Override
    public void lock(String lockString) throws Exception {
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
    
    @Override
    public void release(String lockString) throws Exception {
       synchronized(lockMap) {
           Long lockTime = lockMap.get(lockString);
           if (lockTime==null) {
               throw new Exception("No lock to release for="+lockString);
           }
           lockMap.remove(lockString);
       } 
    }
    
    @Override
    public List<GeometricIndexManagerModel> getModel(int epochCount) throws Exception {
       List<GeometricIndexManagerModel> modelList=new ArrayList<>();
       for (String signature : scanMap.keySet()) {
           modelList.addAll(getModelForScanner(signature, epochCount));
        }
        Collections.sort(modelList);
        return modelList;
    }
    
    @Override
    public List<GeometricIndexManagerModel> getModelForScanner(String signature) throws Exception {
        ActiveDataScan scan=scanMap.get(signature);
        int scanCount=scan.getEpochHistory().size()+1;
        return getModelForScanner(signature, scanCount);
        
    }
    
    public List<GeometricIndexManagerModel> getModelForScanner(String signature, int epochCount) throws Exception {
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
        if (scan.getStatusDescriptor().equals(ActiveDataScan.SCAN_STATUS_EPOCH_COMPLETED)) {
            model.setEndTime(scanStatus.getEndTimestamp());
        } else {
            model.setEndTime(null);
        }
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

    public Long getModifiedTimestamp(String signature) throws Exception {
        ActiveDataScan scan=scanMap.get(signature);
        return scan.getTimestamp();
    }

    @Override
    public void spawnPreEpoch(ActiveDataScan scan) throws Exception {
        preAndPostPool.schedule(scan.getPreRunnable(), 0, TimeUnit.MILLISECONDS);
    }

    @Override
    public void spawnPostEpoch(ActiveDataScan scan) throws Exception {
        preAndPostPool.schedule(scan.getPostRunnable(), 0, TimeUnit.MILLISECONDS);
    }

    @Override
    public Map<Long, List<ActiveDataEntityEvent>> getEventMap(String signature) {
        ActiveDataScan scan=scanMap.get(signature);
        return scan.getEventMap();
    }

}
