
package org.janelia.it.jacs.compute.service.blast.persist.query;

import javax.ejb.EJBObject;
import java.rmi.RemoteException;

/**
 * @author Tareq Nabeel
 */
public interface PersistQueryNodeRemote extends EJBObject {
    public Long saveFastaFileNode(String userLogin, String fastaFilePath) throws PersistQNodeException, RemoteException;

    public Long saveFastaText(String userLogin, String fastaText) throws PersistQNodeException, RemoteException;
}
