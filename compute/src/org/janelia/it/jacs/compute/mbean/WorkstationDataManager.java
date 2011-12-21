package org.janelia.it.jacs.compute.mbean;

import java.sql.Timestamp;
import java.util.*;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanLocal;
import org.janelia.it.jacs.compute.api.AnnotationBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.service.entity.SampleFileNodeSyncService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.fileDiscovery.*;
import org.janelia.it.jacs.model.tasks.utility.GenericTask;
import org.janelia.it.jacs.model.user_data.Node;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 5/24/11
 * Time: 10:37 AM
 */
public class WorkstationDataManager implements WorkstationDataManagerMBean {

    private static final Logger logger = Logger.getLogger(WorkstationDataManager.class);

	
    public WorkstationDataManager() {
    }

    public void runSampleSyncService(String user, Boolean testRun) {
        try {
        	HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
        	taskParameters.add(new TaskParameter(SampleFileNodeSyncService.PARAM_testRun, Boolean.toString(testRun), null)); 
        	Task task = new GenericTask(new HashSet<Node>(), user, new ArrayList<Event>(), 
        			taskParameters, "sampleSync", "Sample Sync");
            task.setJobName("MultiColor FlipOut File Discovery Task");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("SampleFileNodeSync", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public void runMCFODataPipeline(String user, String inputDirList, String topLevelFolderName, Boolean refreshProcessing, Boolean refreshAlignment, Boolean refreshSeparation) {
        try {
        	Task task = new MCFODataPipelineTask(new HashSet<Node>(), 
            		user, new ArrayList<Event>(), new HashSet<TaskParameter>(), 
            		inputDirList, topLevelFolderName, refreshProcessing, refreshAlignment, refreshSeparation);
            task.setJobName("MultiColor FlipOut Data Pipeline Task");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("MCFODataPipeline", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public void runMCFOSamplePipeline(String sampleEntityId, Boolean refreshProcessing, Boolean refreshAlignment, Boolean refreshSeparation) {
        try {
        	Entity sampleEntity = EJBFactory.getLocalAnnotationBean().getEntityById(sampleEntityId);
        	if (sampleEntity==null) throw new IllegalArgumentException("Entity with id "+sampleEntityId+" does not exist");
        	Task task = new MCFOSamplePipelineTask(new HashSet<Node>(), 
        			sampleEntity.getUser().getUserLogin(), new ArrayList<Event>(), new HashSet<TaskParameter>(), 
        			sampleEntityId, refreshProcessing, refreshAlignment, refreshSeparation);
            task.setJobName("MultiColor FlipOut Sample Pipeline Task");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("MCFOSamplePipeline", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void runMCFOSeparationPipeline(String sampleEntityId, String inputFilename, String resultEntityName) {
        try {
        	Entity sampleEntity = EJBFactory.getLocalAnnotationBean().getEntityById(sampleEntityId);
        	if (sampleEntity==null) throw new IllegalArgumentException("Entity with id "+sampleEntityId+" does not exist");
        	Task task = new MCFOSeparationPipelineTask(new HashSet<Node>(), 
        			sampleEntity.getUser().getUserLogin(), new ArrayList<Event>(), new HashSet<TaskParameter>(), sampleEntityId, inputFilename, resultEntityName);
            task.setJobName("MultiColor FlipOut Separation Pipeline Task");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("MCFOSeparationPipeline", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public void runMCFOSampleViewCreation(String sourceEntityId, String targetEntityName) {
        try {
        	Entity sourceEntity = EJBFactory.getLocalAnnotationBean().getEntityById(sourceEntityId);
        	if (sourceEntity==null) throw new IllegalArgumentException("Entity with id "+sourceEntityId+" does not exist");
        	Task task = new EntityViewCreationTask(new HashSet<Node>(), 
        			sourceEntity.getUser().getUserLogin(), new ArrayList<Event>(), new HashSet<TaskParameter>(), sourceEntityId, targetEntityName);
            task.setJobName("Sample View Creation Task");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("MCFOSampleViewCreation", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void runMergedTileDataPipeline(String user, Boolean refresh, String inputDirList, String topLevelFolderName) {
    	// TODO: redo this
//        try {
//        	Task task = new MCFODataPipelineTask(new HashSet<Node>(), 
//            		user, new ArrayList<Event>(), new HashSet<TaskParameter>(), 
//            		inputDirList, topLevelFolderName, refresh, false);
//            task.setJobName("Merged Tile Data Pipeline Task");
//            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
//            EJBFactory.getLocalComputeBean().submitJob("MergedTileDataPipeline", task.getObjectId());
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
    }
    
    public void runFlyScreenPipeline(String user, Boolean refresh) {
        try {
            String topLevelFolderName = "FlyLight Screen Data";
            String inputDirList = "/groups/scicomp/jacsData/ScreenStaging";
            Task task = new FileDiscoveryTask(new HashSet<Node>(),
                    user, new ArrayList<Event>(), new HashSet<TaskParameter>(),
                    inputDirList, topLevelFolderName, refresh);
            task.setJobName("FlyLight Screen File Discovery Task");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("FlyLightScreen", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    /**
     * @deprecated this has not been kept in sync. use the data viewer instead.
     */
    public void setupEntityTypes() {
        try {
            AnnotationBeanRemote annotationBean = EJBFactory.getRemoteAnnotationBean();
            annotationBean.setupEntityTypes();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void deleteEntityById(String entityId) {
     try {
         Long id=new Long(entityId);
         AnnotationBeanRemote annotationBean=EJBFactory.getRemoteAnnotationBean();
         annotationBean.deleteEntityById(id);
     } catch (Exception ex) {
         ex.printStackTrace();
     }
    }

    public void doEntityTreePerformanceTest() {
        try {
            AnnotationBeanLocal annotationBean = EJBFactory.getLocalAnnotationBean();
            Set<Entity> entities=annotationBean.getEntitiesByName("FlyLight Screen Data");
            if (entities.size()==1) {
                Entity topEntity=entities.iterator().next();
                logger.info("Found top-level entity name="+topEntity.getName());
                Date start=new Date();
                Entity populatedEntity=annotationBean.getEntityTree(topEntity.getId());
                Date end=new Date();
                Long ms=end.getTime()-start.getTime();
                logger.info("Entity tree took "+ms+" milliseconds");
            } else {
                logger.error("Found more than one top-level entity");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
