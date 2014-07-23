/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.mbean;

import java.rmi.RemoteException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.geometricSearch.GeometricIndexTask;

/**
 *
 * @author murphys
 */
public class GeometricIndexManager implements GeometricIndexManagerMBean {
    
    private static final Logger logger = Logger.getLogger(GeometricIndexManager.class);
    private static final long MANAGER_DELAY_INTERVAL_MINUTES = 1L;
    
    private static final ScheduledThreadPoolExecutor managerPool=new ScheduledThreadPoolExecutor(1);
    private static ScheduledFuture<?> managerFuture=null;
    
    private static GeometricIndexTask indexTask=null;
    private static long indexTaskStartTime=0L;

    @Override
    public void startGeometricIndexManager() {
        logger.info("GeometricIndexManager - start()");
        startManager();
    }

    @Override
    public void stopGeometricIndexManager() {
        logger.info("GeometricIndexManager - stop()");
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

    private static class GeometricIndexManagerThread implements Runnable {

        @Override
        public void run() {
            logger.info("GeometricIndexManagerThread - run()");
            ComputeBeanLocal computeBean=EJBFactory.getLocalComputeBean();
            if (indexTask==null) {
                logger.info("Creating and submitting GeometricIndexTask...");
                indexTask=new GeometricIndexTask();
                indexTaskStartTime=new Date().getTime();
                try {
                    indexTask=(GeometricIndexTask) computeBean.saveOrUpdateTask(indexTask);
                } catch (DaoException ex) {
                    logger.error("Exception saving GeometricIndex task", ex);
                }
                try {
                    logger.info("Submitting GeometricIndexTask id="+indexTask.getObjectId());
                    computeBean.submitJob("GeometricIndex", indexTask.getObjectId());
                } catch (RemoteException ex) {
                    logger.error("Exception submitting GeometricIndexTask", ex);
                }
            } else {
                logger.info("Information for GeometrixIndexTask created at time="+new Date(indexTaskStartTime));
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
