package org.janelia.it.jacs.compute.mbean;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 5/24/11
 * Time: 10:38 AM
 */


public interface WorkstationDataManagerMBean {

    public void runResultsDiscovery(String user, String dir, String entityId);
    	
    public void runMultiColorFlipOutFileDiscoveryService(String user, boolean refresh);

    public void runMCFOStitchedFileDiscoveryService(String user, boolean refresh);

    public void runMCFODataPipelineService(String user);
    
    public void setupEntityTypes();

    public void deleteEntityById(String entityId);

}