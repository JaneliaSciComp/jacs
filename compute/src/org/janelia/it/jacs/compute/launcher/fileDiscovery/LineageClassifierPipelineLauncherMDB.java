package org.janelia.it.jacs.compute.launcher.fileDiscovery;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;

import org.janelia.it.jacs.compute.engine.launcher.ejb.SeriesLauncherMDB;



/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 11/21/11
 * Time: 10:47 PM
 * To change this template use File | Settings | File Templates.
 */

@MessageDriven(name = "LineageClassifierPipelineLauncherMDB", activationConfig = {
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge "),
//        @ActivationConfigProperty(propertyName = "messagingType", propertyValue = "javax.jms.MessageListener"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "java:/jms/queue/lineageClassifierPipelineLauncher"),
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "3"),
        @ActivationConfigProperty(propertyName = "transactionTimeout", propertyValue = "432000"),
//        @ActivationConfigProperty(propertyName = "DLQMaxResent", propertyValue = "0")
})
//@PoolClass(value  StrictMaxPool.class, maxSize = 3, timeout = 10000)

public class LineageClassifierPipelineLauncherMDB extends SeriesLauncherMDB {
}

