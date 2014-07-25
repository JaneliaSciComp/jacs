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
import org.janelia.it.jacs.compute.service.activeData.ActiveDataScanStatus;
import org.janelia.it.jacs.compute.service.activeData.ActiveTestVisitor;
import org.janelia.it.jacs.compute.service.activeData.EntityScanner;
import org.janelia.it.jacs.compute.service.activeData.SampleScanner;
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
    private static List<ServiceState> taskDoneList=new ArrayList<>();
  
    private ScheduledThreadPoolExecutor managerPool=null;
    private ScheduledFuture<?> managerFuture=null;
    private ActiveDataClient activeData=null;
    
    private ServiceState taskDone=new ServiceState();
    
    @Override
    protected void execute() throws Exception {
        managerPool=new ScheduledThreadPoolExecutor(1);
        threadPools.add(managerPool);
        taskDoneList.add(taskDone);
        GeometricIndexTask indexTask=(GeometricIndexTask)task;
        EJBFactory.getRemoteComputeBean().saveEvent(indexTask.getObjectId(), Event.RUNNING_EVENT, "Running", new Date());
        List<VisitorFactory> geometricIndexVisitors=new ArrayList<>();
        Map<String,Object> parameterMap=new HashMap<>();
        VisitorFactory testFactory=new VisitorFactory(parameterMap, ActiveTestVisitor.class);
        geometricIndexVisitors.add(testFactory);
        SampleScanner sampleScanner=new SampleScanner(geometricIndexVisitors);
        sampleScanner.setRemoveAfterEpoch(true);
        activeData = new ActiveDataClientSimpleLocal();
        sampleScanner.setActiveDataClient(activeData);
        sampleScanner.start();
        long startTime=new Date().getTime();
        GeometricIndexServiceThread serviceThread=new GeometricIndexServiceThread(indexTask, sampleScanner, startTime, taskDone);
        managerFuture = managerPool.scheduleWithFixedDelay(serviceThread, 0, 1, TimeUnit.MINUTES);
        while(!taskDone.get()) {
            Thread.sleep(1000L);
        }
        managerPool.shutdown();
        threadPools.remove(managerPool);
        taskDoneList.remove(taskDone);
        logger.info("execute() at end");
    }
    
        private static class GeometricIndexServiceThread implements Runnable {
            private GeometricIndexTask indexTask;
            private SampleScanner sampleScanner;
            long startTime=0L;
            ServiceState taskDone;
           
            public GeometricIndexServiceThread(GeometricIndexTask indexTask, SampleScanner sampleScanner, long startTime, ServiceState taskDone) {
                this.indexTask=indexTask;
                this.sampleScanner=sampleScanner;
                this.startTime=startTime;
                this.taskDone=taskDone;
            }
            
            @Override
            public void run() {
                logger.info("run() called");
                if (new Date().getTime() - startTime > MAX_SERVICE_TIME_MS) {
                    logger.error("Exceeded max service time");
                }
                ActiveDataScanStatus scanStatus=null;
                try {
                    scanStatus = sampleScanner.getActiveDataStatus();
                    logger.info("scanStatus="+scanStatus);
                    String status=sampleScanner.getStatus();
                    if (status.equals(EntityScanner.STATUS_ERROR)) {
                        logger.error("sampleScanner status is ERROR");
                        EJBFactory.getRemoteComputeBean().saveEvent(indexTask.getObjectId(), Event.ERROR_EVENT, "sampleScanner error", new Date());
                    } else if (status.equals(EntityScanner.STATUS_EPOCH_COMPLETED)) {
                        logger.info("sampleScanner completed");
                        EJBFactory.getRemoteComputeBean().saveEvent(indexTask.getObjectId(), Event.COMPLETED_EVENT, "Completed", new Date());
                        logger.info("GeometricIndexService scan completed");
                        taskDone.set(true);
                    } else {
                        logger.info("sampleScanner running - status="+status);
                    }
                } catch (Exception ex) {
                    logger.error(ex,ex);
                }
            }
            
        }
        
        public static synchronized void shutdown() {
            logger.info("shutdown()");
            for (ServiceState taskDone : taskDoneList) {
                logger.info("Marking taskDone=true");
                taskDone.set(true);
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
            boolean isDone=false;
            
            public void set(boolean value) {
                isDone=value;
            }
            
            public boolean get() {
                return isDone;
            }
        }
    
}
