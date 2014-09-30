/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.service.activeData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.service.activeData.scanner.EntityScanner;
import org.janelia.it.jacs.compute.service.activeData.visitor.ActiveVisitor;

/**
 *
 * @author murphys
 */
public class ScannerManager {
    
    private final Logger logger = Logger.getLogger(ScannerManager.class);
    
    private final ScheduledThreadPoolExecutor managerPool=new ScheduledThreadPoolExecutor(1);
    private final ScheduledThreadPoolExecutor scannerPool;
    private int nextScannerIndex=0;
    
    private final List<EntityScanner> scannerList=new ArrayList<>();
    private ScheduledFuture<?> managerFuture=null;       
    ActiveDataClient activeData=new ActiveDataClientSimpleLocal();   
    private static ScannerManager manager=null;
    private final List<EntityScanner> scannersToRemove=new ArrayList<>();
    
    public static ScannerManager getInstance() {
        if (manager==null) {
            manager=new ScannerManager();
        }
        return manager;
    }
    
    private ScannerManager() {
        scannerPool=new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors());
    }
    
    public EntityScanner getScannerBySignature(String signature) {
        for (EntityScanner scanner : scannerList) {
            if (scanner.getSignature().equals(signature)) {
                return scanner;
            }
        }
        return null;
    }
    
    public synchronized void addScanner(EntityScanner scanner) throws Exception {
        logger.info("addScanner() with signature="+scanner.getSignature());
        // Handle new scanner        
        EntityScanner possibleDuplicate=getScannerBySignature(scanner.getSignature());
        if (possibleDuplicate!=null) {
            throw new Exception("May not have duplicate scanners");
        }
        ActiveDataScanStatus scanStatus=activeData.getScanStatus(scanner.getSignature());
        if (scanStatus!=null) {
            logger.info("scanStatus from activeData is not null, status="+scanStatus);
            if (scanStatus.getStatusDescriptor().equals(ActiveDataScan.SCAN_STATUS_EPOCH_COMPLETED)) {
                logger.info("Scan already active in system - at completed state. Incrementing to next Epoch in this case");
                activeData.advanceEpoch(scanner.getSignature());
            }
        } else {
            logger.info("scanStatus from activeData is null");
        }
        activeData.registerScanner(scanner.getSignature());
        scanner.setStatus(EntityScanner.STATUS_PROCESSING);
        synchronized(scannerList) {
            scannerList.add(scanner);
        }
        // Re-start manager if necessary
        if (managerFuture == null) {
            logger.info("managerFuture is null - creating new instance within managerPool");
            managerFuture = managerPool.scheduleWithFixedDelay(new ScanManagerThread(), 50, 50, TimeUnit.MILLISECONDS);
        } else {
            logger.info("managerFuture is not null - assuming that a manager instance is running");
        }
    }
    
    public void removeScanner(String signature) throws Exception {
        EntityScanner scannerToRemove=getScannerBySignature(signature);
        if (scannerToRemove!=null) {
            synchronized(scannersToRemove) {
                scannersToRemove.add(scannerToRemove);
            }
        }
    }
    
    public ActiveDataScanStatus getActiveDataStatus(String signature) throws Exception {
        return activeData.getScanStatus(signature);
    }

    public Runnable getRunnableForNextId(final EntityScanner scanner) throws Exception {
        return new Runnable() {
            @Override
            public void run() {
                Long entityId;
                try {
                    entityId = activeData.getNext(scanner.getSignature());
                    logger.info("getRunnableForNextId() id="+entityId);
                    if (entityId == ActiveDataScan.ID_CODE_SCAN_ERROR) {
                        scanner.setStatus(EntityScanner.STATUS_ERROR);
                        throw new Exception("Error in ActiveDataScan signature="+scanner.getSignature());
                    } else if (entityId == ActiveDataScan.ID_CODE_EPOCH_COMPLETED_SUCCESSFULLY) {
                        logger.info("Current scan completed successfully - skipping processing of nextId");
                        String scannerStatus=scanner.getStatus();
                        if (scannerStatus.equals(EntityScanner.STATUS_PROCESSING)){
                            scanner.setStatus(EntityScanner.STATUS_EPOCH_COMPLETED);
                        }
                    } else if (entityId == ActiveDataScan.ID_CODE_WAIT) {
                        logger.info("Received ID_CODE_WAIT");
                        Thread.sleep(1000); // wait 1 second
                    } else { // normal processing case
                        logger.info("Normal processing case with id="+entityId);
                        Map<String, Object> contextMap=new HashMap<>();
                        for (VisitorFactory vf : scanner.getVisitorFactoryList()) {
                            ActiveVisitor av = vf.createInstance();
                            av.setEntityId(entityId);
                            av.setContextMap(contextMap);
                            String scannerSignature=scanner.getSignature();
                            try {
                                activeData.setEntityStatus(scannerSignature, entityId, ActiveDataScan.ENTITY_STATUS_PROCESSING);
                                Boolean visitorSucceeded=av.call();
                                if (!visitorSucceeded) {
                                    logger.error("Visitor "+av.getClass().getName()+" failed");
                                    activeData.setEntityStatus(scannerSignature, entityId, ActiveDataScan.ENTITY_STATUS_ERROR);
                                } else {
                                    activeData.setEntityStatus(scannerSignature, entityId, ActiveDataScan.ENTITY_STATUS_COMPLETED_SUCCESSFULLY);
                                }
                            } catch (Exception ex2) {
                                logger.error("Caught visitor exception - problem not caught by normal visitor error handler");
                                activeData.setEntityStatus(scannerSignature, entityId, ActiveDataScan.ENTITY_STATUS_ERROR);
                                logger.error(ex2,ex2);
                            }
                        }
                    }
                } catch (Exception ex) {
                    logger.error(ex, ex);
                }
            }
        };
    }
    
    private class ScanManagerThread implements Runnable {

        /*
         Every time the ScanManager wakes up, its job is to make sure the scanner thread
         pool is loaded up with processing scan jobs, and to add jobs as needed to the pool
         using the round-robin index.
         */
        @Override
        public void run() {
            logger.info("run() starting");
            boolean discontinueManagerThreadFlag = false;
            // First, remove any scanners and check if we should discontinue management
            synchronized (scannerList) {
                if (scannersToRemove.size() > 0) {
                    for (EntityScanner scannerToRemove : scannersToRemove) {
                        scannerList.remove(scannerToRemove);
                    }
                    scannersToRemove.clear();
                }
                if (scannerList.isEmpty()) {
                    discontinueManagerThreadFlag = true;
                    try {
                        discontinueManagerThread();
                    } catch (Exception ex) {
                        logger.error(ex, ex);
                    }
                }
            }
            if (!discontinueManagerThreadFlag) {
                int availableThreadSlots = scannerPool.getCorePoolSize() - scannerPool.getActiveCount();
                logger.info("availableThreadSlots=" + availableThreadSlots);
                for (int i = 0; i < availableThreadSlots; i++) {
                    if (nextScannerIndex >= scannerList.size()) {
                        nextScannerIndex = 0;
                    }
                    EntityScanner scanner = scannerList.get(nextScannerIndex);
                    String scannerStatus = scanner.getStatus();
                    try {
                        if (scannerStatus.equals(EntityScanner.STATUS_INACTIVE)
                                || scannerStatus.equals(EntityScanner.STATUS_PROCESSING)) {
                            scannerPool.submit(getRunnableForNextId(scanner));
                        } else if (scannerStatus.equals(EntityScanner.STATUS_ERROR)) {
                            logger.error("Skipping scanner signature=" + scanner.getSignature() + " due to error status");
                            scannersToRemove.add(scanner);
                        } else if (scannerStatus.equals(EntityScanner.STATUS_EPOCH_COMPLETED)) {
                            if (scanner.getRemoveAfterEpoch()) {
                                logger.info("Removing scanner " + scanner.getSignature() + " after completed Epoch");
                                scannersToRemove.add(scanner);
                            } else {
                                logger.info("Advancing Epoch for scanner signature="+scanner.getSignature());
                                activeData.advanceEpoch(scanner.getSignature());
                                scanner.setStatus(EntityScanner.STATUS_PROCESSING);
                                logger.info("Running scanner with fresh Epoch");
                                scannerPool.submit(getRunnableForNextId(scanner));
                            }
                        }
                    } catch (Exception ex) {
                        logger.error(ex, ex);
                    }
                }
                nextScannerIndex++;
            }
        }

    }
    
    private synchronized void discontinueManagerThread() throws Exception {
        if (scannerList.isEmpty()) {
            logger.info("scannerList is empty, so cancelling managerFuture and setting to null");
            if (managerFuture != null) {
                managerFuture.cancel(false);
                if (managerFuture.isDone()) {
                    managerFuture = null;
                } else {
                    throw new Exception("ManagerFuture unexpectedly is not done even after cancellation");
                }
            } else {
                logger.info("managerFuture is already null - skipping cancellation and nullification");
            }
        } else {
            logger.info("Ignoring request to discontinue ManagerThread since the scannerList is not empty");
        }
    }
    
    public synchronized void shutdown() {
        scannerPool.shutdown();
        managerPool.shutdown();
    }  
    
}
