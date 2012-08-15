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
	public void runScreenScoresLoading(String user);
	public void runScreenScoresLoading2(String user);
	public void runScreenScoresLoading3(String user, String rejectsPath, String acceptsPath);
	public void runScreenScoresLoading4(String user, String addsPath);
	public void runScreenScoresExport(String user, String outputFilepath);
	public void runSplitLinesLoading(String user, String topLevelFolderName, String representativesPath, String splitConstructsPath);
	
	public void runNeo4jSync(Boolean clearDb);
	
	public void runMongoDbSync(Boolean clearDb);
	
	public void runSolrIndexSync(Boolean clearIndex); 
	
	public void runSolrTreeIndexing(Long rootId);
	
	public void runOrphanAnnotationCheckerService(String user, Boolean deleteAnnotationsMissingTargets, Boolean deleteAnnotationsMissingTerms);
	
	public void runSampleCleaningService(String user, Boolean testRun);
	
    public void runSampleSyncService(String user, Boolean testRun);
    
    public void runSampleImageSync(String user);

    public void runMCFODataCompress(String user, Boolean testRun);
    
    public void runMCFODataUpgrade(String user, Boolean testRun);
    
    public void runSingleFastLoadArtifactPipeline(String user, String separationEntityId);
    
    public void runCompleteFastLoadArtifactPipeline(String user);
    
    public void runLeetSageBasedDataPipeline(String user, String topLevelFolderName, String imageFamily, Boolean refreshProcessing, Boolean refreshAlignment, Boolean refreshSeparation);
    
    public void runCentralBrainDataPipeline(String user, String topLevelFolderName, Boolean refreshProcessing, Boolean refreshAlignment, Boolean refreshSeparation);
    
    public void runAlignWholeBrainDataPipeline(String user, Boolean refreshAlignment, Boolean refreshSeparation);
    
    public void runAlignWholeBrainSamplePipeline(String user, Boolean refreshAlignment, Boolean refreshSeparation);
    
    public void runTwoChanDataPipeline(String user);
    
    public void runTwoChanSamplePipeline(String sampleEntityId);

    public void runMCFODataPipeline(String user, String topLevelFolderName, Boolean refreshProcessing, Boolean refreshAlignment, Boolean refreshSeparation);

    public void runMCFOSamplePipeline(String sampleEntityId, Boolean refreshProcessing, Boolean refreshAlignment, Boolean refreshSeparation);
    
	public void runMCFOSeparationPipeline(String sampleEntityId, String inputFilename, String resultEntityName);
	
    public void runMCFOSampleViewCreation(String sourceEntityId, String targetEntityName);

    public void runFlyScreenPipeline(String user, Boolean refresh);

    public void runFlyScreenPatternAnnotationPipeline(String user, Boolean refresh);

    public void performScreenPipelineSurgery(String user);

    public void runFileTreeLoaderPipeline(String user, String rootDirectoryPath, String topLevelFolderName);
    
    public void runTicPipeline(String user, String dirOfInputFile, String inputFileName, String transformationMatrixFile,
                               String borderValue, String correctionFile, String microscopeSettingsFile);

    public void createPatternAnnotationQuantifierSummaryFile();

    public void createMaskSummaryFile(String maskFolderName);

    public void runMaskAnnotationPipeline(String user, String maskFolderName, Boolean refresh);

    public void runNeuronMergeTest(String taskOwner, String separationEntityId, String commaSeparatedNeuronFragmentList);

    public void runSlowImportTask(String parentDirPath, String topLevelFolderName, String owner);

    }