package org.janelia.it.jacs.compute.launcher.annotation;

import org.janelia.it.jacs.compute.engine.launcher.ejb.SeriesLauncherMDB;
import org.jboss.annotation.ejb.PoolClass;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 9/10/13
 * Time: 1:20 PM
 * To change this template use File | Settings | File Templates.
 */
@MessageDriven(name = "CompartmentAnnotation3DLauncherMDB", activationConfig = {
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge "),
        @ActivationConfigProperty(propertyName = "messagingType", propertyValue = "javax.jms.MessageListener"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/compartmentAnnotation3DLauncher"),
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "3"),
        @ActivationConfigProperty(propertyName = "transactionTimeout", propertyValue = "432000"),
        @ActivationConfigProperty(propertyName = "DLQMaxResent", propertyValue = "0")
})
@PoolClass(value = org.jboss.ejb3.StrictMaxPool.class, maxSize = 3, timeout = 10000)

public class CompartmentAnnotation3DLauncherMDB extends SeriesLauncherMDB {
}
