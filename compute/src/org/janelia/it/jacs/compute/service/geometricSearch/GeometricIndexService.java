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
import org.janelia.it.jacs.compute.mbean.GeometricIndexManager;
import org.janelia.it.jacs.compute.service.activeData.scanner.AlignmentSampleScanner;
import org.janelia.it.jacs.compute.service.activeData.scanner.TextFileScanner;
import org.janelia.it.jacs.compute.service.activeData.visitor.ActiveEntitySubtreeVisitor;
import org.janelia.it.jacs.compute.service.activeData.visitor.ActiveEntityTestVisitor;
import org.janelia.it.jacs.compute.service.activeData.ActiveDataScanStatus;
import org.janelia.it.jacs.compute.service.activeData.visitor.ActiveTestVisitor;
import org.janelia.it.jacs.compute.service.activeData.scanner.EntityScanner;
import org.janelia.it.jacs.compute.service.activeData.scanner.SampleScanner;
import org.janelia.it.jacs.compute.service.activeData.ScannerManager;
import org.janelia.it.jacs.compute.service.activeData.VisitorFactory;
import org.janelia.it.jacs.compute.service.activeData.visitor.AlignmentPropertiesVisitor;
import org.janelia.it.jacs.compute.service.activeData.visitor.alignment.AlignmentCompletionVisitor;
import org.janelia.it.jacs.compute.service.activeData.visitor.alignment.AlignmentSetupVisitor;
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
        long startTime=new Date().getTime();

        VisitorFactory alignmentSetupFactory=new VisitorFactory(parameterMap, AlignmentSetupVisitor.class);
        VisitorFactory alignmentCompletionFactory=new VisitorFactory(parameterMap, AlignmentCompletionVisitor.class);
        geometricIndexVisitors.add(alignmentSetupFactory);
        geometricIndexVisitors.add(alignmentCompletionFactory);
        AlignmentSampleScanner sampleScanner=new AlignmentSampleScanner(geometricIndexVisitors);
        sampleScanner.setRemoveAfterEpoch(true);
        ScannerManager.getInstance().addScanner(sampleScanner);
        GeometricIndexServiceThread serviceThread=new GeometricIndexServiceThread(indexTask, sampleScanner, startTime, serviceState);


//        VisitorFactory alignTextFileFactory=new VisitorFactory(parameterMap, AlignmentPropertiesVisitor.class);
//        geometricIndexVisitors.add(alignTextFileFactory);
//        TextFileScanner textScanner=new TextFileScanner(geometricIndexVisitors);
//        textScanner.setRemoveAfterEpoch(true);
//        logger.info("Adding scanner to ScannerManager with signature="+textScanner.getSignature());
//        ScannerManager.getInstance().addScanner(textScanner);
//        GeometricIndexServiceThread serviceThread=new GeometricIndexServiceThread(indexTask, textScanner, startTime, serviceState);



//        VisitorFactory testFactory=new VisitorFactory(parameterMap, ActiveTestVisitor.class);
//        geometricIndexVisitors.add(testFactory);
//        VisitorFactory testEntityFactory=new VisitorFactory(parameterMap, ActiveEntitySubtreeVisitor.class);
//        geometricIndexVisitors.add(testEntityFactory);
//        SampleScanner sampleScanner=new SampleScanner(geometricIndexVisitors);
//        sampleScanner.setRemoveAfterEpoch(true);
//        logger.info("Adding scanner to ScannerManager");
//        ScannerManager.getInstance().addScanner(sampleScanner);
//        GeometricIndexServiceThread serviceThread=new GeometricIndexServiceThread(indexTask, sampleScanner, startTime, serviceState);

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
            private EntityScanner scanner;
            long startTime=0L;
            ServiceState taskState;
           
            public GeometricIndexServiceThread(GeometricIndexTask indexTask, EntityScanner scanner, long startTime, ServiceState taskState) {
                this.indexTask=indexTask;
                this.scanner=scanner;
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
                    scanStatus = ScannerManager.getInstance().getActiveDataStatus(scanner.getSignature());
                    logger.info("scanStatus="+scanStatus);
                    String status=scanner.getStatus();
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
