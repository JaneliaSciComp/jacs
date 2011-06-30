package org.janelia.it.jacs.compute.mbean;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 5/24/11
 * Time: 10:38 AM
 */


public interface WorkstationDataManagerMBean {

    public void runMultiColorFlipOutFileDiscoveryService();

    public void setupEntityTypes();

    public void createOrVerifySystemUser();

    public void deleteEntityById(String entityId);

}