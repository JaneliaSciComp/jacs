package org.janelia.it.jacs.compute.mbean;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EJBFactory;
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

public class SampleDataManager implements SampleDataManagerMBean {

    private static final Logger logger = Logger.getLogger(SampleDataManager.class);

    // -----------------------------------------------------------------------------------------------------
    // Maintenance Pipelines    
    // -----------------------------------------------------------------------------------------------------

    public void runAllSampleMaintenancePipelines() {
        try {
            Set<String> subjectKeys = new HashSet<String>();
            for(Entity dataSet : EJBFactory.getLocalEntityBean().getEntitiesByTypeName(EntityConstants.TYPE_SAMPLE)) {
                subjectKeys.add(dataSet.getOwnerKey());
            }
            for(String subjectKey : subjectKeys) {
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
            task.setJobName("Sample Maintenance Pipeline Task");
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
            task.setJobName("Sample Cleaning Task");
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
            task.setJobName("Sample Trash Compactor Task");
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
            task.setJobName("Sample Data Compression Task");
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
            task.setJobName("Sample Image Registration Task");
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
            Set<String> subjectKeys = new HashSet<String>();
            for(Entity dataSet : EJBFactory.getLocalEntityBean().getEntitiesByTypeName(EntityConstants.TYPE_DATA_SET)) {
                subjectKeys.add(dataSet.getOwnerKey());
            }
            for(String subjectKey : subjectKeys) {
                runUserDataSetPipelines(subjectKey, runMode, reuseProcessing);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public void runUserDataSetPipelines(String username, String runMode, Boolean reuseProcessing) {
        try {
            HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
            taskParameters.add(new TaskParameter("run mode", runMode, null)); 
            taskParameters.add(new TaskParameter("reuse processing", reuseProcessing.toString(), null)); 
            Task task = new GenericTask(new HashSet<Node>(), username, new ArrayList<Event>(), 
                    taskParameters, "userDatSetPipelines", "User Data Set Pipelines");
            task.setJobName("User Data Set Pipelines Task");
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
                    taskParameters, "flylightSampleAllPipelines", "Flylight Sample All Pipelines");
            task.setJobName("Flylight Sample All Pipelines Task");
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
            task.setJobName("Configured Sample Pipeline Task");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("PipelineConfig_"+configurationName, task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public void runNeuronSeparationPipeline(String resultEntityId) {
        try {
            Entity sampleEntity = EJBFactory.getLocalEntityBean().getEntityById(resultEntityId);
            if (sampleEntity==null) throw new IllegalArgumentException("Entity with id "+resultEntityId+" does not exist");
            HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
            taskParameters.add(new TaskParameter("result entity id", resultEntityId, null)); 
            Task task = new GenericTask(new HashSet<Node>(), sampleEntity.getOwnerKey(), new ArrayList<Event>(), 
                    taskParameters, "separationPipeline", "Separation Pipeline");
            task.setJobName("Separation Pipeline Task");
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
}