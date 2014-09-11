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
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.shared.geometric_search.GeometricIndexManagerModel;
import org.janelia.it.jacs.shared.utils.FileUtil;

/**
 *
 * @author murphys
 */
public class ActiveDataScan {
    
    Logger logger = Logger.getLogger(ActiveDataScan.class);

    private final String ACTIVE_DATA_DIR_PATH= SystemConfigurationProperties.getString("ActiveData.Dir");

    public static final String SCAN_STATUS_INITIAL = "Initial";
    public static final String SCAN_STATUS_PRE_EPOCH = "Pre Epoch";
    public static final String SCAN_STATUS_PROCESSING = "Processing";
    public static final String SCAN_STATUS_SCANS_COMPLETED = "Scans Completed";
    public static final String SCAN_STATUS_POST_EPOCH = "Post Epoch";
    public static final String SCAN_STATUS_EPOCH_COMPLETED = "Epoch Completed";
    public static final String SCAN_STATUS_ERROR = "Error";
    
    public static final byte ENTITY_STATUS_UNDEFINED = 0;
    public static final byte ENTITY_STATUS_PROCESSING = 1;
    public static final byte ENTITY_STATUS_COMPLETED_SUCCESSFULLY = 2;
    public static final byte ENTITY_STATUS_ERROR = 3;
    
    public static final long ID_CODE_EPOCH_COMPLETED_SUCCESSFULLY = -1;
    public static final long ID_CODE_SCAN_ERROR = -2;
    public static final long ID_CODE_WAIT = -3;
    
    private int epochNumber=0;
    private int currentEpochNumProcessing=0;
    private int currentEpochNumSuccessful=0;
    private int currentEpochNumError=0;
    private String statusDescriptor = SCAN_STATUS_INITIAL;
    private String signature;
    
    long[] idArray=null;
    Map<Long,Byte> statusMap = new HashMap<>();
    Map<Long, List<ActiveDataEntityEvent>> eventMap = new HashMap<>();
    List<ScanEpochRecord> epochHistory = new ArrayList<>();
    
    int nextIdIndex=0;
    long currentEpochStartTimestamp=0L;
    long currentEpochEndTimestamp=0L;
    
    Map<Long, ActiveDataScannerStats> scannerStatMap=new HashMap<>();

    private Long modifiedTimestamp=new Long(new Date().getTime());

    public ActiveDataScan() {
        currentEpochStartTimestamp=new Date().getTime();
    }
    
    public synchronized void setIdArray(long[] idArray) {
        this.idArray=idArray;
        for (int i=0;i<idArray.length;i++) {
            statusMap.put(idArray[i], ENTITY_STATUS_UNDEFINED);
        }
    }
    
    public int getIdCount() {
        if (idArray==null) {
            return 0;
        }
        return idArray.length;
    }

    private void updateTimestamp() {
        modifiedTimestamp=new Date().getTime();
    }

    public long getTimestamp() {
        return modifiedTimestamp;
    }
    
    public synchronized ActiveDataScanStatus getStatus() {
        ActiveDataScanStatus status=new ActiveDataScanStatus(
                currentEpochStartTimestamp,
                currentEpochEndTimestamp,
                epochNumber, 
                currentEpochNumProcessing,
                currentEpochNumSuccessful,
                currentEpochNumError,
                getIdCount(),
                statusDescriptor);
        return status;
    }

    public void setCurrentEpochEndTimestamp(long endTimestamp) {
        this.currentEpochEndTimestamp=endTimestamp;
        updateTimestamp();
    }
    
    private synchronized void resetStatus() {
        for (ActiveDataScannerStats ads : scannerStatMap.values()) {
            ads.reset();
        }
        statusDescriptor = SCAN_STATUS_INITIAL;
        nextIdIndex=0;
         for (int i=0;i<idArray.length;i++) {
            statusMap.put(idArray[i], ENTITY_STATUS_UNDEFINED);
        }
        updateStatus();
    }
    
    public synchronized void updateStatus() {
        currentEpochNumProcessing=0;
        currentEpochNumSuccessful=0;
        currentEpochNumError=0;
        for (int i=0;i<idArray.length;i++) {
            byte s=statusMap.get(idArray[i]);
            if (s==ENTITY_STATUS_PROCESSING) {
                currentEpochNumProcessing++;
            } else if (s==ENTITY_STATUS_COMPLETED_SUCCESSFULLY) {
                currentEpochNumSuccessful++;
            } else if (s==ENTITY_STATUS_ERROR) {
                currentEpochNumError++;
            }
        }
        updateTimestamp();
    }
    
    public synchronized void addScanner(long scannerId, ActiveDataScannerStats stats) {
        scannerStatMap.put(scannerId, stats);
    }
    
    public int scannerCount() {
        return scannerStatMap.size();
    }
    
    public synchronized void setStatusDescriptor(String statusDescriptor) {
        if (statusDescriptor.equals(SCAN_STATUS_INITIAL) ||
            statusDescriptor.equals(SCAN_STATUS_PRE_EPOCH) ||
            statusDescriptor.equals(SCAN_STATUS_PROCESSING) ||
            statusDescriptor.equals(SCAN_STATUS_SCANS_COMPLETED) ||
            statusDescriptor.equals(SCAN_STATUS_POST_EPOCH) ||
            statusDescriptor.equals(SCAN_STATUS_EPOCH_COMPLETED) ||
            statusDescriptor.equals(SCAN_STATUS_ERROR)) {
            this.statusDescriptor=statusDescriptor;
            updateTimestamp();
        }
    }
    
    public String getStatusDescriptor() {
        return statusDescriptor;
    }
    
    public synchronized long getNextId() {
        // Check if scans are complete
        updateStatus();
        boolean allEntitiesProcessedForThisScan=(currentEpochNumSuccessful+currentEpochNumError==idArray.length);
        if (nextIdIndex >= idArray.length) {
            if (!allEntitiesProcessedForThisScan) {
                logger.info("Waiting for last remaining entities to finish processing");
                return ActiveDataScan.ID_CODE_WAIT;
            } else {
                if (statusDescriptor.equals(SCAN_STATUS_PROCESSING)) {
                    logger.info("Setting status to SCAN_STATUS_SCANS_COMPLETED");
                    setStatusDescriptor(SCAN_STATUS_SCANS_COMPLETED);
                }
            }
        }
        // Now determine appropriate response
        if (statusDescriptor.equals(ActiveDataScan.SCAN_STATUS_SCANS_COMPLETED) ||
            statusDescriptor.equals(ActiveDataScan.SCAN_STATUS_POST_EPOCH) ||
            statusDescriptor.equals(ActiveDataScan.SCAN_STATUS_EPOCH_COMPLETED)) {
            logger.info("Returning ID_CODE_EPOCH_COMPLETED_SUCCESSFULLY");
            return ActiveDataScan.ID_CODE_EPOCH_COMPLETED_SUCCESSFULLY;
        } else if (statusDescriptor.equals(ActiveDataScan.SCAN_STATUS_INITIAL) ||
                   statusDescriptor.equals(ActiveDataScan.SCAN_STATUS_PRE_EPOCH)) {
            return ActiveDataScan.ID_CODE_WAIT;
        } else if (!statusDescriptor.equals(ActiveDataScan.SCAN_STATUS_PROCESSING)) {
            logger.info("Returning ID_CODE_SCAN_ERROR");
            return ActiveDataScan.ID_CODE_SCAN_ERROR;
        } else {
            long nextId=idArray[nextIdIndex];
            nextIdIndex++;
            logger.info("Returning next id="+nextId);
            return nextId;
        }
    }
    
    public int getEntityStatus(long entityId) throws Exception {
        Byte b=statusMap.get(entityId);
        if (b==null) {
            throw new Exception("Could not find entry for entityId="+entityId);
        }
        return b;
    }
    
    public synchronized void setEntityStatus(long entityId, int statusCode) throws Exception {
        if (isValidEntityCode(statusCode)) {
            byte sc=(byte)statusCode;
            statusMap.put(entityId, sc);
            updateTimestamp();
        } else {
            throw new Exception("Invalid entity status code=" + statusCode);        
        }
    }
    
    private static boolean isValidEntityCode(int statusCode) throws Exception {
        if (statusCode < 0 || statusCode > 255) {
            return false;
        }
        byte sc = (byte) statusCode;
        if (sc == ENTITY_STATUS_UNDEFINED
                || sc == ENTITY_STATUS_PROCESSING
                || sc == ENTITY_STATUS_COMPLETED_SUCCESSFULLY
                || sc == ENTITY_STATUS_ERROR) {
            return true;
        } else {
            return false;
        }
    }
    
    public synchronized void addEntityEvent(long entityId, String descriptor) throws Exception {
        if (descriptor==null) {
            descriptor="";
        }
        Byte status=statusMap.get(entityId);
        if (status==null) {
            throw new Exception("Could not validate existence of entityId="+entityId);
        }
        List<ActiveDataEntityEvent> eventList=eventMap.get(entityId);
        if (eventList==null) {
            eventList=new ArrayList<>();
            eventMap.put(entityId, eventList);
        }
        eventList.add(new ActiveDataEntityEvent(descriptor));
        updateTimestamp();
    }
    
    public List<ActiveDataEntityEvent> getEntityEvents(long entityId) throws Exception {
        Byte status=statusMap.get(entityId);
        if (status==null) {
            throw new Exception("Could not validate existence of entityId="+entityId);
        }
        List<ActiveDataEntityEvent> events=eventMap.get(entityId);
        return events;
    }
    
    public synchronized void clearEntityEvents(long entityId) throws Exception {
        Byte status=statusMap.get(entityId);
        if (status==null) {
            throw new Exception("Could not validate existence of entityId="+entityId);
        }
        List<ActiveDataEntityEvent> events=eventMap.get(entityId);
        if (events!=null) {
            events.clear();
        }
        updateTimestamp();
    }
    
    public synchronized void advanceEpoch() throws Exception {
        if(statusDescriptor==SCAN_STATUS_EPOCH_COMPLETED) {
            updateStatus();
            logger.info("Creating EpochRecord for scanner="+getClassnameFromSignature(signature));
            ScanEpochRecord record=new ScanEpochRecord();
            record.setStartTimestamp(currentEpochStartTimestamp);
            if (currentEpochEndTimestamp>currentEpochStartTimestamp) {
                record.setEndTimestamp(currentEpochEndTimestamp);
            } else {
                record.setEndTimestamp(new Date().getTime());
            }
            record.setTotalIdCount(idArray.length);
            record.setSuccessfulCount(currentEpochNumSuccessful);
            record.setErrorCount(currentEpochNumError);
            epochHistory.add(record);
            epochNumber++;
            currentEpochStartTimestamp=new Date().getTime();
            currentEpochEndTimestamp=0L;
            updateIdList();
            resetStatus();
            setStatusDescriptor(SCAN_STATUS_INITIAL);
        } else if (statusDescriptor==SCAN_STATUS_ERROR) {
            throw new Exception("Advancing Epoch not permitted unless in completed state");
        } else {
            logger.info("Ignore request for new Epoch since already processing");
            return;
        }
    }
    
    public List<ScanEpochRecord> getEpochHistory() {
        List<ScanEpochRecord> epochHistoryCopy=new ArrayList<>();
        epochHistoryCopy.addAll(epochHistory);
        return epochHistoryCopy;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
    
    public void updateIdList() throws Exception {
        Class c = Class.forName(getClassnameFromSignature(signature));
        EntityScanner es = (EntityScanner) c.newInstance();
        long[] idArray = null;
        try {
            idArray = es.generateIdList(null /* data resource */);
        } catch (Exception ex) {
            setStatusDescriptor(ActiveDataScan.SCAN_STATUS_ERROR);
            logger.error("Error generating ID list", ex);
            throw ex;
        }
        setIdArray(idArray);
    }

    public long getFirstEpochStartTime() {
        if (epochHistory.size()==0) {
            return currentEpochStartTimestamp;
        } else {
            return epochHistory.get(0).getStartTimestamp();
        }
    }

    public File getScanDirectory() {
        String dirPath=ACTIVE_DATA_DIR_PATH + "/" + GeometricIndexManagerModel.getAbbreviatedSignature(signature) + "/"
                + new Long(getFirstEpochStartTime()).toString() + "/" + new Integer(epochNumber).toString();
        return new File(dirPath);
    }

    private String getClassnameFromSignature(String signature) throws Exception {
        String[] sigElements=signature.split(":");
        return sigElements[0];
    }

    Runnable getPreRunnable() throws Exception {
        final ActiveDataScan thisScan=this;
        return new Runnable() {
            public void run() {
                try {
                    String dirPath=getScanDirectory().getAbsolutePath();
                    logger.info("Checking scan dir="+dirPath);
                    FileUtil.ensureDirExists(dirPath);
                    Class c = Class.forName(getClassnameFromSignature(signature));
                    EntityScanner es = (EntityScanner) c.newInstance();
                    es.preEpoch(thisScan);
                    thisScan.setStatusDescriptor(ActiveDataScan.SCAN_STATUS_PROCESSING);
                }
                catch (Exception ex) {
                    try { setStatusDescriptor(ActiveDataScan.SCAN_STATUS_ERROR); } catch (Exception e2) {
                        logger.error(e2, e2);
                    }
                    logger.error(ex, ex);
                }
            }
        };
    }

    Runnable getPostRunnable() throws Exception {
        final ActiveDataScan thisScan=this;
        return new Runnable() {
            public void run() {
                try {
                    logger.info("getPostRunnable() run() start");
                    Class c = Class.forName(getClassnameFromSignature(signature));
                    EntityScanner es = (EntityScanner) c.newInstance();
                    logger.info("getPostRunnable() run() calling es.postEpoch()");
                    es.postEpoch(thisScan);
                    logger.info("getPostRunnable() run() setting scan status to SCAN_STATUS_EPOCH_COMPLETED");
                    thisScan.setCurrentEpochEndTimestamp(new Date().getTime());
                    thisScan.setStatusDescriptor(ActiveDataScan.SCAN_STATUS_EPOCH_COMPLETED);
                    logger.info("getPostRunnable() run() done");
                }
                catch (Exception ex) {
                    try { setStatusDescriptor(ActiveDataScan.SCAN_STATUS_ERROR); } catch (Exception e2) {
                        logger.error(e2, e2);
                    }
                    logger.error(ex, ex);
                }
            }
        };
    }
    
}
