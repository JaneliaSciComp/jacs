
package org.janelia.it.jacs.compute.launcher.rnaSeq;

import org.janelia.it.jacs.compute.engine.launcher.ejb.SeriesLauncherMDB;
import org.jboss.annotation.ejb.PoolClass;
import org.jboss.ejb3.StrictMaxPool;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Apr 2, 2010
 * Time: 1:57:12 PM
 */
@MessageDriven(name = "RnaSeqPipelineLauncherMDB", activationConfig = {
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge "),
        @ActivationConfigProperty(propertyName = "messagingType", propertyValue = "javax.jms.MessageListener"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/rnaSeqPipelineLauncher"),
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "3"),
//    @ActivationConfigProperty(propertyName="MaxMessages", propertyValue="20"),
        @ActivationConfigProperty(propertyName = "transactionTimeout", propertyValue = "432000"),
        // DLQMaxResent is a JBoss-specific management property. 0 = no resent messages
        @ActivationConfigProperty(propertyName = "DLQMaxResent", propertyValue = "0")
})
@PoolClass(value = StrictMaxPool.class, maxSize = 3, timeout = 10000)
public class RnaSeqPipelineLauncherMDB extends SeriesLauncherMDB {

}