package org.janelia.it.jacs.compute.launcher.pipelines;

import org.janelia.it.jacs.compute.engine.launcher.ejb.SeriesLauncherMDB;



import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;

// IMPORTANT: keep maxSize of StrictMaxPool in sync with maxSessions in activation config property.

@MessageDriven(name = "TmNeuronConverterMDB", activationConfig = {
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge "),
//        @ActivationConfigProperty(propertyName = "messagingType", propertyValue = "javax.jms.MessageListener"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "java:/jms/queue/tmNeuronUpdate"),
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "35"),
        @ActivationConfigProperty(propertyName = "transactionTimeout", propertyValue = "72000"),
//        @ActivationConfigProperty(propertyName = "DLQMaxResent", propertyValue = "0")
})
//@PoolClass(value  StrictMaxPool.class, maxSize = 35, timeout = 10000)

@SuppressWarnings("unused")
public class TmNeuronConverterMDB extends SeriesLauncherMDB{
}