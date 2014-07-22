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

/**
 *
 * @author murphys
 */
public abstract class EntityScanner {
    
    private static final Logger logger = Logger.getLogger(EntityScanner.class);
    
    public static final String STATUS_PROCESSING = "Processing";
    public static final String STATUS_ERROR = "Error";
    public static final String STATUS_INACTIVE = "Inactive";
    
    private static final ScheduledThreadPoolExecutor managerPool=new ScheduledThreadPoolExecutor(1);
    private static final ScheduledThreadPoolExecutor scannerPool;
    private static int nextScannerIndex=0;
    
    private static final List<EntityScanner> scannerList=new ArrayList<>();
    private static ScheduledFuture<?> managerFuture=null;
       
    ActiveDataClient activeData;
    
    private List<VisitorFactory> visitorFactoryList=new ArrayList<>();
   
    static {
        scannerPool=new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors());
    }
    
    public EntityScanner(List<VisitorFactory> visitorFactoryList) {
        this.visitorFactoryList=visitorFactoryList;
    }
    
    // Scanner Methods
    
    public abstract long[] generateIdList(Object dataResource);
    
    
    public void setActiveDataClient(ActiveDataClient activeDataClient) {
        this.activeData=activeDataClient;
        activeData.setEntityScanner(this);
    }
    
    public List<VisitorFactory> getVisitorFactoryList() {
        return visitorFactoryList;
    }
    
    public String getSignature() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getName());
        for (VisitorFactory vf : visitorFactoryList) {
            sb.append(":"+vf.getVisitorClassName());
        }
        return sb.toString();
    }
    
    public void start() throws Exception {
        addActiveDataScanner(this);
    }
    
    public void stop() throws Exception {
        removeActiveDataScanner(this);
    }
    
    public Runnable getRunnableForNextId() throws Exception {
        return new Runnable() {
            @Override
            public void run() {
                Long entityId;
                try {
                    entityId = activeData.getNext();
                    if (entityId == ActiveDataScan.ID_CODE_SCAN_ERROR) {
                        throw new Exception("Error in ActiveDataScan getNextId()");
                    } else if (entityId == ActiveDataScan.ID_CODE_EPOCH_COMPLETED_SUCCESSFULLY) {
                        logger.info("Current scan completed successfully - skipping processing of nextId");
                    } else {
                        Map<String, Object> contextMap=new HashMap<>();
                        for (VisitorFactory vf : visitorFactoryList) {
                            ActiveVisitor av = vf.createInstance();
                            av.setEntityId(entityId);
                            av.setContextMap(contextMap);
                            av.run();
                        }
                    }
                } catch (Exception ex) {
                    logger.error(ex);
                }
            }
        };
    }
    
    // Scanner Management Methods
    
    /*
    
    On each server processing Active Data scanners, there is a single thread pool from which
    the scanners are executed. The scanners within this thread pool interact with a central 
    Active Data system to determine the next entity to process, and to determine when all
    entities associated with the current epoch are done.
    
    For a scanner to begin processing, it must (1) have its Active Data client set, and
    (2) its start() method must be called, which is non-blocking. To stop it, the stop()
    method is called.
    
    The EntityScanner thread pool is of finite size, but there can be an arbitrary number
    of scanners associated with it on a given jvm.
    
    Jobs are submitted to the thread pool in round-robin fashion, keeping the thread pool
    maximally active. 
    
    */
    
    private static void addActiveDataScanner(EntityScanner scanner) throws Exception {
        synchronized (scannerList) {
            scannerList.add(scanner);
        }
        if (managerFuture == null) {
            managerFuture = managerPool.scheduleWithFixedDelay(new ScanManager(), 1000, 1000, TimeUnit.MILLISECONDS);
        }
    }
    
    private static void removeActiveDataScanner(EntityScanner scanner) throws Exception {
        synchronized(scannerList) {
            scannerList.remove(scanner);
        }
        if (scannerList.isEmpty()) {
            managerFuture.cancel(false);
            if (managerFuture.isDone()) {
                managerFuture=null;
            } else {
                throw new Exception("ManagerFuture unexpectedly is not done even after cancellation");
            }
        }
    }
    
    private static class ScanManager implements Runnable {

        /*
        Every time the ScanManager wakes up, its job is to make sure the scanner thread
        pool is loaded up with processing scan jobs, and to add jobs as needed to the pool
        using the round-robin index.
        */
        @Override
        public void run() {
            int availableThreadSlots=scannerPool.getCorePoolSize() - scannerPool.getActiveCount();
            for (int i=0;i<availableThreadSlots;i++) {
                if (nextScannerIndex>=scannerList.size()) {
                    nextScannerIndex=0;
                }
                EntityScanner scanner=scannerList.get(nextScannerIndex);
                try {
                    scannerPool.submit(scanner.getRunnableForNextId());
                } catch (Exception ex) {
                    logger.error(ex);
                }
                nextScannerIndex++;
            }
        }

    }
    
    
}
