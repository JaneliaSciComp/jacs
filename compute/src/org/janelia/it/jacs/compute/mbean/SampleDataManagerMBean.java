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
    public void runSampleImageRegistration(String user);
    
    // Generic confocal image processing pipelines, driven by pipeline configurations on a data-set basis
    public void runAllDataSetPipelines(String runMode, Boolean reuseProcessing);
    public void runUserDataSetPipelines(String username, String runMode, Boolean reuseProcessing);
    public void runSampleFolder(String folderId, Boolean reuseProcessing);
    public void runSamplePipelines(String sampleId, Boolean reuseProcessing);
    public void runConfiguredSamplePipeline(String sampleEntityId, String configurationName, Boolean reuseProcessing);
    public void runNeuronSeparationPipeline(String resultEntityId);
    public void runSingleFastLoadArtifactPipeline(String separationEntityId);
    public void runSingleMaskChanArtifactPipeline(String separationEntityId);
    public void runCompleteFastLoadArtifactPipeline(String user);
    public void runCompleteMaskChanArtifactPipeline(String user);
    
}