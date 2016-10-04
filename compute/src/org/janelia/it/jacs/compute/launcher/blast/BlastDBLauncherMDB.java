
package org.janelia.it.jacs.compute.launcher.blast;

import org.janelia.it.jacs.compute.engine.launcher.ejb.SeriesLauncherMDB;



import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;

/**
 * You don't have to give up and rollback the exception as the base SequenceLauncherMDB class does.
 * You can override the handleException method of the base SeriesLauncherMDB if you want to go for retries
 * and really know what you're doing.  Beware of retries.  The transaction might have sunk into a corrupted
 * state. Put different error codes into your Service exception when you throw them within your services and
 * analyze the code in the catch blocks below if you want to go for retries.  ServiceException
 * has an error code data member.
 *
 * @author Tareq Nabeel
 */
@MessageDriven(activationConfig = {
        //@ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge "),
//        @ActivationConfigProperty(propertyName = "messagingType", propertyValue = "javax.jms.MessageListener"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/blastDBLauncher"),
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "5")//,
//    @ActivationConfigProperty(propertyName="MaxMessages", propertyValue="5"),
        //@ActivationConfigProperty(propertyName = "transactionTimeout", propertyValue = "18000")//,
        // DLQMaxResent is a JBoss-specific management property. 0 = no resent messages
//        @ActivationConfigProperty(propertyName = "DLQMaxResent", propertyValue = "0")
})
//@PoolClass(value  StrictMaxPool.class, maxSize = 5, timeout = 10000)
public class BlastDBLauncherMDB extends SeriesLauncherMDB {

}