
package org.janelia.it.jacs.compute.launcher.rnaSeq;

import org.janelia.it.jacs.compute.engine.launcher.ejb.SeriesLauncherMDB;
import org.jboss.annotation.ejb.PoolClass;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 4, 2010
 * Time: 10:27:41 AM
 */
@MessageDriven(name = "GtfFileUploadLauncherMDB", activationConfig = {
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge "),
        @ActivationConfigProperty(propertyName = "messagingType", propertyValue = "javax.jms.MessageListener"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/gtfFileUploadLauncher"),
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "2"),
//    @ActivationConfigProperty(propertyName="MaxMessages", propertyValue="20"),
        @ActivationConfigProperty(propertyName = "transactionTimeout", propertyValue = "432000"),
        // DLQMaxResent is a JBoss-specific management property. 0 = no resent messages
        @ActivationConfigProperty(propertyName = "DLQMaxResent", propertyValue = "0")
})
@PoolClass(value = org.jboss.ejb3.StrictMaxPool.class, maxSize = 2, timeout = 10000)
public class GtfFileUploadLauncherMDB extends SeriesLauncherMDB {

}

