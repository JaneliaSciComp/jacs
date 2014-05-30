
package org.janelia.it.jacs.compute.service.genomeProject;

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
 * User: tsafford
 * Date: Sep 12, 2007
 * Time: 9:51:16 AM
 */
@Stateless
@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
@TransactionTimeout(3600)
@PoolClass(value = StrictMaxPool.class, maxSize = 40, timeout = 10000)
public class CreateGenomeProjectFileNodeSLSB extends BaseServiceSLSB {
    public IService getService(IProcessData
            processData) {
        return new CreateGenomeProjectFileNodeService();
    }
}