package org.janelia.it.jacs.compute.mbean;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.service.entity.SageQiScoreSyncService;
import org.janelia.it.jacs.compute.service.entity.SampleTrashCompactorService;
import org.janelia.it.jacs.compute.service.entity.sample.SampleRetirementService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.utility.*;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.rmi.RemoteException;
import java.util.*;

public class SampleDataManager implements SampleDataManagerMBean {

    private static final Logger log = Logger.getLogger(SampleDataManager.class);

    private void saveAndRunTask(String user, String processName, String displayName) throws Exception {
        HashSet<TaskParameter> taskParameters = new HashSet<>();
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
    
    private void addExtraParams(HashSet<TaskParameter> taskParameters, String extraParams) throws Exception {
        if (StringUtils.isEmpty(extraParams)) return;
        for (String extraParam : extraParams.split(",")) {
            String[] p = extraParam.split("=");
            if (p.length!=2) {
                throw new Exception("Unable to parse extra parameter: "+extraParam);
            }
            taskParameters.add(new TaskParameter(p[0], p[1], null));
        }
    }
    
    // -----------------------------------------------------------------------------------------------------
    // Maintenance Pipelines    
    // -----------------------------------------------------------------------------------------------------
    
    public void runAllSampleMaintenancePipelines() {
        try {
            log.info("Building list of users with data sets...");
            Set<String> subjectKeys = new TreeSet<>();
            for(Entity sample : EJBFactory.getLocalEntityBean().getEntitiesByTypeName(EntityConstants.TYPE_DATA_SET)) {
                subjectKeys.add(sample.getOwnerKey());
            }
            List<String> sortedKeys = new ArrayList<>(subjectKeys);
            Collections.sort(sortedKeys);
            log.info("Found users with data sets: " + sortedKeys);
            for(String subjectKey : sortedKeys) {
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
            HashSet<TaskParameter> taskParameters = new HashSet<>();
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
            HashSet<TaskParameter> taskParameters = new HashSet<>();
            taskParameters.add(new TaskParameter(SampleTrashCompactorService.PARAM_testRun, Boolean.toString(testRun), null)); 
            saveAndRunTask(user, processName, displayName, taskParameters);
        } 
        catch (Exception ex) {
            log.error("Error running pipeline", ex);
        }
    }
    
    public void runAllSampleDataCompression(String compressionType){
        try {
            log.info("Building list of users with data sets...");
            Set<String> subjectKeys = new TreeSet<>();
            for(Entity dataSet : EJBFactory.getLocalEntityBean().getEntitiesByTypeName(EntityConstants.TYPE_DATA_SET)) {
                subjectKeys.add(dataSet.getOwnerKey());
            }
            log.info("Found users with data sets: " + subjectKeys);
            for(String subjectKey : subjectKeys) {
                log.info("Queuing sample data compression for "+subjectKey);
                runSampleDataCompression(subjectKey, null, compressionType);
            }
        }
        catch (Exception e) {
            log.error("Error running All Sample Data Compression",e);
        }
    }

    public void runSampleDataCompression(String user, String dataSetName, String compressionType) {
        try {
            String processName = "SampleCompression";
            String displayName = "Sample Data Compression";
            if ("".equals(dataSetName)) {dataSetName=null;}
            HashSet<TaskParameter> taskParameters = new HashSet<>();
            taskParameters.add(new TaskParameter("data set name", dataSetName, null));
            taskParameters.add(new TaskParameter("compression type", compressionType, null));
            saveAndRunTask(user, processName, displayName, taskParameters);
        } 
        catch (Exception ex) {
            log.error("Error running pipeline", ex);
        }
    }

    public void runSingleSampleDataCompression(String sampleId, String compressionType) {
        try {
            String processName = "PostPipeline_SampleCompression";
            String displayName = "Single Sample Data Compression";
            Entity sample = EJBFactory.getLocalEntityBean().getEntityById(sampleId);
            if (sample==null) throw new IllegalArgumentException("Entity with id "+sampleId+" does not exist");
            HashSet<TaskParameter> taskParameters = new HashSet<>();
            taskParameters.add(new TaskParameter("sample entity id", sampleId, null));
            taskParameters.add(new TaskParameter("compression type", compressionType, null));
            String user = sample.getOwnerKey();
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
    
    public void runSampleRetirement(String user, String dataSetName, String maxSamples, Boolean testRun) {
        try {
            String processName = "SampleRetirementPipeline";
            String displayName = "Sample Retirement Pipeline";
            HashSet<TaskParameter> taskParameters = new HashSet<>();
            taskParameters.add(new TaskParameter(SampleRetirementService.PARAM_testRun, Boolean.toString(testRun), null)); 
            taskParameters.add(new TaskParameter("data set name", dataSetName, null));
            taskParameters.add(new TaskParameter("max samples", maxSamples, null));
            saveAndRunTask(user, processName, displayName, taskParameters);
        } 
        catch (Exception ex) {
            log.error("Error running pipeline", ex);
        }
    }
    
    public void runAllSampleRetirement(String maxSamplesPerUser, Boolean testRun) {
        try {
            Set<String> subjectKeys = new TreeSet<>();
            for(Entity dataSet : EJBFactory.getLocalEntityBean().getEntitiesByTypeName(EntityConstants.TYPE_DATA_SET)) {
                subjectKeys.add(dataSet.getOwnerKey());
            }
            for(String subjectKey : subjectKeys) {
                runSampleRetirement(subjectKey, null, maxSamplesPerUser, testRun);
            }
        } 
        catch (Exception ex) {
            log.error("Error running pipeline", ex);
        }
    }

    public void runSingleSampleArchival(String sampleEntityId) {
        try {
            Entity sampleEntity = EJBFactory.getLocalEntityBean().getEntityById(sampleEntityId);
            HashSet<TaskParameter> taskParameters = new HashSet<>();
            taskParameters.add(new TaskParameter("sample entity id", sampleEntityId, null));
            String processName = "SyncSampleToArchive";
            String displayName = "Single Sample Archival";
            saveAndRunTask(sampleEntity.getOwnerKey(), processName, displayName, taskParameters);
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
    
    public void runSyncSampleToScality(String sampleEntityId, String filetypes, Boolean deleteSourceFiles) {
        try {
            Entity sampleEntity = EJBFactory.getLocalEntityBean().getEntityById(sampleEntityId);
            HashSet<TaskParameter> taskParameters = new HashSet<>();
            taskParameters.add(new TaskParameter("sample entity id", sampleEntityId, null));
            taskParameters.add(new TaskParameter("file types", filetypes, null));
            taskParameters.add(new TaskParameter("delete source files", deleteSourceFiles.toString(), null));
            String processName = "SyncSampleToScality";
            String displayName = "Sync Sample to Scality";
            saveAndRunTask(sampleEntity.getOwnerKey(), processName, displayName, taskParameters);
        } 
        catch (Exception ex) {
            log.error("Error running pipeline", ex);
        }
    }
    
    public void runSyncDataSetToScality(String user, String dataSetName, String filetypes, Boolean deleteSourceFiles) {
        try {
            if ("".equals(dataSetName)) {dataSetName=null;}
            HashSet<TaskParameter> taskParameters = new HashSet<>();
            taskParameters.add(new TaskParameter("data set name", dataSetName, null));
            taskParameters.add(new TaskParameter("file types", filetypes, null));
            taskParameters.add(new TaskParameter("delete source files", deleteSourceFiles.toString(), null));
            String processName = "SyncUserFilesToScality";
            String displayName = "Sync User Files to Scality";
            saveAndRunTask(user, processName, displayName, taskParameters);
        } 
        catch (Exception ex) {
            log.error("Error running pipeline", ex);
        }
    }

    // todo Proved to be too slow.  Used the commented out main method below to generate insert statements adding canceled event (insanely faster)
    public void cancelAllIncompleteUserTasks(String user){
        try {
            log.info("Building list of users with data sets...");
            Set<String> subjectKeys = new TreeSet<>();
            for(Entity dataSet : EJBFactory.getLocalEntityBean().getEntitiesByTypeName(EntityConstants.TYPE_DATA_SET)) {
                subjectKeys.add(dataSet.getOwnerKey());
            }
            log.info("Found users with data sets: " + subjectKeys);
            log.info("Canceling incomplete tasks");
            for(String subjectKey : subjectKeys) {
                if (null!=user && !EntityUtils.getNameFromSubjectKey(subjectKey).equals(user)) {continue;}
                log.info("  Canceling tasks for user "+subjectKey);
                int c = EJBFactory.getLocalComputeBean().cancelIncompleteTasksForUser(subjectKey);
                if (c>0) {
                    log.info("  Canceled "+c+" incomplete tasks for "+subjectKey);
                }
            }
            log.info("Completed cancelAllIncompleteUserTasks");
        }
        catch (Exception ex) {
            log.error("Error clearing data set pipeline tasks", ex);
        }
    }

    // -----------------------------------------------------------------------------------------------------
    // Generic confocal image processing pipelines
    // -----------------------------------------------------------------------------------------------------
    
    public void cancelAllIncompleteDataSetPipelineTasks() {
        try {
            log.info("Building list of users with data sets...");
            Set<String> subjectKeys = new TreeSet<>();
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
    
    public String runAllDataSetPipelines(String runMode, Boolean reuseSummary, Boolean reuseProcessing, Boolean reusePost, Boolean reuseAlignment, Boolean force) {
        try {
            log.info("Building list of users with data sets...");
            Set<String> subjectKeys = new TreeSet<>();
            for(Entity dataSet : EJBFactory.getLocalEntityBean().getEntitiesByTypeName(EntityConstants.TYPE_DATA_SET)) {
                subjectKeys.add(dataSet.getOwnerKey());
            }
            log.info("Found users with data sets: "+subjectKeys);
            StringBuilder sb = new StringBuilder();
            for(String subjectKey : subjectKeys) {
                log.info("Queuing data set pipelines for "+subjectKey);
                String ret = runUserDataSetPipelines(subjectKey, null, true, runMode, reuseSummary, reuseProcessing, reusePost, reuseAlignment, force);
                if (sb.length()>0) sb.append(",\n");
                sb.append(subjectKey).append(": ").append(ret);
            }
            return sb.toString();
        } 
        catch (Exception ex) {
            log.error("Error running pipeline", ex);
            return "Error: "+ex.getMessage();
        }
    }
    
    public String runUserDataSetPipelines(String user, String dataSetName, Boolean runSampleDiscovery, String runMode, Boolean reuseSummary, Boolean reuseProcessing, Boolean reusePost, Boolean reuseAlignment, Boolean force) {
        try {
            String processName = "GSPS_UserDataSetPipelines";
            String displayName = "User Data Set Pipelines";
	            HashSet<TaskParameter> taskParameters = new HashSet<>();
	            taskParameters.add(new TaskParameter("run mode", runMode, null));
            if (runSampleDiscovery!=null) {
                taskParameters.add(new TaskParameter("run sample discovery", runSampleDiscovery.toString(), null));
            }
            if (reuseSummary!=null) {
                taskParameters.add(new TaskParameter("reuse summary", reuseSummary.toString(), null));
            }
            if (reuseProcessing!=null) {
                taskParameters.add(new TaskParameter("reuse processing", reuseProcessing.toString(), null));
            }
            if (reusePost!=null) {
                taskParameters.add(new TaskParameter("reuse post", reusePost.toString(), null));
            }
            if (reuseAlignment!=null) {
                taskParameters.add(new TaskParameter("reuse alignment", reuseAlignment.toString(), null));
            }
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

    public void runSampleFolder(String folderId, Boolean reuseSummary, Boolean reuseProcessing, Boolean reusePost, Boolean reuseAlignment, String extraParams) {
        try {
            Entity entity = EJBFactory.getLocalEntityBean().getEntityById(folderId);
            if (entity==null) throw new IllegalArgumentException("Entity with id "+folderId+" does not exist");
            EJBFactory.getLocalEntityBean().loadLazyEntity(entity, false);
            for(Entity child : entity.getOrderedChildren()) {
                if (EntityConstants.TYPE_FOLDER.equals(child.getEntityTypeName())) {
                    log.info("runSampleFolder - Running folder: "+child.getName()+" (id="+child.getId()+")");
                    runSampleFolder(child.getId().toString(), reuseSummary, reuseProcessing, reusePost, reuseAlignment, extraParams);
                }
                else if (EntityConstants.TYPE_SAMPLE.equals(child.getEntityTypeName())) {
                    log.info("runSampleFolder - Running sample: "+child.getName()+" (id="+child.getId()+")");
                    runSamplePipelines(child.getId().toString(), reuseSummary, reuseProcessing, reusePost, reuseAlignment, extraParams);  
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

    public void runSamplePipelines(String sampleId, Boolean reuseSummary, Boolean reuseProcessing, Boolean reusePost, Boolean reuseAlignment, String extraParams) {
        try {
            String processName = "GSPS_CompleteSamplePipeline";
            Entity sample = EJBFactory.getLocalEntityBean().getEntityById(sampleId);
            if (sample==null) throw new IllegalArgumentException("Entity with id "+sampleId+" does not exist");
            HashSet<TaskParameter> taskParameters = new HashSet<>();
            taskParameters.add(new TaskParameter("sample entity id", sampleId, null));
            if (reuseSummary!=null) {
                taskParameters.add(new TaskParameter("reuse summary", reuseSummary.toString(), null));
            }
            if (reuseProcessing!=null) {
                taskParameters.add(new TaskParameter("reuse processing", reuseProcessing.toString(), null));
            }
            if (reusePost!=null) {
                taskParameters.add(new TaskParameter("reuse post", reusePost.toString(), null));
            }
            if (reuseAlignment!=null) {
                taskParameters.add(new TaskParameter("reuse alignment", reuseAlignment.toString(), null));
            }
            addExtraParams(taskParameters, extraParams);
            saveAndRunTask(sample.getOwnerKey(), processName, processName, taskParameters);
        } 
        catch (Exception ex) {
            log.error("Error running pipeline", ex);
        }
    }
    
    public void runConfiguredSamplePipeline(String sampleEntityId, String configurationName, Boolean reuseSummary, Boolean reuseProcessing, Boolean reusePost, Boolean reuseAlignment) {
        try {
            String processName = "PipelineConfig_"+configurationName;
            Entity sample = EJBFactory.getLocalEntityBean().getEntityById(sampleEntityId);
            if (sample==null) throw new IllegalArgumentException("Entity with id "+sampleEntityId+" does not exist");
            HashSet<TaskParameter> taskParameters = new HashSet<>();
            taskParameters.add(new TaskParameter("sample entity id", sampleEntityId, null)); 
            if (reuseSummary!=null) {
                taskParameters.add(new TaskParameter("reuse summary", reuseSummary.toString(), null));
            }
            if (reuseProcessing!=null) {
                taskParameters.add(new TaskParameter("reuse processing", reuseProcessing.toString(), null));
            }
            if (reusePost!=null) {
                taskParameters.add(new TaskParameter("reuse post", reusePost.toString(), null));
            }
            if (reuseAlignment!=null) {
                taskParameters.add(new TaskParameter("reuse alignment", reuseAlignment.toString(), null));
            }
            String user = sample.getOwnerKey();
            saveAndRunTask(user, processName, processName, taskParameters);
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
            HashSet<TaskParameter> taskParameters = new HashSet<>();
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
            HashSet<TaskParameter> taskParameters = new HashSet<>();
            taskParameters.add(new TaskParameter("separation id 1", separationId1, null));
            taskParameters.add(new TaskParameter("separation id 2", separationId2, null));
            String user = result2.getOwnerKey();
            saveAndRunTask(user, processName, displayName, taskParameters);
        } 
        catch (Exception ex) {
            log.error("Error running pipeline", ex);
        }
    }

    public void applyProcessToDataset(String user, String dataSetName, String parentOrChildren, String processName, String extraParams) {
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
            HashSet<TaskParameter> taskParameters = new HashSet<>();
            taskParameters.add(new TaskParameter("data set name", dataSetName, null)); 
            taskParameters.add(new TaskParameter("process def name", processName, null));
            taskParameters.add(new TaskParameter("parent or children", parentOrChildren, null));
            addExtraParams(taskParameters, extraParams);
            saveAndRunTask(user, parentProcessName, displayName, taskParameters);
        } 
        catch (Exception ex) {
            log.error("Error running pipeline", ex);
        }
    }
    
    public void applyProcessToSample(String sampleEntityId, String processName, String extraParams) {
        try {
            String displayName = "Apply Process To Sample";
            Entity sample = EJBFactory.getLocalEntityBean().getEntityById(sampleEntityId);
            if (sample==null) throw new IllegalArgumentException("Entity with id "+sampleEntityId+" does not exist");
            HashSet<TaskParameter> taskParameters = new HashSet<>();
            taskParameters.add(new TaskParameter("sample entity id", sampleEntityId, null)); 
            addExtraParams(taskParameters, extraParams);
            String user = sample.getOwnerKey();
            saveAndRunTask(user, processName, displayName, taskParameters);
        } 
        catch (Exception ex) {
            log.error("Error running pipeline", ex);
        }
    }

    public void applyProcessToSamplesInFolder(String folderId, String processName, String extraParams) {
        try {
            Entity entity = EJBFactory.getLocalEntityBean().getEntityById(folderId);
            if (entity==null) throw new IllegalArgumentException("Entity with id "+folderId+" does not exist");
            EJBFactory.getLocalEntityBean().loadLazyEntity(entity, false);
            for(Entity child : entity.getOrderedChildren()) {
                if (EntityConstants.TYPE_FOLDER.equals(child.getEntityTypeName())) {
                    log.info("runSampleFolder - Running folder: "+child.getName()+" (id="+child.getId()+")");
                    applyProcessToSamplesInFolder(child.getId().toString(), processName, extraParams);
                }
                else if (EntityConstants.TYPE_SAMPLE.equals(child.getEntityTypeName())) {
                    log.info("runSampleFolder - Running sample: "+child.getName()+" (id="+child.getId()+")");
                    applyProcessToSample(child.getId().toString(), processName, extraParams);  
                    Thread.sleep(1000); // Sleep so that the logs are a little cleaner
                }
                else {
                    log.info("applyProcessToSamplesInFolder - Ignoring child which is not a folder or sample: "+child.getName()+" (id="+child.getId()+")");
                }
            }
        } catch (Exception ex) {
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
     * Example file exists in /groups/jacs/jacsShare/saffordTest/leetLSMs28days.txt (or older file)
     *                        /groups/jacs/jacsShare/saffordTest/leetLSMs7days.txt  (or older file)
     */
    public void bzipLSMCompressionService(String filePath, String owner, String compressMode) {
        try {
            BZipTestTask bzipTask = new BZipTestTask(owner, new ArrayList<Event>(), filePath, compressMode);
            if (BZipTestTask.MODE_COMPRESS.equals(compressMode) || BZipTestTask.MODE_DECOMPRESS.equals(compressMode)) {
                bzipTask = (BZipTestTask) EJBFactory.getLocalComputeBean().saveOrUpdateTask(bzipTask);
                EJBFactory.getLocalComputeBean().submitJob("BzipTestService", bzipTask.getObjectId());
            }
        }
        catch (DaoException | RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to point to a file of object id's and move them from one ring into another.  (Probably should parameterize the rings)
     * Example file exists in /groups/jacs/jacsShare/saffordTest/scalityMigration.txt
     *
     */
    public void scalityMigrationService(String filePath) {
        try {
            ScalityMigrationTask migrationTask = new ScalityMigrationTask("system", new ArrayList<Event>(), filePath);
            migrationTask = (ScalityMigrationTask) EJBFactory.getLocalComputeBean().saveOrUpdateTask(migrationTask);
            EJBFactory.getLocalComputeBean().submitJob("ScalityMigration", migrationTask.getObjectId());
        }
        catch (DaoException | RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void visuallyLosslessCorrectionService(String filePath, String debug) {
        try {
            Scanner scanner = new Scanner(new File(filePath));
            HashSet<String> userList = new HashSet<>();
            while (scanner.hasNextLine()) {
                String tmpLine = scanner.nextLine().trim();
                String originalPDB = tmpLine.substring(0,tmpLine.lastIndexOf(".h5j"))+".v3dpbd";
                File tmpOriginalPBD = new File(originalPDB);
                File tmpOriginalVL = new File(tmpLine);
                if (!tmpOriginalVL.exists()) {
                    log.debug("Can't find the original VL file: "+tmpLine);
                }
                if (!tmpOriginalPBD.exists()) {
                    log.debug("Can't find the original PBD file: "+originalPDB);
                }

                tmpLine = tmpLine.substring(tmpLine.indexOf("filestore/")+10);
                String tmpUser = tmpLine.substring(0,tmpLine.indexOf("/"));
                if (!userList.contains(tmpUser)) {
                    userList.add(tmpUser);
                    log.debug("Adding user "+tmpUser);
                }
            }

            for (String targetOwner : userList) {
                VLCorrectionTask vlcorrectionTask = new VLCorrectionTask("system", new ArrayList<Event>(), filePath, targetOwner, Boolean.valueOf(debug));
                vlcorrectionTask = (VLCorrectionTask) EJBFactory.getLocalComputeBean().saveOrUpdateTask(vlcorrectionTask);
                EJBFactory.getLocalComputeBean().submitJob("VLCorrectionService", vlcorrectionTask.getObjectId());
            }
        }
        catch (DaoException | RemoteException | FileNotFoundException e) {
            e.printStackTrace();
        }
    }

//    /**
//     * To cancel stranded jobs do the following:
//     * 1. Get the tasks not in a terminal state
//     *
//         @export on;
//         @export set filename="/Users/saffordt/Desktop/AllStrandedTasksnernaKonrad.txt" CsvIncludeColumnHeader="false";
//         select task_event.task_id, task_event.event_no, task_event.description, task_event.event_timestamp, task_event.event_type, CURRENT_TIMESTAMP
//         from task join task_event on task.task_id = task_event.task_id
//         where task.task_owner='nerna' and event_no in (
//         select max(event_no) as event_no
//         from task_event task_event1 where  task_event1.task_id=task_event.task_id
//         order by task.task_id asc ) and event_type != 'completed' and task_event.event_type!='error' and task_event.event_type!='canceled';
//         @export off;
//
//      * 2. Run the main method below to create the new cancel events
//      * 3. Run the insert statements and provide a valid output file path
//         @cd /Users/saffordt/Desktop/;
//         @run AllStrandedTasksnernaKonrad.txt.update.sql
//
//         and
//
//         /Users/saffordt/Desktop/AllStrandedTasksnernaKonrad.txt.update.sql.log
//      *
//      *
//     */
//    public static void main(String[] args) {
//        String filePath = "/Users/saffordt/Desktop/AllStrandedTasksdicksonlab1115.txt";
//        File tmpFile = new File(filePath);
//        try (FileWriter writer = new FileWriter(new File(filePath+".update.sql"))){
//            Scanner scanner = new Scanner(tmpFile);
//            while (scanner.hasNextLine()) {
//                String tmpLine = scanner.nextLine().trim();
//                String[] pieces = tmpLine.split("\t");
//                writer.write("insert into task_event (task_id,event_no,description,event_timestamp,event_type) values ("+
//                        pieces[0]+","+(Integer.valueOf(pieces[1])+1)+",'canceled','"+pieces[5]+"','canceled');\n");
//            }
//        }
//        catch (IOException e) {
//            e.printStackTrace();
//        }
//    }


    public void runSageLoader(String owner, String item, String configPath, String grammarPath, String lab,
            String debug, String lock) {
        log.info("Heard call for SageLoader API");
        try {
            final String line = null; // line parameter only needed for development environment testing
            SageLoaderTask task = new SageLoaderTask(owner,
                                                     new ArrayList<Event>(),
                                                     item,
                                                     line,
                                                     configPath,
                                                     grammarPath,
                                                     lab,
                                                     debug,
                                                     lock);
            task = (SageLoaderTask) EJBFactory.getRemoteComputeBean().saveOrUpdateTask(task);
            EJBFactory.getRemoteComputeBean().submitJob("SageLoader", task.getObjectId());
            log.info("Done runSageLoader call (" + owner + "," + item + ")");
        }
        catch (Exception e) {
            log.error("runSageLoader: failed execution", e);
        }
    }

    public void runSageArtifactExport(String owner, String releaseName) {
        try {
            Subject subject = EJBFactory.getLocalComputeBean().getSubjectByNameOrKey(owner);
            if (subject==null) throw new IllegalArgumentException("User with name "+owner+" does not exist");
            List<Entity> releases = EJBFactory.getLocalEntityBean().getEntitiesByNameAndTypeName(subject.getKey(), releaseName, EntityConstants.TYPE_FLY_LINE_RELEASE);
            if (releases.isEmpty()) throw new IllegalArgumentException("Release with name "+releaseName+" does not exist");
            if (releases.size()>1) throw new IllegalArgumentException("More than one release with name "+releaseName);
            Entity release = releases.get(0);
            String processName = "SageArtifactExport";
            String displayName = "Sage Artifact Export";
            HashSet<TaskParameter> taskParameters = new HashSet<>();
            taskParameters.add(new TaskParameter("release entity id", release.getId().toString(), null)); 
            saveAndRunTask(subject.getKey(), processName, displayName, taskParameters);
        } 
        catch (Exception ex) {
            log.error("Error running SAGE Artifact Export", ex);
        }
    }

    public void runSageArtifactExport() {
        try {
            for(Entity releaseEntity : EJBFactory.getLocalEntityBean().getEntitiesByTypeName(EntityConstants.TYPE_FLY_LINE_RELEASE)) {
                String processName = "SageArtifactExport";
                String displayName = "Sage Artifact Export";
                HashSet<TaskParameter> taskParameters = new HashSet<>();
                taskParameters.add(new TaskParameter("release entity id", releaseEntity.getId().toString(), null)); 
                saveAndRunTask(releaseEntity.getOwnerKey(), processName, displayName, taskParameters);
            }
        } 
        catch (Exception ex) {
            log.error("Error running SAGE Artifact Export", ex);
        }
    }
    
    public void runSageQiScoreSync(Boolean testRun) {
        try {
            String processName = "SageQiScoreSync";
            String displayName = "Sage Qi Score Sync";
            HashSet<TaskParameter> taskParameters = new HashSet<>();
            taskParameters.add(new TaskParameter(SageQiScoreSyncService.PARAM_testRun, Boolean.toString(testRun), null)); 
            saveAndRunTask("system", processName, displayName, taskParameters);
        } 
        catch (Exception ex) {
            log.error("Error running SAGE Qi Score Sync", ex);
        }
    }
}