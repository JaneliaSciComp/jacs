
package org.janelia.it.jacs.compute.service.blast.persist.query;

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
@Stateless
@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
@TransactionTimeout(3600)
//@PoolClass(value  StrictMaxPool.class, maxSize = 60, timeout = 10000)
public class PersistBlastQueryNodeSLSB extends BaseServiceSLSB {
    public IService getService(IProcessData processData) {
        return new PersistQueryNodeService();
    }

    public Long saveFastaFileNode(String userLogin, String fastaFilePath) throws PersistQNodeException {
        PersistQueryNodeService processor = new PersistQueryNodeService();
        return processor.saveFastaFileNode(userLogin, fastaFilePath);
    }

    public Long saveFastaText(String userLogin, String fastaText) throws PersistQNodeException {
        PersistQueryNodeService processor = new PersistQueryNodeService();
        return processor.saveFastaText(userLogin, fastaText);
    }

}
