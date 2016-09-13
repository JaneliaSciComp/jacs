
package org.janelia.it.jacs.compute.service.blast.createtask;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.BaseServiceSLSB;
import org.janelia.it.jacs.compute.engine.service.IService;

import org.jboss.ejb3.annotation.TransactionTimeout;


import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

/**
 * @author Tareq Nabeel
 */
@Stateless(name = "CreateBlastTaskSLSBEJB")
@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
@TransactionTimeout(3600)
//@PoolClass(value  StrictMaxPool.class, maxSize = 10, timeout = 10000)
public class CreateBlastTaskSLSB extends BaseServiceSLSB {

    public IService getService(IProcessData processData) {
        return new CreateTaskService();
    }

}
