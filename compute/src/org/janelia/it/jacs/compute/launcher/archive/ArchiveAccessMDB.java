package org.janelia.it.jacs.compute.launcher.archive;

import java.util.Set;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJBException;
import javax.ejb.MessageDriven;
import javax.jms.Message;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.engine.launcher.ejb.SeriesLauncherMDB;
import org.janelia.it.jacs.model.tasks.Task;
import org.jboss.annotation.ejb.Depends;
import org.jboss.annotation.ejb.PoolClass;

/**
 * An MBD for handling archive requests asynchronously.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@MessageDriven(name = "ArchiveAccessMDB", activationConfig = {
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge "),
        @ActivationConfigProperty(propertyName = "messagingType", propertyValue = "javax.jms.MessageListener"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/archiveAccess"),
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "1"),
        @ActivationConfigProperty(propertyName = "transactionTimeout", propertyValue = "432000"),
        @ActivationConfigProperty(propertyName = "DLQMaxResent", propertyValue = "0")
})
@PoolClass(value = org.jboss.ejb3.StrictMaxPool.class, maxSize = 1, timeout = 10000)
public class ArchiveAccessMDB extends SeriesLauncherMDB {

    private static Logger logger = Logger.getLogger(ArchiveAccessMDB.class);
    
    public static final String REQUEST_MOVE_TO_ARCHIVE = "moveToArchive";
    
    @Depends({"jboss:custom=ArchivalManager"})
    private ArchivalManagerManagement archivalManager;
    
    @Override
    public void onMessage(Message message) {
        try {
            String request = message.getStringProperty("REQUEST");
            if (REQUEST_MOVE_TO_ARCHIVE.equals(request)) {
                logger.debug("Limiting archive access for moveToArchive request");
                String filepath = message.getStringProperty("FILE_PATH");
                if (filepath==null) {
                    String filepaths = message.getStringProperty("FILE_PATHS");
                    if (filepaths==null) {
                        throw new IllegalStateException("Both FILE_PATH and FILE_PATHS cannot be null when REQUEST="+REQUEST_MOVE_TO_ARCHIVE);
                    }
                    archivalManager.moveToArchive(Task.listOfStringsFromCsvString(filepaths));      
                }
                else {
                    archivalManager.moveToArchive(filepath);      
                }
            }
            else {
                logger.debug("Limiting archive access for message");
                super.onMessage(message);    
            }
        }
        catch (Exception e) {
            throw new EJBException(e);
        }
    }
}
