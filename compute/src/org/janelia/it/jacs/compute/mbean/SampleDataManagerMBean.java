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
    public void runSampleDataCompression(String user, Boolean testRun);
    public void runSingleSampleDataCompression(String sampleId);
    public void runSampleImageRegistration(String user);
    public void runSingleSampleArchival(String sampleEntityId);  
    public void runCompleteSampleArchival(String user); 
    
    // Generic confocal image processing pipelines, driven by pipeline configurations on a data-set basis
    public void cancelAllIncompleteDataSetPipelineTasks();
    public String runAllDataSetPipelines(String runMode, Boolean reuseProcessing, Boolean reuseAlignment, Boolean force);
    public String runUserDataSetPipelines(String user, String dataSetName, Boolean runSampleDiscovery, String runMode, Boolean reusePipelineRuns, Boolean reuseProcessing, Boolean reuseAlignment, Boolean force);
    public void runSampleFolder(String folderId, Boolean reusePipelineRuns, Boolean reuseProcessing, Boolean reuseAlignment);
    public void runSamplePipelines(String sampleId, Boolean reusePipelineRuns, Boolean reuseProcessing, Boolean reuseAlignment);
    public void runConfiguredSamplePipeline(String sampleEntityId, String configurationName, Boolean reuseProcessing, Boolean reuseAlignment);
    public void runNeuronSeparationPipeline(String resultEntityId);
    public void runNeuronSeparationMapping(String separationId1, String separationId2);
    
    // Generic sample processing
    public void applyProcessToDataset(String user, String dataSetName, String parentOrChildren, String processName);
    public void applyProcessToSample(String sampleEntityId, String processName);
    public void applyProcessToSamplesInFolder(String folderId, String processName);
    
    // Upgrade pipelines
    public void runRepairSeparationsPipeline(String user);
    public void runRepairSeparationResultsPipeline(String user);

    public void bzipLSMCompressionService(String filePath, String owner, String compressMode);

    // SAGE database
    public void runSageLoader(String owner, String item, String configPath, String grammarPath, String lab, String debug, String lock);
    public void runSageArtifactExport(String user);
    public void runSageQiScoreSync();
}