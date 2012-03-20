package org.janelia.it.jacs.compute.mbean;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 5/24/11
 * Time: 10:38 AM
 */


public interface WorkstationDataManagerMBean {
    	
	public void runSolrIndexSync(Boolean clearIndex); 
	
    public void runSampleSyncService(String user, Boolean testRun);
    
    public void runSampleImageSync(String user);

    public void runMCFODataCompress(String user, Boolean testRun);
    
    public void runMCFODataUpgrade(String user, Boolean testRun);
    
    public void runMCFODataPipeline(String user, String inputDirList, String topLevelFolderName, Boolean refreshProcessing, Boolean refreshAlignment, Boolean refreshSeparation);

    public void runMCFOSamplePipeline(String sampleEntityId, Boolean refreshProcessing, Boolean refreshAlignment, Boolean refreshSeparation);
    
	public void runMCFOSeparationPipeline(String sampleEntityId, String inputFilename, String resultEntityName);
	
    public void runMCFOSampleViewCreation(String sourceEntityId, String targetEntityName);

    public void runFlyScreenPipeline(String user, Boolean refresh);

    public void runFlyScreenPatternAnnotationPipeline(String user, Boolean refresh);

    public void deleteEntityById(String entityId);

    public void doEntityTreePerformanceTest();

    public void performScreenPipelineSurgery(String user);

    public void runFileTreeLoaderPipeline(String user, String rootDirectoryPath, String topLevelFolderName);
    
    public void runTicPipeline(String user, String rootDirectoryPath);

    public void createPatternAnnotationQuantifierSummaryFile();

}