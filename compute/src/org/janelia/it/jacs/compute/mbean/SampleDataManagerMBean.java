package org.janelia.it.jacs.compute.mbean;

/**
 * MBean for managing Samples and their processing results. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface SampleDataManagerMBean {

    // Maintenance Pipelines    
    public void runAllSampleMaintenancePipelines(); // All maintenance pipelines for all users
    public void runUserSampleMaintenancePipelines(String user); // All maintenance pipelines for a single users
    public void runSampleCleaning(String user, Boolean testRun);
    public void runSampleTrashCompactor(String user, Boolean testRun);
    public void runAllSampleDataCompression(String compressionType);
    public void runSampleDataCompression(String user, String dataSetName, String compressionType);
    public void runSingleSampleDataCompression(String sampleId, String compressionType);
    public void runSampleImageRegistration(String user);
    public void runSampleRetirement(String user);
    public void runSampleRetirement();
    
    // File management
    public void runSingleSampleArchival(String sampleEntityId);  
    public void runCompleteSampleArchival(String user);
    public void runSyncSampleToScality(String sampleEntityId, String filetypes);  
    public void runSyncDataSetToScality(String user, String dataSet, String filetypes);
    
    // Generic confocal image processing pipelines, driven by pipeline configurations on a data-set basis
    public void cancelAllIncompleteDataSetPipelineTasks();
    public void cancelAllIncompleteUserTasks(String user);
    public String runAllDataSetPipelines(String runMode, Boolean reuseSummary, Boolean reuseProcessing, Boolean reusePost, Boolean reuseAlignment, Boolean force);
    public String runUserDataSetPipelines(String user, String dataSetName, Boolean runSampleDiscovery, String runMode, Boolean reusePipelineRuns, Boolean reuseSummary, Boolean reuseProcessing, Boolean reusePost, Boolean reuseAlignment, Boolean force);
    public void runSampleFolder(String folderId, Boolean reusePipelineRuns, Boolean reuseSummary, Boolean reuseProcessing, Boolean reusePost, Boolean reuseAlignment, String extraParams);
    public void runSamplePipelines(String sampleId, Boolean reusePipelineRuns, Boolean reuseSummary, Boolean reuseProcessing, Boolean reusePost, Boolean reuseAlignment, String extraParams);
    public void runConfiguredSamplePipeline(String sampleEntityId, String configurationName, Boolean reuseSummary, Boolean reuseProcessing, Boolean reusePost, Boolean reuseAlignment);
    public void runNeuronSeparationPipeline(String resultEntityId);
    public void runNeuronSeparationMapping(String separationId1, String separationId2);
    
    // Generic sample processing
    public void applyProcessToDataset(String user, String dataSetName, String parentOrChildren, String processName, String extraParams);
    public void applyProcessToSample(String sampleEntityId, String processName, String extraParams);
    public void applyProcessToSamplesInFolder(String folderId, String processName, String extraParams);
    
    // Upgrade pipelines
    public void runRepairSeparationsPipeline(String user);
    public void runRepairSeparationResultsPipeline(String user);
    public void runScalityCorrectionService(String user);

    public void scalityMigrationService(String filePath);
    public void bzipLSMCompressionService(String filePath, String owner, String compressMode);
    public void visuallyLosslessCorrectionService(String filePath, String debug);
    
    // SAGE database
    public void runSageLoader(String owner, String item, String configPath, String grammarPath, String lab, String debug, String lock);
    public void runSageArtifactExport(String owner, String releaseName);
    public void runSageQiScoreSync(Boolean testRun);
}