package org.janelia.it.jacs.compute.launcher.fileDiscovery;

import org.janelia.it.jacs.compute.engine.launcher.ejb.SeriesLauncherMDB;



import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;


@MessageDriven(name = "ImportFilePipelineLauncherMDB", activationConfig = {
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge "),
//        @ActivationConfigProperty(propertyName = "messagingType", propertyValue = "javax.jms.MessageListener"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "java:/jms/queue/fileImportFilePipelineLauncher"),
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "3"),
        @ActivationConfigProperty(propertyName = "transactionTimeout", propertyValue = "432000"),
//        @ActivationConfigProperty(propertyName = "DLQMaxResent", propertyValue = "0")
})
//@PoolClass(value  StrictMaxPool.class, maxSize = 3, timeout = 10000)

public class ImportFilePipelineLauncherMDB extends SeriesLauncherMDB {
}

