package org.janelia.it.jacs.compute.mservice;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.service.fly.ScreenSampleLineCoordinationService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.FileUtil;

import javax.ejb.EntityBean;
import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 10/2/12
 * Time: 1:26 PM
 * To change this template use File | Settings | File Templates.
 */


/*
    This service runs to validate the contents of the Pattern Annotation results, checking that the appropriate
    entities exist for each sample.

 */

public class ArnimPatternAnnotationFinisherMService extends MService {

    public static final String workspaceTopLevelDirPath="/groups/scicomp/jacsData/arnimPatternAnnotationFinisherWorkspace";

    Logger logger = Logger.getLogger(ArnimPatternAnnotationFinisherMService.class);
    Set<Entity> sampleUpdateSet = Collections.synchronizedSet(new HashSet<Entity>());

    public static final int THREAD_POOL_SIZE=400;

    ExecutorService executorService= Executors.newFixedThreadPool(THREAD_POOL_SIZE);

    Long globalStartTime=0L;
    Long sampleCount=0L;

    static Long activeThreadCount=0L;
    static Long finishedThreads=0L;
    static Long accumulatedTime=0L;

    public class SampleRetrievalThread extends Thread {

        Long sampleId;

        public SampleRetrievalThread(Long sampleId) {
            this.sampleId=sampleId;
        }

        public void run() {
            EntityBeanLocal entityBean= EJBFactory.getLocalEntityBean();
            logger.info("Getting   sample tree id="+sampleId);
            Long startTime=new Date().getTime();
            Entity sampleTree=entityBean.getEntityTree(sampleId);
            Long endTime=new Date().getTime();
            Long elapsed=endTime-startTime;
            logger.info("Retrieved sample tree id="+sampleId+" in "+elapsed+" ms");
            ArnimPatternAnnotationFinisherMService.decrementActiveThreadCount(elapsed);
        }

    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public ArnimPatternAnnotationFinisherMService(String username) throws Exception {
        super(username);
    }

    public static synchronized void incrementActiveThreadCount() {
        activeThreadCount++;
    }

    public static synchronized void decrementActiveThreadCount(Long time) {
        activeThreadCount--;
        finishedThreads++;
        accumulatedTime+=time;
    }

    public static Long getActiveThreadCount() {
        return activeThreadCount;
    }

    public void run() throws Exception {
        logger.info("ArnimPatternAnnotationFinisherMService: run() start");

        globalStartTime=new Date().getTime();

        // Get top-level folder
        Entity topLevelSampleFolder = getTopLevelFolder(ScreenSampleLineCoordinationService.SCREEN_PATTERN_TOP_LEVEL_FOLDER_NAME,
                false /* create if doesn't exist */);
        if (topLevelSampleFolder == null) {
            throw new Exception("Top level folder with name=" + ScreenSampleLineCoordinationService.SCREEN_PATTERN_TOP_LEVEL_FOLDER_NAME + " is null");
        }

        // Collect Sample ID => Set<String>, where the Set<String> contains paths to the annotation directories
        final Map<Long, Set<String>> samplePatternAnnotationDirPaths=new HashMap<Long, Set<String>>();

        searchEntityContents(topLevelSampleFolder, new EntitySearchTrigger() {
            public boolean evaluate(Entity parent, Entity entity, int level) {
                logger.info("level=" + level + " : name=" + entity.getName() + " type=" + entity.getEntityType().getName());

//                if (parent.getEntityType().getName().equals(EntityConstants.TYPE_SCREEN_SAMPLE)) {
//
//                    if (entity.getEntityType().getName().equals(EntityConstants.TYPE_FOLDER)) {
//
//                        Set<String> dirPathSet=samplePatternAnnotationDirPaths.get(parent.getId());
//                        if (dirPathSet==null) {
//                            dirPathSet=new HashSet<String>();
//                            samplePatternAnnotationDirPaths.put(parent.getId(), dirPathSet);
//                        }
//                        String dirPath=entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
//                        if (dirPath!=null) {
//                            dirPathSet.add(dirPath);
//                        }
//                    }
//                    return false;
//                }

                if (entity.getEntityType().getName().equals(EntityConstants.TYPE_SCREEN_SAMPLE)) {

                    while(ArnimPatternAnnotationFinisherMService.getActiveThreadCount()>THREAD_POOL_SIZE) {
                        try { Thread.sleep(1000); } catch (Exception ex) {}
                    }

                    ArnimPatternAnnotationFinisherMService.incrementActiveThreadCount();

                    executorService.submit(new SampleRetrievalThread(entity.getId()));

                    sampleCount++;

                    if (sampleCount%100==0) {
                        Long currentTime=new Date().getTime();
                        Long elapsedTime=currentTime-globalStartTime;
                        Long totalThreads=finishedThreads;
                        Long msPerSample=elapsedTime/totalThreads;
                        Long avgTimePerCall=accumulatedTime/totalThreads;
                        logger.info("Finished threads="+totalThreads+" msPerSample="+msPerSample+" msPerCall="+avgTimePerCall);
                    }

                    return false;
                }

                else {
                    return true;
                }
            }
        });

        long dirCount=0L;
        for (Long sampleId : samplePatternAnnotationDirPaths.keySet()) {
            dirCount+=samplePatternAnnotationDirPaths.get(sampleId).size();
        }
        logger.info("Found "+samplePatternAnnotationDirPaths.size()+" sample entries containing "+dirCount+" directory paths");

        // Write sample dirs to file


        logger.info("ArnimPatternAnnotationFinisherMService: run() end");
    }

    File getWorkspaceDir() {
        return new File(workspaceTopLevelDirPath);
    }

    File getBinDir() {
        return new File(workspaceTopLevelDirPath, "bin");
    }

    File getJobsDir() {
        return new File(workspaceTopLevelDirPath, "jobs");
    }

    File getDataDir() {
        return new File(workspaceTopLevelDirPath, "data");
    }

    boolean ValidateSample(Long sampleId) throws Exception {
        return false;
    }


}
