package org.janelia.it.jacs.compute.mbean;

import java.io.File;
import java.io.FileNotFoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.domain.DomainDAL;
import org.janelia.it.jacs.compute.access.mongodb.DomainDAOManager;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.service.domain.SampleTrashCompactorService;
import org.janelia.it.jacs.compute.service.domain.alignment.SageQiScoreSyncService;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.sample.DataSet;
import org.janelia.it.jacs.model.domain.sample.LineRelease;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.utility.BZipTestTask;
import org.janelia.it.jacs.model.tasks.utility.GenericTask;
import org.janelia.it.jacs.model.tasks.utility.JFSExportTask;
import org.janelia.it.jacs.model.tasks.utility.SageLoaderTask;
import org.janelia.it.jacs.model.tasks.utility.ScalityMigrationTask;
import org.janelia.it.jacs.model.tasks.utility.VLCorrectionTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.shared.utils.StringUtils;

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
    
    private Set<String> getSubjectsWithDataSets() {
        DomainDAO dao = DomainDAOManager.getInstance().getDao();
        log.info("Building list of users with data sets...");
        Set<String> subjectKeys = new TreeSet<>();
        for(DataSet dataSet : dao.getDataSets(null)) {
            subjectKeys.add(dataSet.getOwnerKey());
        }
        return subjectKeys;
    }

    // -----------------------------------------------------------------------------------------------------
    // Maintenance Pipelines    
    // -----------------------------------------------------------------------------------------------------
    
    public void runAllSampleMaintenancePipelines() {
        try {
            Set<String> subjectKeys = getSubjectsWithDataSets();
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
            Set<String> subjectKeys = getSubjectsWithDataSets();
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
            DomainDAO dao = DomainDAOManager.getInstance().getDao();
            Sample sample = dao.getDomainObject(null, Sample.class, new Long(sampleId));
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
    
    public void runSyncSampleToScality(String sampleEntityId, String filetypes, Boolean deleteSourceFiles) {
        try {
            DomainDAO dao = DomainDAOManager.getInstance().getDao();
            Sample sample = dao.getDomainObject(null, Sample.class, new Long(sampleEntityId));
            HashSet<TaskParameter> taskParameters = new HashSet<>();
            taskParameters.add(new TaskParameter("sample entity id", sampleEntityId, null));
            taskParameters.add(new TaskParameter("file types", filetypes, null));
            taskParameters.add(new TaskParameter("delete source files", deleteSourceFiles.toString(), null));
            String processName = "SyncSampleToScality";
            String displayName = "Sync Sample to Scality";
            saveAndRunTask(sample.getOwnerKey(), processName, displayName, taskParameters);
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

    public void runSyncAllLSMsToScality() {
        try {
            Set<String> subjectKeys = getSubjectsWithDataSets();
            for(String subjectKey : subjectKeys) {
                log.info("Queuing scality sync for "+subjectKey);
                runSyncDataSetToScality(subjectKey, null, "lsm", true);
            }
        }
        catch (Exception e) {
            log.error("Error running Sync All LSMs to Scality",e);
        }
    }

    // todo Proved to be too slow.  Used the commented out main method below to generate insert statements adding canceled event (insanely faster)
    public void cancelAllIncompleteUserTasks(String user){
        try {
            Set<String> subjectKeys = getSubjectsWithDataSets();
            log.info("Found users with data sets: " + subjectKeys);
            log.info("Canceling incomplete tasks");
            for(String subjectKey : subjectKeys) {
                if (null!=user && !DomainUtils.getNameFromSubjectKey(subjectKey).equals(user)) {continue;}
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
            Set<String> subjectKeys = getSubjectsWithDataSets();
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
            Set<String> subjectKeys = getSubjectsWithDataSets();
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
            DomainDAL dal = DomainDAL.getInstance();
            TreeNode treeNode = dal.getDomainObject(null, TreeNode.class, new Long(folderId));
            if (treeNode!=null) {
                for(DomainObject child : dal.getDomainObjects(null, treeNode.getChildren())) {
                    if (child instanceof TreeNode) {
                        log.info("runSampleFolder - Running folder: "+child.getName()+" (id="+child.getId()+")");
                        runSampleFolder(child.getId().toString(), reuseSummary, reuseProcessing, reusePost, reuseAlignment, extraParams);
                    }
                    else if (child instanceof Sample) {
                        log.info("runSampleFolder - Running sample: "+child.getName()+" (id="+child.getId()+")");
                        runSamplePipelines(child.getId().toString(), reuseSummary, reuseProcessing, reusePost, reuseAlignment, extraParams);
                        Thread.sleep(1000); // Sleep so that the logs are a little cleaner
                    }
                    else {
                        log.info("runSampleFolder - Ignore child "+child.getType()+": "+child.getName());
                    }
                }
            }
            else {
                throw new IllegalArgumentException("Folder with id "+folderId+" does not exist");
            }
        } catch (Exception ex) {
            log.error("Error running pipeline", ex);
        }
    }

    public void runSamplePipelines(String sampleId, Boolean reuseSummary, Boolean reuseProcessing, Boolean reusePost, Boolean reuseAlignment, String extraParams) {
        try {
            String processName = "GSPS_CompleteSamplePipeline";
            Sample sample = DomainDAL.getInstance().getDomainObject(null, Sample.class, new Long(sampleId));
            if (sample==null) throw new IllegalArgumentException("Sample with id "+sampleId+" does not exist");
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
            Sample sample = DomainDAL.getInstance().getDomainObject(null, Sample.class, new Long(sampleEntityId));
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

    public void applyProcessToDataset(String user, String dataSetName, String processName, String extraParams) {
        try {
            DomainDAL dal = DomainDAL.getInstance();
            if (!StringUtils.isEmpty(dataSetName)) {
                Subject subject = dal.getSubjectByNameOrKey(user);
                if (subject==null) throw new IllegalArgumentException("User with name "+user+" does not exist");
                List<DataSet> dataSets = dal.getDomainObjectsByName(subject.getKey(), DataSet.class, dataSetName);
                if (dataSets.isEmpty()) throw new IllegalArgumentException("Data set with name "+dataSetName+" does not exist");
                if (dataSets.size()>1) throw new IllegalArgumentException("More than one data set with name "+dataSetName+" exists");   
            }
            String parentProcessName = "GSPS_ApplyProcessToSamples";
            String displayName = "Apply Process To Dataset";
            HashSet<TaskParameter> taskParameters = new HashSet<>();
            taskParameters.add(new TaskParameter("data set name", dataSetName, null)); 
            taskParameters.add(new TaskParameter("process def name", processName, null));
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
            Sample sample = DomainDAL.getInstance().getDomainObject(null, Sample.class, new Long(sampleEntityId));
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
            DomainDAL dal =  DomainDAL.getInstance();
            TreeNode treeNode = dal.getDomainObject(null, TreeNode.class, new Long(folderId));
            if (treeNode!=null) {
                for(DomainObject child : dal.getDomainObjects(null, treeNode.getChildren())) {
                    if (child instanceof TreeNode) {
                        log.info("applyProcessToSamplesInFolder - Running folder: "+child.getName()+" (id="+child.getId()+")");
                        applyProcessToSamplesInFolder(child.getId().toString(), processName, extraParams);
                    }
                    else if (child instanceof Sample) {
                        log.info("applyProcessToSamplesInFolder - Running sample: "+child.getName()+" (id="+child.getId()+")");
                        applyProcessToSample(child.getId().toString(), processName, extraParams);
                        Thread.sleep(1000); // Sleep so that the logs are a little cleaner
                    }
                    else {
                        log.info("applyProcessToSamplesInFolder - Ignore child "+child.getType()+": "+child.getName());
                    }
                }
            }
            else {
                throw new IllegalArgumentException("Object set with id "+folderId+" does not exist");
            }
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

    /**
     * Method to point to a file of jfs commands and extract them.
     * Example file exists in /nrs/jacs/jacsData/saffordt/jfsonly.sh
     *
     */
    public void jfsExportService(String filePath) {
        try {
            JFSExportTask exportTask = new JFSExportTask("system", new ArrayList<Event>(), filePath);
            exportTask = (JFSExportTask) EJBFactory.getLocalComputeBean().saveOrUpdateTask(exportTask);
            EJBFactory.getLocalComputeBean().submitJob("JFSExport", exportTask.getObjectId());
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
//        String filePath = "/Users/saffordt/Desktop/AllStrandedTasksnerna04252016.txt";
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

    public void runSyncReleaseFolders(String owner, String releaseName) {
        try {
            DomainDAL dal = DomainDAL.getInstance();
            Subject subject = dal.getSubjectByNameOrKey(owner);
            if (subject==null) throw new IllegalArgumentException("User with name "+owner+" does not exist");
            List<LineRelease> releases = dal.getDomainObjectsByName(subject.getKey(), LineRelease.class, releaseName);
            if (releases.isEmpty()) throw new IllegalArgumentException("Release with name "+releaseName+" does not exist");
            if (releases.size()>1) throw new IllegalArgumentException("More than one release with name "+releaseName);
            LineRelease release = releases.get(0);
            String processName = "ConsoleSyncReleaseFolders";
            String displayName = "Sync Release Folders";
            HashSet<TaskParameter> taskParameters = new HashSet<>();
            taskParameters.add(new TaskParameter("release entity id", release.getId().toString(), null));
            saveAndRunTask(subject.getKey(), processName, displayName, taskParameters);
        }
        catch (Exception ex) {
            log.error("Error running Sync Release Folders", ex);
        }
    }

    public void runSageArtifactExport(String owner, String releaseName) {
        try {
            DomainDAL dal = DomainDAL.getInstance();
            Subject subject = dal.getSubjectByNameOrKey(owner);
            if (subject==null) throw new IllegalArgumentException("User with name "+owner+" does not exist");
            List<LineRelease> releases = dal.getDomainObjectsByName(subject.getKey(), LineRelease.class, releaseName);
            if (releases.isEmpty()) throw new IllegalArgumentException("Release with name "+releaseName+" does not exist");
            if (releases.size()>1) throw new IllegalArgumentException("More than one release with name "+releaseName);
            LineRelease release = releases.get(0);
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
            for(LineRelease release : DomainDAL.getInstance().getLineReleases(null)) {
                String processName = "SageArtifactExport";
                String displayName = "Sage Artifact Export";
                HashSet<TaskParameter> taskParameters = new HashSet<>();
                taskParameters.add(new TaskParameter("release entity id", release.getId().toString(), null));
                saveAndRunTask(release.getOwnerKey(), processName, displayName, taskParameters);
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