
package org.janelia.it.jacs.compute.ws;

import org.jboss.annotation.ejb.PoolClass;
import org.jboss.annotation.ejb.TransactionTimeout;

import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Sep 10, 2008
 * Time: 9:14:13 AM
 */
@SOAPBinding(style = SOAPBinding.Style.RPC)
@Stateless(name = "AdminWS")
@WebService(endpointInterface = "org.janelia.it.jacs.compute.ws.AdminWS")
@Remote(AdminWS.class)
@TransactionTimeout(60)
@PoolClass(value = org.jboss.ejb3.StrictMaxPool.class, maxSize = 1, timeout = 10000)
public class AdminWSBean extends BaseWSBean {
    public String echo(@WebParam(name = "echoString") String echoString) {
        return "Web Service Echo + " + echoString;
    }

}
