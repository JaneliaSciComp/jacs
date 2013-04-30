package org.janelia.it.jacs.compute.mbean;

import org.janelia.it.jacs.compute.api.EJBFactory;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 4/30/13
 * Time: 12:11 PM
 * To change this template use File | Settings | File Templates.
 */
public class TiledMicroscopeManager implements TiledMicroscopeManagerMBean {

    public void createTiledMicroscopeEntityTypes() {
        try {
            EJBFactory.getRemoteTiledMicroscopeBean().createTiledMicroscopeEntityTypes();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
