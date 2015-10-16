
package org.janelia.it.jacs.compute.engine.launcher.ejb;

import org.janelia.it.jacs.compute.engine.launcher.ILauncher;
import org.janelia.it.jacs.compute.engine.launcher.SequenceLauncher;
import org.jboss.annotation.ejb.PoolClass;
import org.jboss.ejb3.StrictMaxPool;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;

/**
 * This MDB wraps a SequenceLauncher and is used for asynchronous launching of a sequence.  Because it runs in a new
 * transaction context, it rollbacks the transaction when something goes wrong in SequenceLauncher.  Before it does so
 * it sends a jms response to the initiator of the sequence action if a response was required.
 *
 * @author Tareq Nabeel
 */
@MessageDriven(name = "SequenceLauncherMDB", activationConfig = {
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge "),
        @ActivationConfigProperty(propertyName = "messagingType", propertyValue = "javax.jms.MessageListener"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/AnonymousSequenceLauncher"),
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "500"),
//    @ActivationConfigProperty(propertyName="MaxMessages", propertyValue="50"),
        @ActivationConfigProperty(propertyName = "transactionTimeout", propertyValue = "432000"),
        // DLQMaxResent is a JBoss-specific management property. 0 = no resent messages
        @ActivationConfigProperty(propertyName = "DLQMaxResent", propertyValue = "0")
})
@PoolClass(value = StrictMaxPool.class, maxSize = 500, timeout = 80000)
public class SequenceLauncherMDB extends SeriesLauncherMDB {

    protected ILauncher getLauncher() {
        return new SequenceLauncher();
    }

}
