
package org.janelia.it.jacs.compute.service.reversePsiBlast;

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
 * User: aresnick
 * Date: Jul 9, 2009
 * Time: 2:32:32 PM
 * <p/>
 * <p/>
 * Description:
 */
@Stateless
@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
@TransactionTimeout(3600)
@PoolClass(value = StrictMaxPool.class, maxSize = 20, timeout = 10000)
public class ReversePsiBlastCreateResultNodeSLSB extends BaseServiceSLSB {
    public IService getService(IProcessData processData) {
        return new ReversePsiBlastCreateResultNodeService();
    }
}
