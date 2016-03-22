package org.janelia.it.jacs.compute.mbean;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 4/30/13
 * Time: 12:10 PM
 * To change this template use File | Settings | File Templates.
 */
public interface TiledMicroscopeManagerMBean {

    public void createTiledMicroscopeEntityTypes();
    
    public void convertAllTmWorkspaceToProtobuf();

    public void test();

}
