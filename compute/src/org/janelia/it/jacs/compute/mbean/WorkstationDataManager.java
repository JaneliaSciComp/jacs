package org.janelia.it.jacs.compute.mbean;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanLocal;
import org.janelia.it.jacs.compute.api.AnnotationBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.service.entity.SampleFileNodeSyncService;
import org.janelia.it.jacs.compute.service.fileDiscovery.FlyScreenDiscoveryService;
import org.janelia.it.jacs.compute.service.fly.ScreenSampleLineCoordinationService;
import org.janelia.it.jacs.compute.service.solr.SolrIndexingService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityAttribute;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.fileDiscovery.*;
import org.janelia.it.jacs.model.tasks.fly.FlyScreenPatternAnnotationTask;
import org.janelia.it.jacs.model.tasks.tic.TicTask;
import org.janelia.it.jacs.model.tasks.utility.GenericTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.shared.utils.EntityUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

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

    public void runSolrIndexSync(Boolean clearIndex) {
        try {
        	HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
        	taskParameters.add(new TaskParameter(SolrIndexingService.PARAM_clearIndex, Boolean.toString(clearIndex), null)); 
        	Task task = new GenericTask(new HashSet<Node>(), "system", new ArrayList<Event>(), 
        			taskParameters, "solrIndexSync", "Solr Index Sync");
            task.setJobName("Solr Index Sync Task");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("SolrIndexSync", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public void runSampleSyncService(String user, Boolean testRun) {
        try {
        	HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
        	taskParameters.add(new TaskParameter(SampleFileNodeSyncService.PARAM_testRun, Boolean.toString(testRun), null)); 
        	Task task = new GenericTask(new HashSet<Node>(), user, new ArrayList<Event>(), 
        			taskParameters, "sampleSync", "Sample Sync");
            task.setJobName("MultiColor FlipOut Sample Fileshare Sync Task");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("SampleFileNodeSync", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void runSampleImageSync(String user) {
        try {
        	HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
        	Task task = new GenericTask(new HashSet<Node>(), user, new ArrayList<Event>(), 
        			taskParameters, "sampleImageSync", "Sample Image Sync");
            task.setJobName("MultiColor FlipOut Sample Image Sync Task");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("SampleImageSync", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public void runMCFODataCompress(String user, Boolean testRun) {
        try {
        	HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
        	taskParameters.add(new TaskParameter(SampleFileNodeSyncService.PARAM_testRun, Boolean.toString(testRun), null)); 
        	Task task = new GenericTask(new HashSet<Node>(), user, new ArrayList<Event>(), 
        			taskParameters, "mcfoDataCompress", "MCFO Data Compress");
            task.setJobName("MultiColor FlipOut Data Compress Task");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("MCFODataCompress", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public void runMCFODataUpgrade(String user, Boolean testRun) {
        try {
        	HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
        	taskParameters.add(new TaskParameter(SampleFileNodeSyncService.PARAM_testRun, Boolean.toString(testRun), null)); 
        	Task task = new GenericTask(new HashSet<Node>(), user, new ArrayList<Event>(), 
        			taskParameters, "mcfoDataUpgrade", "MCFO Data Upgrade");
            task.setJobName("MultiColor FlipOut Data Upgrade Task");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("MCFODataUpgrade", task.getObjectId());
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
    
    public void runFlyScreenPipeline(String user, Boolean refresh) {
        try {
            String topLevelFolderName = FlyScreenDiscoveryService.SCREEN_SAMPLE_TOP_LEVEL_FOLDER_NAME;
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

    public void runFlyScreenPatternAnnotationPipeline(String user, Boolean refresh) {
        try {
            String topLevelFolderName = ScreenSampleLineCoordinationService.SCREEN_PATTERN_TOP_LEVEL_FOLDER_NAME;
            Task task = new FlyScreenPatternAnnotationTask(new HashSet<Node>(),
                    user, new ArrayList<Event>(), new HashSet<TaskParameter>(),
                    topLevelFolderName, refresh);
            task.setJobName("FlyLight Screen Pattern Annotation Task");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("FlyScreenPatternAnnotation", task.getObjectId());
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
//                Date start=new Date();
//                Entity populatedEntity=annotationBean.getEntityTree(topEntity.getId());
//                Date end=new Date();
//                Long ms=end.getTime()-start.getTime();
//                logger.info("Entity tree took "+ms+" milliseconds");
//
                Date start=new Date();
                Entity testEntity=annotationBean.getEntityTreeQuery(topEntity.getId());
                Date end=new Date();
                Long ms=end.getTime()-start.getTime();
                logger.info("Test Entity tree took "+ms+" milliseconds");

            } else {
                logger.error("Found more than one top-level entity");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void performScreenPipelineSurgery(String username) {
        try {
            final AnnotationBeanLocal annotationBean = EJBFactory.getLocalAnnotationBean();
            Set<Entity> entities=annotationBean.getEntitiesByName("FlyLight Screen Data");
            Set<Entity> userEntities=new HashSet<Entity>();
            for (Entity e : entities) {
                if (e.getUser().getUserLogin().equals(username)) {
                    userEntities.add(e);
                }
            }

            if (userEntities.size()==1) {
                Entity topEntity=entities.iterator().next();
                logger.info("Found top-level entity name="+topEntity.getName()+" , now changing attributes");
                Entity screenTree=annotationBean.getEntityTree(topEntity.getId());
                EntityAttribute pEa=annotationBean.getEntityAttributeByName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
                EntityAttribute nEa=annotationBean.getEntityAttributeByName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH);

                EntityUtils.replaceAllAttributeTypesInEntityTree(screenTree, pEa, nEa, new EntityUtils.SaveUnit() {
                    public void saveUnit(Object o) throws Exception {
                        EntityData ed=(EntityData)o;
                        logger.info("Saving attribute change for ed="+ed.getId());
                        annotationBean.saveOrUpdateEntityData(ed);
                    }
                }
                );
                logger.info("Done replacement, now saving");
                annotationBean.saveOrUpdateEntity(topEntity);
                logger.info("Done with save");
            } else {
                logger.error("Found more than one top-level entity - this is unexpected - exiting");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void runFileTreeLoaderPipeline(String user, String rootDirectoryPath, String topLevelFolderName) {
        try {
            Task task = new FileTreeLoaderPipelineTask(new HashSet<Node>(), user, new ArrayList<Event>(),
                    new HashSet<TaskParameter>(), rootDirectoryPath, topLevelFolderName);
            task.setJobName("File Tree Loader Pipeline Task");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("FileTreeLoader", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    public void runTicPipeline(String user, String rootDirectoryPath){
        try {
            TicTask task = new TicTask(new HashSet<Node>(), user, new ArrayList<Event>(), new HashSet<TaskParameter>());
            task.setParameter(TicTask.PARAM_inputFile, rootDirectoryPath);
            task.setJobName("Transcription Imaging Consortium Pipeline Task");
            task = (TicTask) EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("TranscriptionImagingConsortium", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}