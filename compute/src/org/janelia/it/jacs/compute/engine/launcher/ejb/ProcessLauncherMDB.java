
package org.janelia.it.jacs.compute.engine.launcher.ejb;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.ComputeException;
import org.janelia.it.jacs.compute.engine.data.QueueMessage;
import org.janelia.it.jacs.compute.engine.def.SeriesDef;
import org.janelia.it.jacs.compute.engine.launcher.ILauncher;
import org.janelia.it.jacs.compute.engine.launcher.ProcessLauncher;
import org.janelia.it.jacs.compute.engine.util.JmsUtil;
import org.jboss.annotation.ejb.PoolClass;
import org.jboss.ejb3.StrictMaxPool;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJBException;
import javax.ejb.MessageDriven;
import javax.jms.Message;

/**
 * This MDB wraps a ProcessLauncher and is used for asynchronous launching of a process.  Because it runs in a new
 * transaction context, it rollbacks the transaction when something goes wrong in ProcessLauncher.  Before it does so
 * it sends a jms response to the initiator of the process action if a response was required.
 *
 * @author Tareq Nabeel
 */
@MessageDriven(name = "ProcessLauncherMDB", activationConfig = {
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge "),
        @ActivationConfigProperty(propertyName = "messagingType", propertyValue = "javax.jms.MessageListener"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/processLauncher"),
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "200"),
//    @ActivationConfigProperty(propertyName="MaxMessages", propertyValue="20"),
        @ActivationConfigProperty(propertyName = "transactionTimeout", propertyValue = "432000"),
        // DLQMaxResent is a JBoss-specific management property. 0 = no resent messages
        @ActivationConfigProperty(propertyName = "DLQMaxResent", propertyValue = "0")
})
@PoolClass(value = StrictMaxPool.class, maxSize = 200, timeout = 10000)
public class ProcessLauncherMDB extends SeriesLauncherMDB {
    private static Logger logger = Logger.getLogger(ProcessLauncherMDB.class);

    protected ILauncher getLauncher() {
        return new ProcessLauncher();
    }

    public void onMessage(Message message) {
        if (logger.isInfoEnabled()) {
            logger.info(getClass().getName() + " onMessage...");
        }
        processMessage(message);
    }


    protected void handleException(SeriesDef seriesToProcess, QueueMessage queueMessage, ComputeException e) {
        JmsUtil.replyToReceivedFromQueue(queueMessage, seriesToProcess, e);
        throw new EJBException(e);   // rollback
    }


}
