package org.janelia.it.jacs.compute.api;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.TiledMicroscopeDAO;
import org.jboss.annotation.ejb.PoolClass;
import org.jboss.annotation.ejb.TransactionTimeout;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 4/30/13
 * Time: 12:54 PM
 * To change this template use File | Settings | File Templates.
 */


@Stateless(name = "TiledMicroscopeEJB")
@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
@TransactionTimeout(432000)
@PoolClass(value = org.jboss.ejb3.StrictMaxPool.class, maxSize = 100, timeout = 10000)
public class TiledMicroscopeBeanImpl implements TiledMicroscopeBeanLocal, TiledMicroscopeBeanRemote {

    private static final Logger _logger = Logger.getLogger(AnnotationBeanImpl.class);

    private final TiledMicroscopeDAO _tiledMicroscopeDAO = new TiledMicroscopeDAO(_logger);

    public void createTiledMicroscopeEntityTypes() throws ComputeException {
        try {
            _tiledMicroscopeDAO.createTiledMicroscopeEntityTypes();
        }
        catch (Exception e) {
            String errorString="Error calling createdTiledMicroscopeEntityTypes at DAO layer";
            _logger.error(errorString);
            throw new ComputeException(errorString);
        }
    }

}
