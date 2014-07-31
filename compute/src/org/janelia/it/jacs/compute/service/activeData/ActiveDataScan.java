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

/**
 *
 * @author murphys
 */
public class ActiveDataScan {
    
    Logger logger = Logger.getLogger(ActiveDataScan.class);
    
    public static final String SCAN_STATUS_UNDEFINED = "Undefined";
    public static final String SCAN_STATUS_PROCESSING = "Processing";
    public static final String SCAN_STATUS_EPOCH_COMPLETED_SUCCESSFULLY = "Epoch Successful";
    public static final String SCAN_STATUS_ERROR = "Error";
    
    public static final byte ENTITY_STATUS_UNDEFINED = 0;
    public static final byte ENTITY_STATUS_PROCESSING = 1;
    public static final byte ENTITY_STATUS_COMPLETED_SUCCESSFULLY = 2;
    public static final byte ENTITY_STATUS_ERROR = 3;
    
    public static final long ID_CODE_EPOCH_COMPLETED_SUCCESSFULLY = -1;
    public static final long ID_CODE_SCAN_ERROR = -2;
    
    private int epochNumber=0;
    private int currentEpochNumProcessing=0;
    private int currentEpochNumSuccessful=0;
    private int currentEpochNumError=0;
    private String statusDescriptor = SCAN_STATUS_UNDEFINED;
    private String entityScannerClassname;
    
    long[] idArray=null;
    Map<Long,Byte> statusMap = new HashMap<>();
    Map<Long, List<ActiveDataEntityEvent>> eventMap = new HashMap<>();
    List<ScanEpochRecord> epochHistory = new ArrayList<>();
    
    int nextIdIndex=0;
    long currentEpochStartTimestamp=0L;
    
    Map<Long, ActiveDataScannerStats> scannerStatMap=new HashMap<>();
    
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
    
    public ActiveDataScanStatus getStatus() {
        updateStatus();
        ActiveDataScanStatus status=new ActiveDataScanStatus(
                currentEpochStartTimestamp,
                epochNumber, 
                currentEpochNumProcessing,
                currentEpochNumSuccessful,
                currentEpochNumError,
                getIdCount(),
                statusDescriptor);
        return status;
    }
    
    private synchronized void resetStatus() {
        for (ActiveDataScannerStats ads : scannerStatMap.values()) {
            ads.reset();
        }
        statusDescriptor = SCAN_STATUS_UNDEFINED;
        nextIdIndex=0;
         for (int i=0;i<idArray.length;i++) {
            statusMap.put(idArray[i], ENTITY_STATUS_UNDEFINED);
        }
        updateStatus();
    }
    
    private synchronized void updateStatus() {
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
    }
    
    public synchronized void addScanner(long scannerId, ActiveDataScannerStats stats) {
        scannerStatMap.put(scannerId, stats);
    }
    
    public int scannerCount() {
        return scannerStatMap.size();
    }
    
    public synchronized void setStatusDescriptor(String statusDescriptor) throws Exception {
        if (statusDescriptor.equals(SCAN_STATUS_UNDEFINED) ||
            statusDescriptor.equals(SCAN_STATUS_PROCESSING) ||
            statusDescriptor.equals(SCAN_STATUS_EPOCH_COMPLETED_SUCCESSFULLY) ||
            statusDescriptor.equals(SCAN_STATUS_ERROR)) {
            this.statusDescriptor=statusDescriptor;
        } else {
            throw new Exception("Do not recognize status type="+statusDescriptor);
        }      
    }
    
    public String getStatusDescriptor() {
        return statusDescriptor;
    }
    
    public synchronized long getNextId() {
        if (nextIdIndex >= idArray.length) {
            statusDescriptor = SCAN_STATUS_EPOCH_COMPLETED_SUCCESSFULLY;
        }
        if (statusDescriptor.equals(ActiveDataScan.SCAN_STATUS_EPOCH_COMPLETED_SUCCESSFULLY)) {
            return ActiveDataScan.ID_CODE_EPOCH_COMPLETED_SUCCESSFULLY;
        } else if (!statusDescriptor.equals(ActiveDataScan.SCAN_STATUS_PROCESSING)) {
            return ActiveDataScan.ID_CODE_SCAN_ERROR;
        } else {
            long nextId=idArray[nextIdIndex];
            nextIdIndex++;
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
    }
    
    public synchronized void advanceEpoch() throws Exception {
        if(statusDescriptor==SCAN_STATUS_EPOCH_COMPLETED_SUCCESSFULLY) {
            updateStatus();
            ScanEpochRecord record=new ScanEpochRecord();
            record.setStartTimestamp(currentEpochStartTimestamp);
            record.setEndTimestamp(new Date().getTime());
            record.setTotalIdCount(idArray.length);
            record.setSuccessfulCount(currentEpochNumSuccessful);
            record.setErrorCount(currentEpochNumError);
            epochHistory.add(record);
            epochNumber++;
            currentEpochStartTimestamp=new Date().getTime();
            updateIdList();
            resetStatus();
            statusDescriptor=SCAN_STATUS_PROCESSING;
        } else if (statusDescriptor==SCAN_STATUS_PROCESSING) {
            logger.info("Ignore request for new Epoch since already processing");
            return;
        } else {
            throw new Exception("Advancing Epoch not permitted unless in completed state");
        }
    }
    
    public List<ScanEpochRecord> getEpochHistory() {
        List<ScanEpochRecord> epochHistoryCopy=new ArrayList<>();
        Collections.copy(epochHistory, epochHistoryCopy);
        return epochHistoryCopy;
    }

    public String getEntityScannerClassname() {
        return entityScannerClassname;
    }

    public void setEntityScannerClassname(String entityScannerClassname) {
        this.entityScannerClassname = entityScannerClassname;
    }
    
    public void updateIdList() throws Exception {
        Class c = Class.forName(entityScannerClassname);
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
    
}
