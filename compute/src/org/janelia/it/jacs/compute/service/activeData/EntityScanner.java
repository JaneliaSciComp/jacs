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
import org.janelia.it.jacs.compute.api.AnnotationBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import static org.janelia.it.jacs.compute.service.activeData.SampleScanner.logger;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 *
 * @author murphys
 */
public abstract class EntityScanner {
    
    private static final Logger logger = Logger.getLogger(EntityScanner.class);
    
    public static final String STATUS_PROCESSING = "Processing";
    public static final String STATUS_ERROR = "Error";
    public static final String STATUS_INACTIVE = "Inactive";
    public static final String STATUS_EPOCH_COMPLETED = "Epoch Completed";
    
    private static final ScheduledThreadPoolExecutor managerPool=new ScheduledThreadPoolExecutor(1);
    private static final ScheduledThreadPoolExecutor scannerPool;
    private static int nextScannerIndex=0;
    
    private static final List<EntityScanner> scannerList=new ArrayList<>();
    private static ScheduledFuture<?> managerFuture=null;
       
    ActiveDataClient activeData;
    protected String status = STATUS_INACTIVE;
    boolean removeAfterEpoch=false;
    
    private List<VisitorFactory> visitorFactoryList=new ArrayList<>();
   
    static {
        scannerPool=new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors());
    }
    
    public EntityScanner() {}
    
    public EntityScanner(List<VisitorFactory> visitorFactoryList) {
        this.visitorFactoryList=visitorFactoryList;
    }
    
    // Scanner Methods
    
    public abstract long[] generateIdList(Object dataResource) throws Exception;
    
    
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
        activeData.registerScanner();
        addActiveDataScanner(this);
        status=STATUS_PROCESSING;
    }
    
    public void stop() throws Exception {
        removeActiveDataScanner(this);
    }
    
    public String getStatus() {
        return status;
    }
    
    public ActiveDataScanStatus getActiveDataStatus() throws Exception {
        return activeData.getScanStatus();
    }
    
    public Runnable getRunnableForNextId() throws Exception {
        return new Runnable() {
            @Override
            public void run() {
                Long entityId;
                try {
                    entityId = activeData.getNext();
                    if (entityId == ActiveDataScan.ID_CODE_SCAN_ERROR) {
                        status=STATUS_ERROR;
                        throw new Exception("Error in ActiveDataScan getNextId()");
                    } else if (entityId == ActiveDataScan.ID_CODE_EPOCH_COMPLETED_SUCCESSFULLY) {
                        logger.info("Current scan completed successfully - skipping processing of nextId");
                        if (status.equals(STATUS_PROCESSING)){
                            status=STATUS_EPOCH_COMPLETED;
                        }
                    } else {
                        Map<String, Object> contextMap=new HashMap<>();
                        for (VisitorFactory vf : visitorFactoryList) {
                            ActiveVisitor av = vf.createInstance();
                            av.setEntityId(entityId);
                            av.setContextMap(contextMap);
                            try {
                                activeData.setEntityStatus(entityId, ActiveDataScan.ENTITY_STATUS_PROCESSING);
                                Boolean visitorSucceeded=av.call();
                                if (!visitorSucceeded) {
                                    logger.error("Visitor "+av.getClass().getName()+" failed");
                                    activeData.setEntityStatus(entityId, ActiveDataScan.ENTITY_STATUS_ERROR);
                                } else {
                                    activeData.setEntityStatus(entityId, ActiveDataScan.ENTITY_STATUS_COMPLETED_SUCCESSFULLY);
                                }
                            } catch (Exception ex2) {
                                logger.error("Caught visitor exception - problem not caught by normal visitor error handler");
                                activeData.setEntityStatus(entityId, ActiveDataScan.ENTITY_STATUS_ERROR);
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
    
    public void setRemoveAfterEpoch(boolean removeAfterEpoch) {
        this.removeAfterEpoch=removeAfterEpoch;
    }
    
    public boolean getRemoveAfterEpoch() {
        return removeAfterEpoch;
    }
    
    protected long[] generateIdListByEntityType(Object dataResource, String entityTypeName) throws Exception {
      AnnotationBeanLocal annotationBeanLocal = EJBFactory.getLocalAnnotationBean();
        List<Long> allEntityIdsByType = null;
        try {
            allEntityIdsByType = annotationBeanLocal.getAllEntityIdsByType(entityTypeName);
        } catch (Exception ex) {
            logger.error(ex, ex);
            throw ex;
        }
        if (allEntityIdsByType == null || allEntityIdsByType.isEmpty()) {
            return new long[0];
        } else {
            long[] result = new long[allEntityIdsByType.size()];
            int i = 0;
            for (Long l : allEntityIdsByType) {
                result[i++] = l;
            }
            return result;
        }         
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
            logger.info("managerFuture is null - creating new instance within managerPool");
            managerFuture = managerPool.scheduleWithFixedDelay(new ScanManager(), 50, 50, TimeUnit.MILLISECONDS);
        } else {
            logger.info("managerFuture is not null - assuming that a manager instance is running");
        }
    }
    
    private static void removeActiveDataScanner(EntityScanner scanner) throws Exception {
        logger.info("Removing scanner");
        synchronized(scannerList) {
            scannerList.remove(scanner);
        }
        if (scannerList.isEmpty()) {
            discontinueManagerThread();
        } else {
            logger.info("scannerList is not empty, so leaving managerFuture intact");
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
            logger.info("run() starting");
            int availableThreadSlots = scannerPool.getCorePoolSize() - scannerPool.getActiveCount();
            logger.info("availableThreadSlots=" + availableThreadSlots);
            for (int i = 0; i < availableThreadSlots; i++) {
                if (nextScannerIndex >= scannerList.size()) {
                    nextScannerIndex = 0;
                }
                logger.info("i=" + i + ", nextScannerIndex=" + nextScannerIndex);
                if (!scannerList.isEmpty()) {
                    EntityScanner scanner = scannerList.get(nextScannerIndex);
                    String scannerStatus = scanner.getStatus();
                    try {
                        if (scannerStatus.equals(STATUS_INACTIVE)
                                || scannerStatus.equals(STATUS_PROCESSING)) {
                            scannerPool.submit(scanner.getRunnableForNextId());
                        } else if (scannerStatus.equals(STATUS_ERROR)) {
                            logger.error("Skipping scanner signature=" + scanner.getSignature() + " due to error status");
                            removeActiveDataScanner(scanner);
                        } else if (scannerStatus.equals(STATUS_EPOCH_COMPLETED)) {
                            if (scanner.getRemoveAfterEpoch()) {
                                logger.info("Removing scanner " + scanner.getSignature() + " after completed Epoch");
                                removeActiveDataScanner(scanner);
                            }
                        }
                    } catch (Exception ex) {
                        logger.error(ex, ex);
                    }
                } else {
                    try {
                        discontinueManagerThread();
                    } catch (Exception ex) {
                        logger.error(ex,ex);
                    }
                }
            }
            nextScannerIndex++;
        }
    }
    
    private static synchronized void discontinueManagerThread() throws Exception {
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
    
    public static synchronized void shutdown() {
        scannerPool.shutdown();
        managerPool.shutdown();
    }  
    
}
