package org.janelia.it.jacs.compute.mbean;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.service.entity.FastLoadArtifactService;
import org.janelia.it.jacs.compute.service.entity.OrphanAnnotationCheckerService;
import org.janelia.it.jacs.compute.service.entity.SampleFileNodeSyncService;
import org.janelia.it.jacs.compute.service.fileDiscovery.FlyScreenDiscoveryService;
import org.janelia.it.jacs.compute.service.fly.MaskGuideService;
import org.janelia.it.jacs.compute.service.fly.ScreenSampleLineCoordinationService;
import org.janelia.it.jacs.compute.service.mongodb.MongoDbLoadService;
import org.janelia.it.jacs.compute.service.solr.SolrIndexingService;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityAttribute;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.fileDiscovery.*;
import org.janelia.it.jacs.model.tasks.fly.FlyScreenPatternAnnotationTask;
import org.janelia.it.jacs.model.tasks.fly.MaskSampleAnnotationTask;
import org.janelia.it.jacs.model.tasks.neuron.NeuronMergeTask;
import org.janelia.it.jacs.model.tasks.tic.SingleTicTask;
import org.janelia.it.jacs.model.tasks.utility.GenericTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.shared.annotation.MaskAnnotationDataManager;
import org.janelia.it.jacs.shared.annotation.PatternAnnotationDataManager;
import org.janelia.it.jacs.shared.utils.EntityUtils;

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
    
    public void runAnnotationImport(String user, String annotationsPath, String ontologyName) {
        try {
        	HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
        	taskParameters.add(new TaskParameter("annotations filepath", annotationsPath, null)); 
        	taskParameters.add(new TaskParameter("ontology name", ontologyName, null)); 
        	Task task = new GenericTask(new HashSet<Node>(), user, new ArrayList<Event>(), 
        			taskParameters, "annotationImport", "annotationImport");
            task.setJobName("Annotation Import Pipeline Task");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("AnnotationImportPipeline", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void runScreenScoresLoading(String user) {
        try {
        	HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
        	Task task = new GenericTask(new HashSet<Node>(), user, new ArrayList<Event>(), 
        			taskParameters, "screenScoresLoading", "Screen Scores Loading");
            task.setJobName("Screen Scores Loading Task");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("ScreenScoresLoadingPipeline", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void runScreenScoresLoading2(String user) {
        try {
        	HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
        	Task task = new GenericTask(new HashSet<Node>(), user, new ArrayList<Event>(), 
        			taskParameters, "screenScoresLoading2", "Screen Scores Loading 2");
            task.setJobName("Screen Scores Loading 2 Task");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("ScreenScoresLoadingPipeline2", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public void runScreenScoresLoading3(String user, String rejectsPath, String acceptsPath) {
        try {
        	HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
        	taskParameters.add(new TaskParameter("rejects file path", rejectsPath, null));
        	taskParameters.add(new TaskParameter("accepts file path", acceptsPath, null));
        	Task task = new GenericTask(new HashSet<Node>(), user, new ArrayList<Event>(), 
        			taskParameters, "screenScoresLoading3", "Screen Scores Loading 3");
            task.setJobName("Screen Scores Loading 3 Task");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("ScreenScoresLoadingPipeline3", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public void runScreenScoresExport(String user, String outputFilepath) {
        try {
        	HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
        	taskParameters.add(new TaskParameter("output filepath", outputFilepath, null));
        	Task task = new GenericTask(new HashSet<Node>(), user, new ArrayList<Event>(), 
        			taskParameters, "screenScoresExport", "Screen Scores Export");
            task.setJobName("Screen Scores Export Task");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("ScreenScoresExportPipeline", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void runSplitLinesLoading(String user, String topLevelFolderName, String representativesPath, String splitConstructsPath) {
        try {
        	HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
        	taskParameters.add(new TaskParameter("top level folder name", topLevelFolderName, null));
        	taskParameters.add(new TaskParameter("representatives filepath", representativesPath, null));
        	taskParameters.add(new TaskParameter("split constructs filepath", splitConstructsPath, null)); 
        	Task task = new GenericTask(new HashSet<Node>(), user, new ArrayList<Event>(), 
        			taskParameters, "splitLinesLoading", "Split Lines Loading");
            task.setJobName("Split Lines Loading Task");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("SplitLinesLoadingPipeline", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public void runNeo4jSync(Boolean clearDb) {
        try {
        	HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
        	taskParameters.add(new TaskParameter(MongoDbLoadService.PARAM_clearDb, Boolean.toString(clearDb), null)); 
        	Task task = new GenericTask(new HashSet<Node>(), "system", new ArrayList<Event>(), 
        			taskParameters, "neo4jSync", "Neo4j Sync");
            task.setJobName("Neo4j Sync Task");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("Neo4jSync", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public void runMongoDbSync(Boolean clearDb) {
        try {
        	HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
        	taskParameters.add(new TaskParameter(MongoDbLoadService.PARAM_clearDb, Boolean.toString(clearDb), null)); 
        	Task task = new GenericTask(new HashSet<Node>(), "system", new ArrayList<Event>(), 
        			taskParameters, "mongoDbSync", "MongoDb Sync");
            task.setJobName("MongoDB Sync Task");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("MongoDbSync", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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

    public void runNeuronMergeTest(String taskOwner, String separationEntityId, String commaSeparatedNeuronFragmentList) {
        try {
            NeuronMergeTask task = new NeuronMergeTask(new HashSet<Node>(), taskOwner, new ArrayList<org.janelia.it.jacs.model.tasks.Event>(), new HashSet<TaskParameter>());
            task.setJobName("Neuron Merge Task");
            task.setParameter(NeuronMergeTask.PARAM_separationEntityId, separationEntityId);
            task.setParameter(NeuronMergeTask.PARAM_commaSeparatedNeuronFragmentList, commaSeparatedNeuronFragmentList);
            task = (NeuronMergeTask) EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("NeuronMerge", task.getObjectId());
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void runSolrTreeIndexing(Long rootId) {
    	try {
            EJBFactory.getLocalSolrBean().indexAllEntitiesInTree(rootId);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public void runOrphanAnnotationCheckerService(String user, Boolean deleteAnnotationsMissingTargets, Boolean deleteAnnotationsMissingTerms) {
        try {
        	HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
        	taskParameters.add(new TaskParameter(OrphanAnnotationCheckerService.PARAM_removeAnnotationsMissingTargets, Boolean.toString(deleteAnnotationsMissingTargets), null)); 
        	taskParameters.add(new TaskParameter(OrphanAnnotationCheckerService.PARAM_removeAnnotationsMissingTerms, Boolean.toString(deleteAnnotationsMissingTerms), null)); 
        	Task task = new GenericTask(new HashSet<Node>(), user, new ArrayList<Event>(), 
        			taskParameters, "orphanAnnotationChecker", "Orphan Annotation Checker");
            task.setJobName("Orphan Annotation Checker Task");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("OrphanAnnotationChecker", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public void runSampleCleaningService(String user, Boolean testRun) {
        try {
        	HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
        	taskParameters.add(new TaskParameter(SampleFileNodeSyncService.PARAM_testRun, Boolean.toString(testRun), null)); 
        	Task task = new GenericTask(new HashSet<Node>(), user, new ArrayList<Event>(), 
        			taskParameters, "sampleCleaning", "Sample Cleaning");
            task.setJobName("Sample Cleaning Task");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("SampleCleaning", task.getObjectId());
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

    public void runSingleFastLoadArtifactPipeline(String user, String separationEntityId) {
        try {
        	HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
        	taskParameters.add(new TaskParameter(FastLoadArtifactService.PARAM_separationId, separationEntityId, null)); 
        	Task task = new GenericTask(new HashSet<Node>(), user, new ArrayList<Event>(), 
        			taskParameters, "fastLoadArtifactPipeline", "Fast Load Artifact Pipeline");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("FastLoadArtifactSinglePipeline", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void runCompleteFastLoadArtifactPipeline(String user) {
        try {
        	Task task = new GenericTask(new HashSet<Node>(), user, new ArrayList<Event>(), 
        			new HashSet<TaskParameter>(), "fastLoadArtifactPipeline", "Fast Load Artifact Pipeline");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("FastLoadArtifactCompletePipeline", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    
    public void runLeetSageBasedDataPipeline(String user, String topLevelFolderName, String imageFamily, Boolean refreshProcessing, Boolean refreshAlignment, Boolean refreshSeparation) {
        try {
        	Task task = new MCFODataPipelineTask(new HashSet<Node>(), 
            		user, new ArrayList<Event>(), new HashSet<TaskParameter>(), null,
            		topLevelFolderName, refreshProcessing, refreshAlignment, refreshSeparation,
            		"/groups/leet/leetimg/leetlab/lineage/"+imageFamily+"/confocalStacks", imageFamily);
            task.setJobName("Leet Data Pipeline Task");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("LeetSageBasedDataPipeline", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public void runCentralBrainDataPipeline(String user, String topLevelFolderName, Boolean refreshProcessing, Boolean refreshAlignment, Boolean refreshSeparation) {
        try {
        	Task task = new MCFODataPipelineTask(new HashSet<Node>(), 
            		user, new ArrayList<Event>(), new HashSet<TaskParameter>(), null,
            		topLevelFolderName, refreshProcessing, refreshAlignment, refreshSeparation,
                    "/groups/flylight/flylight/flip/confocalStacks","flylight_flip");
            task.setJobName("Central Brain Data Pipeline Task");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("CentralBrainDataPipeline", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void runAlignWholeBrainDataPipeline(String user, Boolean refreshAlignment, Boolean refreshSeparation) {
        try {
        	Task task = new MCFODataPipelineTask(new HashSet<Node>(), 
            		user, new ArrayList<Event>(), new HashSet<TaskParameter>(), null,
            		null, null, refreshAlignment, refreshSeparation,"/groups/flylight/flylight/flip/confocalStacks",
                    "flylight_flip");
            task.setJobName("Align Whole Brain Data Pipeline Task");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("AlignWholeBrainDataPipeline", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void runAlignWholeBrainSamplePipeline(String sampleEntityId, Boolean refreshAlignment, Boolean refreshSeparation) {
        try {
        	Entity sampleEntity = EJBFactory.getLocalEntityBean().getEntityById(sampleEntityId);
        	if (sampleEntity==null) throw new IllegalArgumentException("Entity with id "+sampleEntityId+" does not exist");
        	Task task = new MCFOSamplePipelineTask(new HashSet<Node>(), 
        			sampleEntity.getUser().getUserLogin(), new ArrayList<Event>(), new HashSet<TaskParameter>(), 
        			sampleEntityId, false, refreshAlignment, refreshSeparation);
            task.setJobName("Align Whole Brain Sample Pipeline Task");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("AlignWholeBrainSamplePipeline", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void runTwoChanDataPipeline(String user) {
        try {
        	Task task = new MCFODataPipelineTask(new HashSet<Node>(), 
            		user, new ArrayList<Event>(), new HashSet<TaskParameter>(), null,
            		null, null, null, null,"/groups/flylight/flylight/flip/confocalStacks", "flylight_flip");
            task.setJobName("Two Channel Data Pipeline Task");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("TwoChanDataPipeline", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public void runTwoChanSamplePipeline(String sampleEntityId) {
        try {
        	Entity sampleEntity = EJBFactory.getLocalEntityBean().getEntityById(sampleEntityId);
        	if (sampleEntity==null) throw new IllegalArgumentException("Entity with id "+sampleEntityId+" does not exist");
        	Task task = new MCFOSamplePipelineTask(new HashSet<Node>(), 
        			sampleEntity.getUser().getUserLogin(), new ArrayList<Event>(), new HashSet<TaskParameter>(), 
        			sampleEntityId, null, null, null);
            task.setJobName("Two Channel Sample Pipeline Task");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("TwoChanSamplePipeline", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public void runMCFODataPipeline(String user, String topLevelFolderName, Boolean refreshProcessing, Boolean refreshAlignment, Boolean refreshSeparation) {
        try {
        	Task task = new MCFODataPipelineTask(new HashSet<Node>(), 
            		user, new ArrayList<Event>(), new HashSet<TaskParameter>(), null,
            		topLevelFolderName, refreshProcessing, refreshAlignment, refreshSeparation,"/groups/flylight/flylight/flip/confocalStacks",
                    "flylight_flip");
            task.setJobName("MultiColor FlipOut Data Pipeline Task");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("MCFODataPipeline", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public void runMCFOSamplePipeline(String sampleEntityId, Boolean refreshProcessing, Boolean refreshAlignment, Boolean refreshSeparation) {
        try {
        	Entity sampleEntity = EJBFactory.getLocalEntityBean().getEntityById(sampleEntityId);
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
        	Entity sampleEntity = EJBFactory.getLocalEntityBean().getEntityById(sampleEntityId);
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
        	Entity sourceEntity = EJBFactory.getLocalEntityBean().getEntityById(sourceEntityId);
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
    
//    public void deleteEntityById(String entityId) {
//     try {
//         Long id=new Long(entityId);
//         EJBFactory.getLocalEntityBean().deleteEntityById(id);
//     } catch (Exception ex) {
//         ex.printStackTrace();
//     }
//    }

    public void performScreenPipelineSurgery(String username) {
        try {
            final EntityBeanLocal entityBean = EJBFactory.getLocalEntityBean();
            Set<Entity> entities=entityBean.getEntitiesByName("FlyLight Screen Data");
            Set<Entity> userEntities=new HashSet<Entity>();
            for (Entity e : entities) {
                if (e.getUser().getUserLogin().equals(username)) {
                    userEntities.add(e);
                }
            }

            if (userEntities.size()==1) {
                Entity topEntity=entities.iterator().next();
                logger.info("Found top-level entity name="+topEntity.getName()+" , now changing attributes");
                Entity screenTree=entityBean.getEntityTree(topEntity.getId());
                EntityAttribute pEa=entityBean.getEntityAttributeByName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
                EntityAttribute nEa=entityBean.getEntityAttributeByName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH);

                EntityUtils.replaceAllAttributeTypesInEntityTree(screenTree, pEa, nEa, new EntityUtils.SaveUnit() {
                    public void saveUnit(Object o) throws Exception {
                        EntityData ed=(EntityData)o;
                        logger.info("Saving attribute change for ed="+ed.getId());
                        entityBean.saveOrUpdateEntityData(ed);
                    }
                }
                );
                logger.info("Done replacement, now saving");
                entityBean.saveOrUpdateEntity(topEntity);
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

    public void runTicPipeline(String user, String dirOfInputFile, String inputFileName, String transformationMatrixFile,
                               String borderValue, String correctionFile, String microscopeSettingsFile){
        try {
            SingleTicTask task = new SingleTicTask(new HashSet<Node>(), user, new ArrayList<Event>(), new HashSet<TaskParameter>());
            task.setParameter(SingleTicTask.PARAM_inputFilePrefix, "/groups/scicomp/jacsData/saffordTest/tic/StackSeries20Copy.tif");
            task.setParameter(SingleTicTask.PARAM_transformationFile, "/groups/scicomp/jacsData/saffordTest/tic/PSF3stack.mat");
            task.setParameter(SingleTicTask.PARAM_intensityCorrectionFactorFile, "/groups/scicomp/jacsData/saffordTest/tic/kpos-int-cor.mat");
            task.setJobName("Transcription Imaging Consortium Pipeline Task");
            task = (SingleTicTask) EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("TranscriptionImagingConsortium", task.getObjectId());
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void createPatternAnnotationQuantifierSummaryFile() {
        try {
            Map<Entity, Map<String, Double>> entityQuantifierMap=EJBFactory.getLocalAnnotationBean().getPatternAnnotationQuantifiers();
            PatternAnnotationDataManager.createPatternAnnotationQuantifierSummaryFile(entityQuantifierMap);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void createMaskSummaryFile(String maskFolderName) {
        try {
            Map<Entity, Map<String, Double>> entityQuantifierMap=EJBFactory.getLocalAnnotationBean().getMaskQuantifiers(maskFolderName);
            MaskAnnotationDataManager maskManager=new MaskAnnotationDataManager();
            String resourceDirString= SystemConfigurationProperties.getString("MaskSampleAnnotation.ResourceDir");
            String quantifierSummaryFilename= SystemConfigurationProperties.getString("FlyScreen.PatternAnnotationQuantifierSummaryFile");
            File summaryFile=new File(resourceDirString + File.separator+maskFolderName, quantifierSummaryFilename);
            File nameIndexFile=new File(resourceDirString + File.separator+maskFolderName, "maskNameIndex.txt");
            maskManager.loadMaskCompartmentList(nameIndexFile);
            maskManager.createMaskAnnotationQuantifierSummaryFile(summaryFile, entityQuantifierMap);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void runMaskAnnotationPipeline(String user, String maskFolderName, Boolean refresh) {
        try {
            Task task = new MaskSampleAnnotationTask(new HashSet<Node>(),
                    user, new ArrayList<Event>(), new HashSet<TaskParameter>(),
                    maskFolderName, refresh);
            task.setJobName("Mask Annotation Task");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("MaskSampleAnnotation", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void runMaskGuideService(String user, String maskFolderName, Boolean refresh) {
        try {
            HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
            taskParameters.add(new TaskParameter(MaskGuideService.PARAM_maskFolderName, maskFolderName, null));
            taskParameters.add(new TaskParameter(MaskGuideService.PARAM_refresh, refresh.toString(), null));
            Task task = new GenericTask(new HashSet<Node>(), user, new ArrayList<Event>(),
                    taskParameters, "runMaskGuideService", "Mask Guide Service");
            task.setJobName("Mask Guide Task");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("MaskGuide", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void runSlowImportTask(String parentDirPath, String topLevelFolderName, String owner) {
        try {
            String process = "FileTreeLoader";
            File parentDir = new File(parentDirPath);
            if (parentDir.isDirectory() && null!=parentDir.listFiles()) {
                File[] childDirs = parentDir.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File file) {
                        return file.isDirectory();
                    }
                });
                for (File file : childDirs) {
                    FileTreeLoaderPipelineTask task = new FileTreeLoaderPipelineTask(new HashSet<Node>(),
                            owner, new ArrayList<Event>(), new HashSet<TaskParameter>(), file.getAbsolutePath(), topLevelFolderName);
                    task.setJobName("Import Files Task");
                    task = (FileTreeLoaderPipelineTask) EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
                    EJBFactory.getLocalComputeBean().submitJob(process, task.getObjectId());
                    Thread.sleep(120000);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }
}