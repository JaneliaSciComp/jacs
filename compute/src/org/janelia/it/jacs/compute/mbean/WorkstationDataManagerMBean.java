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

	// System Administration Pipelines
	public void runOrphanCheckerServices(Boolean deleteOrphans);
	public void runOrphanEntityCheckerService(String user, Boolean deleteOrphanTrees);
	public void runOrphanAnnotationCheckerService(String user, Boolean deleteAnnotationsMissingTargets, Boolean deleteAnnotationsMissingTerms);
	public void runUpgradeData();
    public void runUpgradeUserData(String user);
    public void runUpgradeSingleSample(String sampleEntityId);
    public void annexEntityTree(String subjectKey, String entityId);
    
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

    public void runCompartmentLoading(String user, String alignmentSpaceName, String maskChanPath, String topLevelFolderName, String opticalResolution, String pixelResolution);

    public void runSlowImportTask(String parentDirPath, String topLevelFolderName, String owner);
    public void runDataDeDuplication();

    // Tile viewer pipelines
    public void create3DTileMicroscopeSamples(String user, String destinationFolderName);
    
    public void addChildFolder(String parentId, String folderName);
    
    public void runBenchmarks();
    public void runScalityBenchmarks(String entityId);
    
    public void runNernaRetiredDataCleanup();
    
}