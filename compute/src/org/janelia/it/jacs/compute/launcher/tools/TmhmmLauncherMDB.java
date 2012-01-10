
package org.janelia.it.jacs.compute.launcher.tools;

import org.janelia.it.jacs.compute.engine.launcher.ejb.SeriesLauncherMDB;
import org.jboss.annotation.ejb.PoolClass;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;

/**
 * Created by IntelliJ IDEA.
 * User: jinman
 * Date: April 18, 2010
 * Time: 12:15:00 PM
 */
@MessageDriven(name = "TmhmmLauncherMDB", activationConfig = {
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge "),
        @ActivationConfigProperty(propertyName = "messagingType", propertyValue = "javax.jms.MessageListener"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/TmhmmLauncher"),
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "5"),
        @ActivationConfigProperty(propertyName = "transactionTimeout", propertyValue = "432000"),
        @ActivationConfigProperty(propertyName = "DLQMaxResent", propertyValue = "0")
})
@PoolClass(value = org.jboss.ejb3.StrictMaxPool.class, maxSize = 5, timeout = 10000)
public class TmhmmLauncherMDB extends SeriesLauncherMDB {

}