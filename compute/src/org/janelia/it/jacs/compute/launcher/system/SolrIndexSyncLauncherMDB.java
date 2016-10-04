package org.janelia.it.jacs.compute.launcher.system;

import org.janelia.it.jacs.compute.engine.launcher.ejb.SeriesLauncherMDB;



import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;

@MessageDriven(activationConfig = {
        //@ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge "),
//        @ActivationConfigProperty(propertyName = "messagingType", propertyValue = "javax.jms.MessageListener"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/solrIndexSyncLauncher"),
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "1")//,
        //@ActivationConfigProperty(propertyName = "transactionTimeout", propertyValue = "432000")//,
//        @ActivationConfigProperty(propertyName = "DLQMaxResent", propertyValue = "0")
})
//@PoolClass(value  StrictMaxPool.class, maxSize = 1, timeout = 10000)

public class SolrIndexSyncLauncherMDB extends SeriesLauncherMDB{
}