package org.janelia.it.jacs.compute.mbean;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 5/24/11
 * Time: 10:38 AM
 */


public interface WorkstationDataManagerMBean {
    	
//    public void runMultiColorFlipOutFileDiscoveryService(String user, boolean refresh);
//
//    public void runMCFOStitchedFileDiscoveryService(String user, boolean refresh);

    public void runMCFODataPipelineService(String user, boolean refresh);

    public void runMCFOSamplePipelineService(String sampleEntityId);
    
    public void setupEntityTypes();

    public void deleteEntityById(String entityId);

}