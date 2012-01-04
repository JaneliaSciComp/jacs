package org.janelia.it.jacs.compute.mbean;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 5/24/11
 * Time: 10:38 AM
 */


public interface WorkstationDataManagerMBean {
    	
    public void runSampleSyncService(String user, Boolean testRun);

    public void runMCFODataPipeline(String user, String inputDirList, String topLevelFolderName, Boolean refreshProcessing, Boolean refreshAlignment, Boolean refreshSeparation);

    public void runMCFOSamplePipeline(String sampleEntityId, Boolean refreshProcessing, Boolean refreshAlignment, Boolean refreshSeparation);
    
	public void runMCFOSeparationPipeline(String sampleEntityId, String inputFilename, String resultEntityName);
	
    public void runMCFOSampleViewCreation(String sourceEntityId, String targetEntityName);

    public void runMergedTileDataPipeline(String user, Boolean refresh, String inputDirList, String topLevelFolderName);

    public void runFlyScreenPipeline(String user, Boolean refresh);

    public void runFlyScreenPatternAnnotationPipeline(String user, Boolean refresh);
    	
    public void setupEntityTypes();

    public void deleteEntityById(String entityId);

    public void doEntityTreePerformanceTest();

}