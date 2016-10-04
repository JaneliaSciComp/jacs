package org.janelia.it.jacs.compute.launcher.pipelines;

import org.janelia.it.jacs.compute.engine.launcher.ejb.SeriesLauncherMDB;



import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;

// IMPORTANT: keep maxSize of StrictMaxPool in sync with maxSessions in activation config property.

@MessageDriven(activationConfig = {
        //@ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge "),
//        @ActivationConfigProperty(propertyName = "messagingType", propertyValue = "javax.jms.MessageListener"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/lvsDiscovery"),
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "1")//,
        //@ActivationConfigProperty(propertyName = "transactionTimeout", propertyValue = "72000")//,
//        @ActivationConfigProperty(propertyName = "DLQMaxResent", propertyValue = "0")
})
//@PoolClass(value  StrictMaxPool.class, maxSize = 1, timeout = 10000)

@SuppressWarnings("unused")
public class LargeVolumeSampleDiscoveryLauncherMDB extends SeriesLauncherMDB{
}