package org.janelia.it.jacs.compute.launcher.archive;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;

import org.janelia.it.jacs.compute.engine.launcher.ejb.SeriesLauncherMDB;
import org.jboss.annotation.ejb.PoolClass;

/**
 * An MBD for handling archive requests asynchronously.
 * 
 * This is a temporary MDB for the complete archival of the entire file store. It is separate from the normal 
 * archiveAccessMDB so that the two do not interfere (i.e. so that normal archive requests can finish normally, without 
 * waiting on the samples queued as part of complete archival. 
 * 
 * TODO: eventually this MBD can be deleted, once we have moved all the existing data to archive 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@MessageDriven(name = "BackgroundArchiveAccessMDB", activationConfig = {
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge "),
        @ActivationConfigProperty(propertyName = "messagingType", propertyValue = "javax.jms.MessageListener"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/backgroundArchiveAccess"),
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "2"),
        @ActivationConfigProperty(propertyName = "transactionTimeout", propertyValue = "432000"),
        @ActivationConfigProperty(propertyName = "DLQMaxResent", propertyValue = "0")
})
@PoolClass(value = org.jboss.ejb3.StrictMaxPool.class, maxSize = 2, timeout = 10000)
public class BackgroundArchiveAccessMDB extends SeriesLauncherMDB {
}
