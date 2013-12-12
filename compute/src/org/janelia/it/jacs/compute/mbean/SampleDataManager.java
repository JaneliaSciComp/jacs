package org.janelia.it.jacs.compute.mbean;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.service.entity.SampleDataCompressionService;
import org.janelia.it.jacs.compute.service.entity.SampleTrashCompactorService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.utility.BZipTestTask;
import org.janelia.it.jacs.model.tasks.utility.GenericTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.shared.utils.StringUtils;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SampleDataManager implements SampleDataManagerMBean {

    private static final Logger log = Logger.getLogger(SampleDataManager.class);

    private void saveAndRunTask(String user, String processName, String displayName) throws Exception {
        HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
        saveAndRunTask(user, processName, displayName, taskParameters);
    }
    
    private void saveAndRunTask(String user, String processName, String displayName, HashSet<TaskParameter> parameters) throws Exception {
        GenericTask task = new GenericTask(new HashSet<Node>(), user, new ArrayList<Event>(), 
                parameters, processName, displayName);
        saveAndRunTask(task);
    }
    
    private void saveAndRunTask(GenericTask task) throws Exception {
        task = (GenericTask)EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
        EJBFactory.getLocalComputeBean().submitJob(task.getTaskName(), task.getObjectId());
    }
    
    // -----------------------------------------------------------------------------------------------------
    // Maintenance Pipelines    
    // -----------------------------------------------------------------------------------------------------
    
    public void runAllSampleMaintenancePipelines() {
        try {
            log.info("Building list of users with samples...");
            Set<String> subjectKeys = new HashSet<String>();
            for(Entity sample : EJBFactory.getLocalEntityBean().getEntitiesByTypeName(EntityConstants.TYPE_SAMPLE)) {
                subjectKeys.add(sample.getOwnerKey());
            }
            log.info("Found users with samples: "+subjectKeys);
            for(String subjectKey : subjectKeys) {
                log.info("Queuing maintenance pipelines for "+subjectKey);
                runUserSampleMaintenancePipelines(subjectKey);
            }
        } 
        catch (Exception ex) {
            log.error("Error running pipeline", ex);
        }
    }

    public void runUserSampleMaintenancePipelines(String user) {
        try {
            String processName = "SampleMaintenancePipeline";
            String displayName = "Sample Maintenance Pipeline";
            saveAndRunTask(user, processName, displayName);
        } 
        catch (Exception ex) {
            log.error("Error running pipeline", ex);
        }
    }

    public void runSampleCleaning(String user, Boolean testRun) {
        try {
            String processName = "SampleCleaning";
            String displayName = "Sample Cleaning";
            HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
            taskParameters.add(new TaskParameter(SampleTrashCompactorService.PARAM_testRun, Boolean.toString(testRun), null)); 
            saveAndRunTask(user, processName, displayName, taskParameters);
        } 
        catch (Exception ex) {
            log.error("Error running pipeline", ex);
        }
    }
    
    public void runSampleTrashCompactor(String user, Boolean testRun) {
        try {
            String processName = "SampleTrashCompactor";
            String displayName = "Sample Trash Compactor";
            HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
            taskParameters.add(new TaskParameter(SampleTrashCompactorService.PARAM_testRun, Boolean.toString(testRun), null)); 
            saveAndRunTask(user, processName, displayName, taskParameters);
        } 
        catch (Exception ex) {
            log.error("Error running pipeline", ex);
        }
    }
    
    public void runSampleDataCompression(String user, Boolean testRun) {
        try {
            String processName = "SampleDataCompression";
            String displayName = "Sample Data Compression";
            HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
            taskParameters.add(new TaskParameter(SampleDataCompressionService.PARAM_testRun, Boolean.toString(testRun), null)); 
            saveAndRunTask(user, processName, displayName, taskParameters);
        } 
        catch (Exception ex) {
            log.error("Error running pipeline", ex);
        }
    }

    public void runSingleSampleDataCompression(String sampleId) {
        try {
            String processName = "SampleDataCompression";
            String displayName = "Single Sample Data Compression";
            Entity sample = EJBFactory.getLocalEntityBean().getEntityById(sampleId);
            if (sample==null) throw new IllegalArgumentException("Entity with id "+sampleId+" does not exist");
            HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
            taskParameters.add(new TaskParameter("root entity id", sampleId, null)); 
            String user = sample.getOwnerKey();
            saveAndRunTask(user, processName, displayName, taskParameters);
        } 
        catch (Exception ex) {
            log.error("Error running pipeline", ex);
        }
    }
    
    public void runResultImageRegistration(String resultId) {
        try {
            String processName = "ResultImageRegistration";
            String displayName = "Result Image Registration";
            Entity result = EJBFactory.getLocalEntityBean().getEntityById(resultId);
            if (result==null) throw new IllegalArgumentException("Entity with id "+resultId+" does not exist");
            HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
            taskParameters.add(new TaskParameter("result entity id", resultId, null)); 
            String user = result.getOwnerKey();
            saveAndRunTask(user, processName, displayName, taskParameters);
        } 
        catch (Exception ex) {
            log.error("Error running pipeline", ex);
        }
    }

    public void runSampleImageRegistration(String user) {
        try {
            String processName = "SampleImageRegistration";
            String displayName = "Sample Image Registration";
            saveAndRunTask(user, processName, displayName);
        } 
        catch (Exception ex) {
            log.error("Error running pipeline", ex);
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
        } 
        catch (Exception ex) {
            log.error("Error running pipeline", ex);
        }
    }
    
    public void runCompleteSampleArchival(String user) {
        try {
            String processName = "CompleteSampleArchivalService";
            String displayName = "Complete Sample Archival";
            saveAndRunTask(user, processName, displayName);
        } 
        catch (Exception ex) {
            log.error("Error running pipeline", ex);
        }
    }
    
    // -----------------------------------------------------------------------------------------------------
    // Generic confocal image processing pipelines
    // -----------------------------------------------------------------------------------------------------
    
    public void cancelAllIncompleteDataSetPipelineTasks() {
        try {
            log.info("Building list of users with data sets...");
            Set<String> subjectKeys = new HashSet<String>();
            for(Entity dataSet : EJBFactory.getLocalEntityBean().getEntitiesByTypeName(EntityConstants.TYPE_DATA_SET)) {
                subjectKeys.add(dataSet.getOwnerKey());
            }
            String processName = "GSPS_UserDataSetPipelines";
            log.info("Cancelling incomplete "+processName+" tasks");
            for(String subjectKey : subjectKeys) {
                log.info("  Checking tasks for user "+subjectKey);
                int c = EJBFactory.getLocalComputeBean().cancelIncompleteTasksWithName(subjectKey, processName);
                if (c>0) {
                    log.info("  Canceled "+c+" incomplete tasks");
                }
            }
            log.info("Completed cancelAllIncompleteDataSetPipelineTasks");
        } 
        catch (Exception ex) {
            log.error("Error clearing data set pipeline tasks", ex);
        }
    }
    
    public String runAllDataSetPipelines(String runMode, Boolean reuseProcessing, Boolean reuseAlignment, Boolean force) {
        try {
            log.info("Building list of users with data sets...");
            Set<String> subjectKeys = new HashSet<String>();
            for(Entity dataSet : EJBFactory.getLocalEntityBean().getEntitiesByTypeName(EntityConstants.TYPE_DATA_SET)) {
                subjectKeys.add(dataSet.getOwnerKey());
            }
            log.info("Found users with data sets: "+subjectKeys);
            
            StringBuilder sb = new StringBuilder();
            for(String subjectKey : subjectKeys) {
                log.info("Queuing data set pipelines for "+subjectKey);
                String ret = runUserDataSetPipelines(subjectKey, null, runMode, reuseProcessing, reuseAlignment, force);
                if (sb.length()>0) sb.append(",\n");
                sb.append(subjectKey+": "+ret+"");
            }
            return sb.toString();
        } 
        catch (Exception ex) {
            log.error("Error running pipeline", ex);
            return "Error: "+ex.getMessage();
        }
    }
    
    public String runUserDataSetPipelines(String user, String dataSetName, String runMode, Boolean reuseProcessing, Boolean reuseAlignment, Boolean force) {
        try {
            String processName = "GSPS_UserDataSetPipelines";
            String displayName = "User Data Set Pipelines";
            HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
            taskParameters.add(new TaskParameter("run mode", runMode, null)); 
            taskParameters.add(new TaskParameter("reuse processing", reuseProcessing.toString(), null));
            taskParameters.add(new TaskParameter("reuse alignment", reuseAlignment.toString(), null));
            if ((dataSetName != null) && (dataSetName.trim().length() > 0)) {
                taskParameters.add(new TaskParameter("data set name", dataSetName, null));
            }
            if (!force) {
		        Task task = EJBFactory.getLocalComputeBean().getMostRecentTaskWithNameAndParameters(user, processName, taskParameters);
		        if (task!=null) {
		        	log.info("Checking most recent similar task: "+task.getObjectId());
		            if (!task.isDone()) {
		            	log.info("Pipeline is still running (last event: "+task.getLastEvent().getEventType()+"). Skipping run.");
		                return "Error: pipeline is already running";
		            }
		            List<Task> childTasks = EJBFactory.getLocalComputeBean().getChildTasksByParentTaskId(task.getObjectId());
		            boolean allDone = true;
		            for(Task subtask : childTasks) {
		                if (!subtask.isDone()) {
		                    allDone = false;
		                    break;
		                }
		            }
		            if (!allDone) {
		            	log.info("One of the subtasks is not done, skipping run.");
		                return "Error: pipeline subtasks are still running";
		            }
		        }
            }
            saveAndRunTask(user, processName, displayName, taskParameters);
            return "Success";
        } 
        catch (Exception ex) {
            log.error("Error running pipeline", ex);
            return "Error: "+ex.getMessage();
        }
    }

    public void runSampleFolder(String folderId, Boolean reuseProcessing, Boolean reuseAlignment) {
        try {
            Entity entity = EJBFactory.getLocalEntityBean().getEntityById(folderId);
            if (entity==null) throw new IllegalArgumentException("Entity with id "+folderId+" does not exist");
            EJBFactory.getLocalEntityBean().loadLazyEntity(entity, false);
            for(Entity child : entity.getOrderedChildren()) {
                if (EntityConstants.TYPE_FOLDER.equals(child.getEntityTypeName())) {
                    log.info("runSampleFolder - Running folder: "+child.getName()+" (id="+child.getId()+")");
                    runSampleFolder(child.getId().toString(), reuseProcessing, reuseAlignment);
                }
                else if (EntityConstants.TYPE_SAMPLE.equals(child.getEntityTypeName())) {
                    log.info("runSampleFolder - Running sample: "+child.getName()+" (id="+child.getId()+")");
                    runSamplePipelines(child.getId().toString(), reuseProcessing, reuseAlignment);  
                    Thread.sleep(1000); // Sleep so that the logs are a little cleaner
                }
                else {
                    log.info("runSampleFolder - Ignoring child which is not a folder or sample: "+child.getName()+" (id="+child.getId()+")");
                }
            }
        } catch (Exception ex) {
            log.error("Error running pipeline", ex);
        }
    }

    public void runSamplePipelines(String sampleId, Boolean reuseProcessing, Boolean reuseAlignment) {
        try {
            String processName = "GSPS_CompleteSamplePipeline";
            String displayName = "Sample All Pipelines";
            Entity sample = EJBFactory.getLocalEntityBean().getEntityById(sampleId);
            if (sample==null) throw new IllegalArgumentException("Entity with id "+sampleId+" does not exist");
            HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
            taskParameters.add(new TaskParameter("sample entity id", sampleId, null)); 
            taskParameters.add(new TaskParameter("reuse processing", reuseProcessing.toString(), null)); 
            taskParameters.add(new TaskParameter("reuse alignment", reuseAlignment.toString(), null));
            String user = sample.getOwnerKey();
            saveAndRunTask(user, processName, displayName, taskParameters);
        } 
        catch (Exception ex) {
            log.error("Error running pipeline", ex);
        }
    }
    
    public void runConfiguredSamplePipeline(String sampleEntityId, String configurationName, Boolean reuseProcessing, Boolean reuseAlignment) {
        try {
            String processName = "PipelineConfig_"+configurationName;
            String displayName = "Configured Sample Pipeline";
            Entity sample = EJBFactory.getLocalEntityBean().getEntityById(sampleEntityId);
            if (sample==null) throw new IllegalArgumentException("Entity with id "+sampleEntityId+" does not exist");
            HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
            taskParameters.add(new TaskParameter("sample entity id", sampleEntityId, null)); 
            taskParameters.add(new TaskParameter("reuse processing", reuseProcessing.toString(), null)); 
            taskParameters.add(new TaskParameter("reuse alignment", reuseAlignment.toString(), null));
            String user = sample.getOwnerKey();
            saveAndRunTask(user, processName, displayName, taskParameters);
        } 
        catch (Exception ex) {
            log.error("Error running pipeline", ex);
        }
    }
    
    public void runNeuronSeparationPipeline(String resultEntityId) {
        try {
            String processName = "PipelineHarness_FlyLightSeparation";
            String displayName = "Standalone Neuron Separation Pipeline";
            Entity result = EJBFactory.getLocalEntityBean().getEntityById(resultEntityId);
            if (result==null) throw new IllegalArgumentException("Entity with id "+resultEntityId+" does not exist");
            HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
            taskParameters.add(new TaskParameter("result entity id", resultEntityId, null)); 
            String user = result.getOwnerKey();
            saveAndRunTask(user, processName, displayName, taskParameters);
        } 
        catch (Exception ex) {
            log.error("Error running pipeline", ex);
        }
    }

    public void runNeuronSeparationMapping(String separationId1, String separationId2) {
        try {
            String processName = "NeuronSeparationMapping";
            String displayName = "Standalone Neuron Separation Mapping";
            Entity result1 = EJBFactory.getLocalEntityBean().getEntityById(separationId1);
            if (result1==null) throw new IllegalArgumentException("Entity with id "+separationId1+" does not exist");
            Entity result2 = EJBFactory.getLocalEntityBean().getEntityById(separationId2);
            if (result2==null) throw new IllegalArgumentException("Entity with id "+separationId2+" does not exist");
            HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
            taskParameters.add(new TaskParameter("separation id 1", separationId1, null));
            taskParameters.add(new TaskParameter("separation id 2", separationId2, null));
            String user = result2.getOwnerKey();
            saveAndRunTask(user, processName, displayName, taskParameters);
        } 
        catch (Exception ex) {
            log.error("Error running pipeline", ex);
        }
    }

    public void applyProcessToDataset(String user, String dataSetName, String parentOrChildren, String processName) {
        try {
            if (!StringUtils.isEmpty(dataSetName)) {
                Subject subject = EJBFactory.getLocalComputeBean().getSubjectByNameOrKey(user);
                if (subject==null) throw new IllegalArgumentException("User with name "+user+" does not exist");
                List<Entity> dataSets = EJBFactory.getLocalEntityBean().getEntitiesByNameAndTypeName(subject.getKey(),
                        dataSetName, EntityConstants.TYPE_DATA_SET);
                if (dataSets.isEmpty()) throw new IllegalArgumentException("Data set with name "+dataSetName+" does not exist");
                if (dataSets.size()>1) throw new IllegalArgumentException("More than one data set with name "+dataSetName+" exists");   
            }
            String parentProcessName = "GSPS_ApplyProcessToSamples";
            String displayName = "Apply Process To Dataset";
            HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
            taskParameters.add(new TaskParameter("data set name", dataSetName, null)); 
            taskParameters.add(new TaskParameter("process def name", processName, null));
            taskParameters.add(new TaskParameter("parent or children", parentOrChildren, null));
            saveAndRunTask(user, parentProcessName, displayName, taskParameters);
        } 
        catch (Exception ex) {
            log.error("Error running pipeline", ex);
        }
    }
    
    public void applyProcessToSample(String sampleEntityId, String processName) {
        try {
            String displayName = "Apply Process To Sample";
            Entity sample = EJBFactory.getLocalEntityBean().getEntityById(sampleEntityId);
            if (sample==null) throw new IllegalArgumentException("Entity with id "+sampleEntityId+" does not exist");
            HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
            taskParameters.add(new TaskParameter("sample entity id", sampleEntityId, null)); 
            String user = sample.getOwnerKey();
            saveAndRunTask(user, processName, displayName, taskParameters);
        } 
        catch (Exception ex) {
            log.error("Error running pipeline", ex);
        }
    }

    public void runRepairSeparationsPipeline(String user) {
        try {
            String processName = "RepairSeparationsPipeline";
            String displayName = "Repair Separations Pipeline";
            saveAndRunTask(user, processName, displayName);
        } 
        catch (Exception ex) {
            log.error("Error running pipeline", ex);
        }
    }

    public void runRepairSeparationResultsPipeline(String user) {
        try {
            String processName = "RepairSeparationResultsPipeline";
            String displayName = "Repair Separation Results Pipeline";
            saveAndRunTask(user, processName, displayName);
        } 
        catch (Exception ex) {
            log.error("Error running pipeline", ex);
        }
    }

    /**
     * Method to point to an ls file and pull out LSM's to be bzip2'd.
     * Example file exists in /groups/scicomp/jacsData/saffordTest/leetLSMs28days.txt (or older file)
     *                        /groups/scicomp/jacsData/saffordTest/leetLSMs7days.txt  (or older file)
     * @param filePath
     * @param owner
     * @param compressMode
     */
    public void bzipLSMCompressionService(String filePath, String owner, String compressMode) {
        try {
            BZipTestTask bzipTask = new BZipTestTask(owner, new ArrayList<Event>(), filePath, compressMode);
            if (BZipTestTask.MODE_COMPRESS.equals(compressMode) || BZipTestTask.MODE_DECOMPRESS.equals(compressMode)) {
                bzipTask = (BZipTestTask) EJBFactory.getLocalComputeBean().saveOrUpdateTask(bzipTask);
                EJBFactory.getLocalComputeBean().submitJob("BzipTestService", bzipTask.getObjectId());
            }
        }
        catch (DaoException e) {
            e.printStackTrace();
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}