package org.janelia.it.jacs.compute.launcher.system;

import com.google.common.collect.ImmutableMap;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.jboss.annotation.ejb.PoolClass;
import org.jboss.ejb3.StrictMaxPool;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.Message;
import javax.jms.MessageListener;
import java.rmi.RemoteException;

/**
 * This typically runs on the same machine on which the actual computation is initiated and it kicks off
 * the actual dispatcher.
 */
@MessageDriven(name = "DispatchListenerMDB", activationConfig = {
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge "),
        @ActivationConfigProperty(propertyName = "messagingType", propertyValue = "javax.jms.MessageListener"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/dispatchRequest"),
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "300"),
        @ActivationConfigProperty(propertyName = "transactionTimeout", propertyValue = "432000"),
        // DLQMaxResent is a JBoss-specific management property. 0 = no resent messages
        @ActivationConfigProperty(propertyName = "DLQMaxResent", propertyValue = "0")
})
@PoolClass(value = StrictMaxPool.class, maxSize = 300, timeout = 10000)
public class DispatchListenerMDB implements MessageListener {

    @Override
    public void onMessage(Message message) {
        ComputeBeanLocal computeBean = EJBFactory.getLocalComputeBean();
        try {
            computeBean.submitJob("StartComputation", ImmutableMap.<String, Object>of(IProcessData.PROCESS_ID, -1L));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
