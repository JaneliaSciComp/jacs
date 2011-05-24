package org.janelia.it.jacs.compute.launcher.fileDiscovery;

import org.janelia.it.jacs.compute.engine.launcher.ejb.SeriesLauncherMDB;
import org.jboss.annotation.ejb.PoolClass;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 5/24/11
 * Time: 12:55 PM
 * To change this template use File | Settings | File Templates.
 */

@MessageDriven(name = "MultiColorFlipOutFileDiscoveryLauncherMDB", activationConfig = {
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge "),
        @ActivationConfigProperty(propertyName = "messagingType", propertyValue = "javax.jms.MessageListener"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/multiColorFlipOutFileDiscoveryLauncher"),
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "2"),
        @ActivationConfigProperty(propertyName = "transactionTimeout", propertyValue = "432000"),
        @ActivationConfigProperty(propertyName = "DLQMaxResent", propertyValue = "0")
})
@PoolClass(value = org.jboss.ejb3.StrictMaxPool.class, maxSize = 2, timeout = 10000)

public class MultiColorFlipOutFileDiscoveryLauncherMDB extends SeriesLauncherMDB{
}