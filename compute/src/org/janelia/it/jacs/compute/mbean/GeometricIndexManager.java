/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.mbean;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.service.activeData.ScannerManager;
import org.janelia.it.jacs.compute.service.geometricSearch.GeometricIndexService;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.geometricSearch.GeometricIndexTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;
import org.jboss.system.ServiceMBeanSupport;

/**
 *
 * @author murphys
 */
public class GeometricIndexManager extends ServiceMBeanSupport implements GeometricIndexManagerMBean {
    
    private static final Logger logger = Logger.getLogger(GeometricIndexManager.class);
    private static final long MANAGER_DELAY_INTERVAL_MINUTES = 60L;
    
    private static final ScheduledThreadPoolExecutor managerPool=new ScheduledThreadPoolExecutor(1);
    private static ScheduledFuture<?> managerFuture=null;
    
    private static GeometricIndexTask indexTask=null;
    private static long indexTaskStartTime=0L;

    @Override
    public void startGeometricIndexManager() {
        logger.info("GeometricIndexManager - startGeometricIndexManager()");
        startManager();
    }

    @Override
    public void stopGeometricIndexManager() {
        logger.info("GeometricIndexManager - stopGeometricIndexManager()");
        stopManager();
    }
    
    public static synchronized void startManager() {
        if (managerFuture == null) {
            logger.info("scheduling GeometricIndexManagerThread");
            managerFuture = managerPool.scheduleWithFixedDelay(new GeometricIndexManagerThread(), 0, MANAGER_DELAY_INTERVAL_MINUTES, TimeUnit.MINUTES);
        } else {
            logger.info("managerFuture is non-null - disregarding startManager()");
        }   
    }
    
    public static synchronized void stopManager() {
        if (managerFuture != null) {
            managerFuture.cancel(false);
            if (managerFuture.isDone()) {
                managerFuture = null;
            } else {
                logger.error("managerFuture unexpectedly is not done even after cancel");
            }            
        } else {
            logger.info("managerFuture is already null - nothing to stop");
        }
    }

    @Override
    public void create() throws Exception {
        logger.info("create() called");
    }

    @Override
    public void destroy() {
        logger.info("destroy() called");
    }

    @Override
    public void start() throws Exception {
        logger.info("start() called");
    }

    @Override
    public void stop() {
        logger.info("stop() called");
        managerPool.shutdown();
        GeometricIndexService.shutdown();
        ScannerManager.getInstance().shutdown();
    }

    private static class GeometricIndexManagerThread implements Runnable {

        @Override
        public void run() {
            logger.info("GeometricIndexManagerThread - run()");
            ComputeBeanRemote computeBean=EJBFactory.getRemoteComputeBean();
            if (indexTask==null) {
                logger.info("Creating and submitting GeometricIndexTask...");
                indexTask=new GeometricIndexTask(new HashSet<Node>(), User.SYSTEM_USER_LOGIN, new ArrayList<Event>(),
                    new HashSet<TaskParameter>());
                indexTaskStartTime=new Date().getTime();
                try {
                    logger.info("Saving GeometricIndexTask...");
                    indexTask=(GeometricIndexTask) computeBean.saveOrUpdateTask(indexTask);
                    logger.info("Save done");
                } catch (Exception ex) {
                    logger.error("Exception saving GeometricIndex task", ex);
                }
                logger.info("Next step is submission of GeometricIndexTask");
                try {
                    logger.info("Submitting GeometricIndexTask id="+indexTask.getObjectId());
                    computeBean.submitJob("GeometricIndex", indexTask.getObjectId());
                    logger.info("Submission done");
                } catch (Exception ex) {
                    logger.error("Exception submitting GeometricIndexTask", ex);
                }
            } else {
                logger.info("Information for GeometricIndexTask created at time="+new Date(indexTaskStartTime));
                try {
                    List<Event> events=computeBean.getEventsForTask(indexTask.getObjectId());
                    for (Event event : events) {
                        logger.info("GeometricIndexTask event="+event.getEventType()+" at time="+event.getTimestamp());
                    }
                    Event lastEvent=events.get(events.size()-1);
                    if (lastEvent.getEventType().equalsIgnoreCase(Event.COMPLETED_EVENT)) {
                        logger.info("Previous GeometricIndexManager task completed successfully - resetting for run on next invocation");
                        indexTask=null;
                    }
                } catch (Exception ex) {
                    logger.error("Error retrieving GeometricIndexTask information, id="+indexTask.getObjectId(), ex);
                }
            }
        }
        
    }
    
}
