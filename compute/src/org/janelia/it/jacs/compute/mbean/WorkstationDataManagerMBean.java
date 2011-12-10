package org.janelia.it.jacs.compute.mbean;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 5/24/11
 * Time: 10:38 AM
 */


public interface WorkstationDataManagerMBean {
    	
	public void runSampleSyncService(String user);
		
	public void runMCFODataPipeline(String user, boolean refresh, String inputDirList, String topLevelFolderName);

    public void runMergedTileDataPipeline(String user, boolean refresh, String inputDirList, String topLevelFolderName);
    
	public void runMCFODataPipelineForTiles(String user, boolean refresh);

    public void runMCFOSamplePipeline(String sampleEntityId);

    public void runMCFOSampleViewCreation(String sourceEntityId, String targetEntityName);

    public void runFlyScreenPipeline(String user, boolean refresh);
    	
    public void setupEntityTypes();

    public void deleteEntityById(String entityId);

}