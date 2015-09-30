
package org.janelia.it.jacs.compute.service.export;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.BaseServiceSLSB;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.jboss.annotation.ejb.PoolClass;
import org.jboss.annotation.ejb.TransactionTimeout;
import org.jboss.ejb3.StrictMaxPool;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Jun 25, 2008
 * Time: 1:22:00 PM
 */
@Stateless
@TransactionAttribute(value = TransactionAttributeType.MANDATORY)
@TransactionTimeout(432000)
@PoolClass(value = StrictMaxPool.class, maxSize = 10, timeout = 10000)
public class FileExportSLSB extends BaseServiceSLSB {

    public IService getService(IProcessData processData) {
        return new FileExportService();
    }

}
