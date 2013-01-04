package org.janelia.it.jacs.compute.mbean;


/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 5/24/11
 * Time: 10:38 AM
 */
public interface WorkstationDataManagerMBean {
    	
	// Pipelines for Arnim
	public void runAnnotationImport(String user, String annotationsPath, String ontologyName);
	public void runScreenScoresLoading(String user, String acceptsPath, String loadedPath);
	public void runScreenScoresExport(String user, String outputFilepath);
	public void runSplitLinesLoading(String user, String topLevelFolderName, String representativesPath, String splitConstructsPath);
	
	
	// Alternative databases (not currently used)
	public void runNeo4jSync(Boolean clearDb);
	public void runMongoDbSync(Boolean clearDb);
	
	
	// SOLR Indexing
	public void runSolrIndexSync(Boolean clearIndex); 	
	public void runSolrTreeIndexing(Long rootId);

	
	// Maintenance Pipelines
	public void runOrphanAnnotationCheckerService(String user, Boolean deleteAnnotationsMissingTargets, Boolean deleteAnnotationsMissingTerms);
    
    public void runUpgradeUserData(String user);
    public void runUpgradeSingleSample(String sampleEntityId);
    
    public void runUserSampleImageRegistration(String user);
    public void runSampleCleaningService(String user, Boolean testRun);
    public void runSampleSyncService(String user, Boolean testRun); 
    public void runMCFODataCompress(String user, Boolean testRun);
    public void runSampleMaintenancePipeline(String user);
    
    
    // Pipelines for FlyLight Single Neuron Data
    public void runSingleFastLoadArtifactPipeline(String separationEntityId);
    public void runCompleteFastLoadArtifactPipeline(String user);
    
    
    // Generic confocal image processing pipelines, driven by pipeline configurations on a data-set basis
    public void runAllDataSetPipelines(String runMode, Boolean reuseProcessing);
    public void runUserDataSetPipelines(String username, String runMode, Boolean reuseProcessing);
    public void runSampleFolder(String folderId, Boolean reuseProcessing);
    public void runSamplePipelines(String sampleId, Boolean reuseProcessing);
    public void runConfiguredSamplePipeline(String sampleEntityId, String configurationName, Boolean reuseProcessing);
    public void runNeuronSeparationPipeline(String resultEntityId);
    
    
    // Pipelines for FlyLight Screen Data
    public void runFlyScreenPipeline(String user, Boolean refresh);
    public void runFlyScreenPatternAnnotationPipeline(String user, Boolean refresh);
    public void performScreenPipelineSurgery(String user);
    public void createPatternAnnotationQuantifierSummaryFile();
    public void createMaskSummaryFile(String maskFolderName);
    public void runMaskAnnotationPipeline(String user, String maskFolderName, Boolean refresh);
    
    
    // Other data pipelines
    public void runFileTreeLoaderPipeline(String user, String rootDirectoryPath, String topLevelFolderName);
    
    public void runTicPipeline(String user, String dirOfInputFile, String inputFileName, String transformationMatrixFile,
                               String borderValue, String correctionFile, String microscopeSettingsFile);

    public void runNeuronMergeTest(String taskOwner, String separationEntityId, String commaSeparatedNeuronFragmentList);

    public void runSlowImportTask(String parentDirPath, String topLevelFolderName, String owner);
    public void runDataDeDuplication();

    
    // Security management (should be made into a separate bean at some point)
    public void createGroup(String groupOwner, String groupName);
    public void removeGroup(String groupName);
    public void addUserToGroup(String groupUser, String groupName);
    public void removeUserFromGroup(String groupUser, String groupName);
    public void annexEntityTree(String subjectKey, String entityId);
    
}