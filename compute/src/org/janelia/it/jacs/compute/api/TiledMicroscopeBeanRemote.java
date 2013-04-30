package org.janelia.it.jacs.compute.api;

import javax.ejb.Remote;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 4/30/13
 * Time: 12:52 PM
 * To change this template use File | Settings | File Templates.
 */
@Remote
public interface TiledMicroscopeBeanRemote {

    public void createTiledMicroscopeEntityTypes() throws ComputeException;

}
