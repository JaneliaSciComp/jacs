package org.janelia.it.jacs.compute.mbean;

import javax.management.MXBean;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 4/30/13
 * Time: 12:10 PM
 * To change this template use File | Settings | File Templates.
 */
@MXBean
public interface TiledMicroscopeManagerMBean {

    public void createTiledMicroscopeEntityTypes();
    
    public void convertAllTmWorkspaceToProtobuf();

    public void test();

}
