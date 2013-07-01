package org.janelia.it.jacs.compute.mbean;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanRemote;
import org.janelia.it.jacs.compute.service.entity.FastLoadArtifactService;
import org.janelia.it.jacs.compute.service.entity.SampleDataCompressionService;
import org.janelia.it.jacs.compute.service.entity.SampleTrashCompactorService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.utility.GenericTask;
import org.janelia.it.jacs.model.user_data.Node;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class SampleDataManager implements SampleDataManagerMBean {

    private static final Logger logger = Logger.getLogger(SampleDataManager.class);

    // -----------------------------------------------------------------------------------------------------
    // Maintenance Pipelines    
    // -----------------------------------------------------------------------------------------------------

    public void runAllSampleMaintenancePipelines() {
        try {
            logger.info("Building list of users with samples...");
            Set<String> subjectKeys = new HashSet<String>();
            for(Entity sample : EJBFactory.getLocalEntityBean().getEntitiesByTypeName(EntityConstants.TYPE_SAMPLE)) {
                subjectKeys.add(sample.getOwnerKey());
            }
            logger.info("Found users with samples: "+subjectKeys);
            for(String subjectKey : subjectKeys) {
                logger.info("Queuing maintenance pipelines for "+subjectKey);
                runUserSampleMaintenancePipelines(subjectKey);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void runUserSampleMaintenancePipelines(String user) {
        try {
            HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
            Task task = new GenericTask(new HashSet<Node>(), user, new ArrayList<Event>(), 
                    taskParameters, "sampleMaintenancePipeline", "Sample Maintenance Pipeline");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("SampleMaintenancePipeline", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void runSampleCleaning(String user, Boolean testRun) {
        try {
            HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
            taskParameters.add(new TaskParameter(SampleTrashCompactorService.PARAM_testRun, Boolean.toString(testRun), null)); 
            Task task = new GenericTask(new HashSet<Node>(), user, new ArrayList<Event>(), 
                    taskParameters, "sampleCleaning", "Sample Cleaning");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("SampleCleaning", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public void runSampleTrashCompactor(String user, Boolean testRun) {
        try {
            HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
            taskParameters.add(new TaskParameter(SampleTrashCompactorService.PARAM_testRun, Boolean.toString(testRun), null)); 
            Task task = new GenericTask(new HashSet<Node>(), user, new ArrayList<Event>(), 
                    taskParameters, "sampleTrashCompactor", "Sample Trash Compactor");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("SampleTrashCompactor", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public void runSampleDataCompression(String user, Boolean testRun) {
        try {
            HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
            taskParameters.add(new TaskParameter(SampleDataCompressionService.PARAM_testRun, Boolean.toString(testRun), null)); 
            Task task = new GenericTask(new HashSet<Node>(), user, new ArrayList<Event>(), 
                    taskParameters, "sampleDataCompression", "Sample Data Compression");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("SampleDataCompression", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void runSingleSampleDataCompression(String sampleId) {
        try {
            Entity sample = EJBFactory.getLocalEntityBean().getEntityById(sampleId);
            if (sample==null) throw new IllegalArgumentException("Entity with id "+sampleId+" does not exist");
            HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
            taskParameters.add(new TaskParameter("root entity id", sampleId, null)); 
            Task task = new GenericTask(new HashSet<Node>(), sample.getOwnerKey(), new ArrayList<Event>(), 
                    taskParameters, "singleSampleDataCompression", "Single Sample Data Compression");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("SampleDataCompression", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public void runSampleImageRegistration(String user) {
        try {
            HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
            Task task = new GenericTask(new HashSet<Node>(), user, new ArrayList<Event>(), 
                    taskParameters, "sampleImageRegistration", "Sample Image Registration");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("SampleImageRegistration", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    // -----------------------------------------------------------------------------------------------------
    // Generic confocal image processing pipelines
    // -----------------------------------------------------------------------------------------------------

    public void runAllDataSetPipelines(String runMode, Boolean reuseProcessing) {
        try {
            logger.info("Building list of users with data sets...");
            Set<String> subjectKeys = new HashSet<String>();
            for(Entity dataSet : EJBFactory.getLocalEntityBean().getEntitiesByTypeName(EntityConstants.TYPE_DATA_SET)) {
                subjectKeys.add(dataSet.getOwnerKey());
            }
            logger.info("Found users with data sets: "+subjectKeys);
            for(String subjectKey : subjectKeys) {
                logger.info("Queuing data set pipelines for "+subjectKey);
                runUserDataSetPipelines(subjectKey, null, runMode, reuseProcessing);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public void runUserDataSetPipelines(String username, String dataSetName, String runMode, Boolean reuseProcessing) {
        try {
            HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
            taskParameters.add(new TaskParameter("run mode", runMode, null)); 
            taskParameters.add(new TaskParameter("reuse processing", reuseProcessing.toString(), null));
            if ((dataSetName != null) && (dataSetName.trim().length() > 0)) {
                taskParameters.add(new TaskParameter("data set name", dataSetName, null));
            }
            Task task = new GenericTask(new HashSet<Node>(), username, new ArrayList<Event>(), 
                    taskParameters, "userDatSetPipelines", "User Data Set Pipelines");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("FlyLightUserDataSetPipelines", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void runSampleFolder(String folderId, Boolean reuseProcessing) {
        try {
            Entity entity = EJBFactory.getLocalEntityBean().getEntityById(folderId);
            if (entity==null) throw new IllegalArgumentException("Entity with id "+folderId+" does not exist");
            EJBFactory.getLocalEntityBean().loadLazyEntity(entity, false);
            for(Entity child : entity.getChildren()) {
                if (EntityConstants.TYPE_FOLDER.equals(child.getEntityType().getName())) {
                    runSampleFolder(child.getId().toString(), reuseProcessing);
                }
                else if (EntityConstants.TYPE_SAMPLE.equals(child.getEntityType().getName())) {
                    logger.info("Running sample: "+child.getName()+" (id="+child.getId()+")");
                    runSamplePipelines(child.getId().toString(), reuseProcessing);  
                    Thread.sleep(1000); // Sleep so that the logs are a little cleaner
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void runSamplePipelines(String sampleId, Boolean reuseProcessing) {
        try {
            Entity sample = EJBFactory.getLocalEntityBean().getEntityById(sampleId);
            if (sample==null) throw new IllegalArgumentException("Entity with id "+sampleId+" does not exist");
            HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
            taskParameters.add(new TaskParameter("sample entity id", sampleId, null)); 
            taskParameters.add(new TaskParameter("reuse processing", reuseProcessing.toString(), null)); 
            Task task = new GenericTask(new HashSet<Node>(), sample.getOwnerKey(), new ArrayList<Event>(), 
                    taskParameters, "sampleAllPipelines", "Sample All Pipelines");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("GSPS_CompleteSamplePipeline", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public void runConfiguredSamplePipeline(String sampleEntityId, String configurationName, Boolean reuseProcessing) {
        try {
            Entity sampleEntity = EJBFactory.getLocalEntityBean().getEntityById(sampleEntityId);
            if (sampleEntity==null) throw new IllegalArgumentException("Entity with id "+sampleEntityId+" does not exist");
            HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
            taskParameters.add(new TaskParameter("sample entity id", sampleEntityId, null)); 
            taskParameters.add(new TaskParameter("reuse processing", reuseProcessing.toString(), null)); 
            Task task = new GenericTask(new HashSet<Node>(), sampleEntity.getOwnerKey(), new ArrayList<Event>(), 
                    taskParameters, "configuredSamplePipeline", "Configured Sample Pipeline");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("PipelineConfig_"+configurationName, task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public void runNeuronSeparationPipeline(String resultEntityId) {
        try {
            Entity resultEntity = EJBFactory.getLocalEntityBean().getEntityById(resultEntityId);
            if (resultEntity==null) throw new IllegalArgumentException("Entity with id "+resultEntityId+" does not exist");
            HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
            taskParameters.add(new TaskParameter("result entity id", resultEntityId, null)); 
            Task task = new GenericTask(new HashSet<Node>(), resultEntity.getOwnerKey(), new ArrayList<Event>(), 
                    taskParameters, "separationPipeline", "Separation Pipeline");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("PipelineHarness_FlyLightSeparation", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public void runSingleFastLoadArtifactPipeline(String separationEntityId) {
        try {
            Entity entity = EJBFactory.getLocalEntityBean().getEntityById(separationEntityId);
            if (entity==null) throw new IllegalArgumentException("Entity with id "+separationEntityId+" does not exist");
            HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
            taskParameters.add(new TaskParameter(FastLoadArtifactService.PARAM_separationId, separationEntityId, null)); 
            Task task = new GenericTask(new HashSet<Node>(), entity.getOwnerKey(), new ArrayList<Event>(), 
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
    
    public void runSingleMaskChanArtifactPipeline(String separationEntityId) {
        try {
            Entity entity = EJBFactory.getLocalEntityBean().getEntityById(separationEntityId);
            if (entity==null) throw new IllegalArgumentException("Entity with id "+separationEntityId+" does not exist");
            HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
            taskParameters.add(new TaskParameter(FastLoadArtifactService.PARAM_separationId, separationEntityId, null)); 
            Task task = new GenericTask(new HashSet<Node>(), entity.getOwnerKey(), new ArrayList<Event>(), 
                    taskParameters, "maskChanArtifactPipeline", "Mask Chan Artifact Pipeline");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("MaskChanArtifactSinglePipeline", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public void runCompleteMaskChanArtifactPipeline(String user) {
        try {
            Task task = new GenericTask(new HashSet<Node>(), user, new ArrayList<Event>(), 
                    new HashSet<TaskParameter>(), "maskChanArtifactPipeline", "Mask Chan Artifact Pipeline");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("MaskChanArtifactCompletePipeline", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void runSingleSampleArchival(String sampleEntityId) {
        try {
            Entity sampleEntity = EJBFactory.getLocalEntityBean().getEntityById(sampleEntityId);
            HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
            taskParameters.add(new TaskParameter("sample entity id", sampleEntityId, null)); 
            Task task = new GenericTask(new HashSet<Node>(), sampleEntity.getOwnerKey(), new ArrayList<Event>(), 
                    taskParameters, "singleSampleArchival", "Single Sample Archival");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("SyncSampleToArchive", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public void runCompleteSampleArchival(String user) {
        try {
            HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
            Task task = new GenericTask(new HashSet<Node>(), user, new ArrayList<Event>(), 
                    taskParameters, "completeSampleArchival", "Complete Sample Archival");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("CompleteSampleArchivalService", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void create3DTileMicroscopeSamples(String user, String destinationFolderName) {
        try {
            // Two main areas for data
            String[] rootPaths = new String[]{"/groups/mousebrainmicro/mousebrainmicro/render"};
            EntityBeanRemote e = EJBFactory.getRemoteEntityBean();
            // Parameters
            String subjectKey = "user:"+user;

            // Main script
            Set<Entity> folders = e.getEntitiesByName(subjectKey, destinationFolderName);
            Entity folder;
            if (folders!=null && folders.size()>0) {
                folder = folders.iterator().next();
            }
            else {
                folder = newEntity(destinationFolderName, EntityConstants.TYPE_FOLDER, subjectKey, true);
                folder = e.saveOrUpdateEntity(subjectKey, folder);
            }

            // Loop through the main areas and pull out the data directories.  Create entities for them if necessary
            for (String rootPath : rootPaths) {
                File[] rootPathDataDirs = (new File(rootPath)).listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File file) {
                        return file.isDirectory();
                    }
                });
                for (File tmpData : rootPathDataDirs) {
                    // If they exist do nothing
                    Set<Entity> testFolders = e.getEntitiesByName(subjectKey, tmpData.getName());
                    if (null!=testFolders && testFolders.size()>0) continue;
                    // else add in the new data
                    Entity sample = newEntity(tmpData.getName(), EntityConstants.TYPE_3D_TILE_MICROSCOPE_SAMPLE, subjectKey, false);
                    sample.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, tmpData.getAbsolutePath());
                    sample = e.saveOrUpdateEntity(subjectKey, sample);
                    System.out.println("Saved sample as "+sample.getId());
                    e.addEntityToParent(subjectKey, folder.getId(), sample.getId(), folder.getMaxOrderIndex() + 1, EntityConstants.ATTRIBUTE_ENTITY);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    public Entity newEntity(String name, String entityTypeName, String ownerKey, boolean isCommonRoot) throws ComputeException {
        Date createDate = new Date();
        Entity entity = new Entity();
        entity.setName(name);
        entity.setOwnerKey(ownerKey);
        entity.setCreationDate(createDate);
        entity.setUpdatedDate(createDate);
        entity.setEntityType(EJBFactory.getRemoteEntityBean().getEntityTypeByName(entityTypeName));
        if (isCommonRoot) {
            entity.setValueByAttributeName(EntityConstants.ATTRIBUTE_COMMON_ROOT, "Common Root");
        }
        return entity;
    }


}