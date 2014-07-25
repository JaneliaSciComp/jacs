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
import org.janelia.it.jacs.compute.service.activeData.ActiveDataClient;
import org.janelia.it.jacs.compute.service.activeData.ActiveDataClientSimpleLocal;
import org.janelia.it.jacs.compute.service.activeData.ActiveDataScanStatus;
import org.janelia.it.jacs.compute.service.activeData.ActiveTestVisitor;
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
    
    private static final ScheduledThreadPoolExecutor managerPool=new ScheduledThreadPoolExecutor(1);
    private static ScheduledFuture<?> managerFuture=null;
    private static long startTime=0L;
    private static ActiveDataClient activeData=null;

    @Override
    protected void execute() throws Exception {
        GeometricIndexTask indexTask=(GeometricIndexTask)task;
        computeBean.saveEvent(indexTask.getObjectId(), Event.RUNNING_EVENT, "Running", new Date());
        List<VisitorFactory> geometricIndexVisitors=new ArrayList<>();
        Map<String,Object> parameterMap=new HashMap<>();
        VisitorFactory testFactory=new VisitorFactory(parameterMap, ActiveTestVisitor.class);
        geometricIndexVisitors.add(testFactory);
        SampleScanner sampleScanner=new SampleScanner(geometricIndexVisitors);
        sampleScanner.setRemoveAfterEpoch(true);
        activeData = new ActiveDataClientSimpleLocal();
        sampleScanner.setActiveDataClient(activeData);
        sampleScanner.start();
        startTime=new Date().getTime();      
        managerFuture = managerPool.scheduleWithFixedDelay(new GeometricIndexServiceThread(), 0, 1, TimeUnit.MINUTES);
        logger.info("GeometricIndexService scan completed");
        computeBean.saveEvent(indexTask.getObjectId(), Event.COMPLETED_EVENT, "Completed", new Date());
    }
    
        private static class GeometricIndexServiceThread implements Runnable {
            
            @Override
            public void run() {
                if (new Date().getTime() - startTime > MAX_SERVICE_TIME_MS) {
                    logger.error("Exceeded max service time");
                }
                ActiveDataScanStatus scanStatus = null;
                try {
                    scanStatus = activeData.getScanStatus();
                } catch (Exception ex) {
                    logger.error(ex,ex);
                }
                logger.info("GeometricIndex: "+scanStatus.toString());
            }
            
        }
        
        public static synchronized void shutdown() {
            managerPool.shutdown();
        }

    
}
