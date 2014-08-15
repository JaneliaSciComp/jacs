/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.service.geometricSearch;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.service.activeData.ActiveDataClient;
import org.janelia.it.jacs.compute.service.activeData.ActiveDataClientSimpleLocal;
import org.janelia.it.jacs.compute.service.activeData.ActiveDataScan;
import org.janelia.it.jacs.compute.service.activeData.ActiveEntityTestVisitor;
import org.janelia.it.jacs.compute.service.activeData.ActiveDataScanStatus;
import org.janelia.it.jacs.compute.service.activeData.ActiveTestVisitor;
import org.janelia.it.jacs.compute.service.activeData.EntityScanner;
import org.janelia.it.jacs.compute.service.activeData.SampleScanner;
import org.janelia.it.jacs.compute.service.activeData.ScannerManager;
import org.janelia.it.jacs.compute.service.activeData.VisitorFactory;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.geometricSearch.GeometricIndexTask;

/**
 *
 * @author murphys
 */
public class GeometricIndexService extends AbstractEntityService {
    
    private static final Logger logger = Logger.getLogger(GeometricIndexService.class);
    public static final long MAX_SERVICE_TIME_MS = 1000 * 60 * 60 * 24; // 24 hours
    private static List<ScheduledThreadPoolExecutor> threadPools=new ArrayList<>();
    private static List<ServiceState> stateList=new ArrayList<>();
  
    private ScheduledThreadPoolExecutor managerPool=null;
    private ScheduledFuture<?> managerFuture=null;    
    private ServiceState serviceState=new ServiceState();
    
    @Override
    protected void execute() throws Exception {
        managerPool=new ScheduledThreadPoolExecutor(1);
        threadPools.add(managerPool);
        stateList.add(serviceState);
        GeometricIndexTask indexTask=(GeometricIndexTask)task;
        EJBFactory.getRemoteComputeBean().saveEvent(indexTask.getObjectId(), Event.RUNNING_EVENT, "Running", new Date());
        List<VisitorFactory> geometricIndexVisitors=new ArrayList<>();
        Map<String,Object> parameterMap=new HashMap<>();
        VisitorFactory testFactory=new VisitorFactory(parameterMap, ActiveTestVisitor.class);
        geometricIndexVisitors.add(testFactory);
        VisitorFactory testEntityFactory=new VisitorFactory(parameterMap, ActiveEntityTestVisitor.class);
        geometricIndexVisitors.add(testEntityFactory);
        SampleScanner sampleScanner=new SampleScanner(geometricIndexVisitors);
        sampleScanner.setRemoveAfterEpoch(true);
        logger.info("Adding scanner to ScannerManager");
        ScannerManager.getInstance().addScanner(sampleScanner);
        long startTime=new Date().getTime();
        GeometricIndexServiceThread serviceThread=new GeometricIndexServiceThread(indexTask, sampleScanner, startTime, serviceState);
        managerFuture = managerPool.scheduleWithFixedDelay(serviceThread, 0, 1, TimeUnit.MINUTES);
        while(!serviceState.getDone() && !serviceState.getError()) {
            Thread.sleep(1000L);
        }
        managerPool.shutdown();
        threadPools.remove(managerPool);
        stateList.remove(serviceState);
        if (serviceState.getError()) {
            throw new Exception("Error during execution");
        }
        logger.info("execute() at end");
    }
    
        private static class GeometricIndexServiceThread implements Runnable {
            private GeometricIndexTask indexTask;
            private SampleScanner sampleScanner;
            long startTime=0L;
            ServiceState taskState;
           
            public GeometricIndexServiceThread(GeometricIndexTask indexTask, SampleScanner sampleScanner, long startTime, ServiceState taskState) {
                this.indexTask=indexTask;
                this.sampleScanner=sampleScanner;
                this.startTime=startTime;
                this.taskState=taskState;
            }
            
            @Override
            public void run() {
                logger.info("run() called");
                if (new Date().getTime() - startTime > MAX_SERVICE_TIME_MS) {
                    logger.error("Exceeded max service time");
                    taskState.setError(true);
                }
                ActiveDataScanStatus scanStatus=null;
                try {
                    scanStatus = ScannerManager.getInstance().getActiveDataStatus(sampleScanner.getSignature());
                    logger.info("scanStatus="+scanStatus);
                    String status=sampleScanner.getStatus();
                    if (status.equals(EntityScanner.STATUS_ERROR)) {
                        logger.error("sampleScanner status is ERROR");
                        EJBFactory.getRemoteComputeBean().saveEvent(indexTask.getObjectId(), Event.ERROR_EVENT, "sampleScanner error", new Date());
                        taskState.setError(true);
                    } else if (status.equals(EntityScanner.STATUS_EPOCH_COMPLETED)) {
                        logger.info("sampleScanner completed");
                        EJBFactory.getRemoteComputeBean().saveEvent(indexTask.getObjectId(), Event.COMPLETED_EVENT, "Completed", new Date());
                        logger.info("GeometricIndexService scan completed");
                        taskState.setDone(true);
                    } else {
                        logger.info("sampleScanner running - status="+status);
                    }
                } catch (Exception ex) {
                    logger.error(ex,ex);
                    taskState.setError(true);
                }
            }
            
        }
        
        public static synchronized void shutdown() {
            logger.info("shutdown()");
            for (ServiceState taskState : stateList) {
                logger.info("Marking taskState=true");
                taskState.setDone(true);
            }
            try {
                Thread.sleep(1000);
            } catch (Exception ex) {}
            logger.info("Now calling shutdown on threadpools");
            for (ScheduledThreadPoolExecutor threadPool : threadPools) {
                try {
                    if (threadPool!=null) {
                        logger.info("threadPool.shutdown()");
                        threadPool.shutdown();
                    }
                } catch (Exception ex) {}
            }       
        }
        
        private static class ServiceState {
            boolean done=false;
            boolean error=false;
            
            public void setDone(boolean value) {
                done=value;
            }
            
            public boolean getDone() {
                return done;
            }

            public void setError(boolean value) { error=value; }

            public boolean getError() { return error; }
        }
    
}
