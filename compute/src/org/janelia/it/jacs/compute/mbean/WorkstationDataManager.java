package org.janelia.it.jacs.compute.mbean;

import java.util.ArrayList;
import java.util.HashSet;

import org.janelia.it.jacs.compute.api.AnnotationBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.fileDiscovery.EntityViewCreationTask;
import org.janelia.it.jacs.model.tasks.fileDiscovery.FileDiscoveryTask;
import org.janelia.it.jacs.model.tasks.fileDiscovery.MCFODataPipelineTask;
import org.janelia.it.jacs.model.tasks.fileDiscovery.MCFOSamplePipelineTask;
import org.janelia.it.jacs.model.tasks.utility.GenericTask;
import org.janelia.it.jacs.model.user_data.Node;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 5/24/11
 * Time: 10:37 AM
 */
public class WorkstationDataManager implements WorkstationDataManagerMBean {

	
    public WorkstationDataManager() {
    }

    public void runSampleSyncService(String user) {
        try {
        	Task task = new GenericTask(new HashSet<Node>(), 
            		user, new ArrayList<Event>(), new HashSet<TaskParameter>(), "sampleSync", "Sample Sync");
            task.setJobName("MultiColor FlipOut File Discovery Task");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("SampleFileNodeSync", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public void runMCFODataPipeline(String user, boolean refresh, String inputDirList, String topLevelFolderName) {
        try {
        	Task task = new MCFODataPipelineTask(new HashSet<Node>(), 
            		user, new ArrayList<Event>(), new HashSet<TaskParameter>(), 
            		inputDirList, topLevelFolderName, refresh);
            task.setJobName("MultiColor FlipOut File Discovery Task");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("MCFODataPipeline", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void runMergedTileDataPipeline(String user, boolean refresh, String inputDirList, String topLevelFolderName) {
        try {
        	Task task = new MCFODataPipelineTask(new HashSet<Node>(), 
            		user, new ArrayList<Event>(), new HashSet<TaskParameter>(), 
            		inputDirList, topLevelFolderName, refresh);
            task.setJobName("Merged Tile File Discovery Task");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("MergedTileDataPipeline", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    /**
     * @deprecated use runMCFODataPipelineService and specify the parameters explicitly
     */
    public void runMCFODataPipelineForTiles(String user, boolean refresh) {
        try {
        	String topLevelFolderName = "FlyLight Single Neuron Data";
        	String inputDirList = "/groups/flylight/flylight/flip/SecData/tiles";
        	Task task = new MCFODataPipelineTask(new HashSet<Node>(), 
            		user, new ArrayList<Event>(), new HashSet<TaskParameter>(), 
            		inputDirList, topLevelFolderName, refresh);
            task.setJobName("MultiColor FlipOut Unified File Discovery Task");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("MCFODataPipeline", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void runMCFOSamplePipeline(String sampleEntityId) {
        try {
        	Entity sampleEntity = EJBFactory.getLocalAnnotationBean().getEntityById(sampleEntityId);
        	Task task = new MCFOSamplePipelineTask(new HashSet<Node>(), 
        			sampleEntity.getUser().getUserLogin(), new ArrayList<Event>(), new HashSet<TaskParameter>(), sampleEntityId);
            task.setJobName("MultiColor FlipOut Unified Sample Pipeline Task");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("MCFOSamplePipeline", task.getObjectId());
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
    
    public void runFlyScreenPipeline(String user, boolean refresh) {
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
}
