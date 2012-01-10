
package org.janelia.it.jacs.compute.ws;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Sep 9, 2008
 * Time: 4:53:26 PM
 */
@SOAPBinding(style = SOAPBinding.Style.RPC)
@WebService()
public interface AdminWS extends Remote {
    String echo(@WebParam(name = "echoString") String echoString) throws RemoteException;
}


