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
	public void runScreenScoresLoading2(String user, String acceptsPath);
	public void runScreenScoresLoading3(String user, String rejectsPath, String acceptsPath);
	public void runScreenScoresLoading4(String user, String addsPath);
	public void runScreenScoresLoading5(String user, String compartments);
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
    public void runSampleImageSync(String user);
    public void runMCFODataUpgrade(String user, Boolean testRun);
    public void runSampleCleaningService(String user, Boolean testRun);
    public void runSampleSyncService(String user, Boolean testRun); 
    public void runMCFODataCompress(String user, Boolean testRun);
    public void runSampleMaintenancePipeline(String user);
    
    
    // Pipeline for the T. Lee lab
    public void runLeetDataPipeline(String user, String topLevelFolderName, String imageFamily, Boolean refreshProcessing, Boolean refreshAlignment);
    public void runLeetSamplePipeline(String sampleEntityId, Boolean refreshProcessing, Boolean refreshAlignment);

    
    // Pipelines for FlyLight Single Neuron Data
    
    public void runCentralBrainDataPipeline(String user, String topLevelFolderName, Boolean refreshProcessing, Boolean refreshAlignment, Boolean refreshSeparation);
    
    public void runAlignWholeBrainDataPipeline(String user, Boolean refreshAlignment, Boolean refreshSeparation);
    public void runAlignWholeBrainSamplePipeline(String user, Boolean refreshAlignment, Boolean refreshSeparation);
    
    public void runTwoChanDataPipeline(String user);
    public void runTwoChanSamplePipeline(String sampleEntityId);
	
    public void runMCFOSampleViewCreation(String sourceEntityId, String targetEntityName);

    public void runSingleFastLoadArtifactPipeline(String user, String separationEntityId);
    public void runCompleteFastLoadArtifactPipeline(String user);
    
    // Generic confocal image processing pipelines, driven by pipeline configurations on a data-set basis
    
    public void runAllDataSetPipelines(String runMode);
    public void runUserDataSetPipelines(String username, String runMode);
    public void runSamplePipeline(String sampleEntityId);
    public void runConfiguredSamplePipeline(String sampleEntityId, String configurationName);
    
    // Pipelines for FlyLight Screen Data
    
    public void runFlyScreenPipeline(String user, Boolean refresh);
    public void runFlyScreenPatternAnnotationPipeline(String user, Boolean refresh);
    public void performScreenPipelineSurgery(String user);
    public void createPatternAnnotationQuantifierSummaryFile();
    public void createMaskSummaryFile(String maskFolderName);
    public void runMaskAnnotationPipeline(String user, String maskFolderName, Boolean refresh);
    public void runArnimPatternAnnotationFinisher(String user);

    
    // Other data pipelines
    
    public void runFileTreeLoaderPipeline(String user, String rootDirectoryPath, String topLevelFolderName);
    
    public void runTicPipeline(String user, String dirOfInputFile, String inputFileName, String transformationMatrixFile,
                               String borderValue, String correctionFile, String microscopeSettingsFile);

    public void runNeuronMergeTest(String taskOwner, String separationEntityId, String commaSeparatedNeuronFragmentList);

    public void runSlowImportTask(String parentDirPath, String topLevelFolderName, String owner);

}