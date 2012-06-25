package org.janelia.it.jacs.compute.mbean;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 5/24/11
 * Time: 10:38 AM
 */
public interface WorkstationDataManagerMBean {
    	
	public void runSplitLinesLoading(String user, String topLevelFolderName, String representativesPath, String splitConstructsPath);
	
	public void runAnnotationImport(String user, String annotationsPath, String ontologyName);
	
	public void runNeo4jSync(Boolean clearDb);
	
	public void runMongoDbSync(Boolean clearDb);
	
	public void runSolrIndexSync(Boolean clearIndex); 
	
	public void runSolrTreeIndexing(Long rootId);
	
    public void runSampleSyncService(String user, Boolean testRun);
    
    public void runSampleImageSync(String user);

    public void runMCFODataCompress(String user, Boolean testRun);
    
    public void runMCFODataUpgrade(String user, Boolean testRun);
    
    public void runLeetFileBasedDataPipeline(String user, String topLevelFolderName, String rootDirectoryPath, Boolean refreshProcessing, Boolean refreshAlignment, Boolean refreshSeparation);
    
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

    public void runLeetCentralBrainDataPipeline(String user, String topLevelFolderName, Boolean refreshProcessing,
                                                Boolean refreshAlignment, Boolean refreshSeparation);

    }