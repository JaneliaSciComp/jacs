package org.janelia.it.jacs.compute.launcher.system;

import org.janelia.it.jacs.compute.engine.launcher.ejb.SeriesLauncherMDB;



import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;

/**
 * This queue only handles messages that create jobs that need to be "forwarded" to the actual computation entry nodes.
 */
@MessageDriven(name = "DispatchLauncherMDB", activationConfig = {
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge "),
        @ActivationConfigProperty(propertyName = "messagingType", propertyValue = "javax.jms.MessageListener"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/dispatchLauncher"),
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "80"),
        @ActivationConfigProperty(propertyName = "transactionTimeout", propertyValue = "432000"),
        @ActivationConfigProperty(propertyName = "DLQMaxResent", propertyValue = "0")
})
//@PoolClass(value  StrictMaxPool.class, maxSize = 80, timeout = 10000)
public class DispatchLauncherMDB extends SeriesLauncherMDB {
}
