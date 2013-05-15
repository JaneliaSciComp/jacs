package org.janelia.it.jacs.compute.launcher.archive;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJBException;
import javax.ejb.MessageDriven;
import javax.jms.Message;
import javax.jms.Queue;

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
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "5"),
        @ActivationConfigProperty(propertyName = "transactionTimeout", propertyValue = "432000"),
        @ActivationConfigProperty(propertyName = "DLQMaxResent", propertyValue = "0")
})
@PoolClass(value = org.jboss.ejb3.StrictMaxPool.class, maxSize = 5, timeout = 10000)
public class ArchiveAccessMDB extends SeriesLauncherMDB {

    private static Logger logger = Logger.getLogger(ArchiveAccessMDB.class);
    
    public static final String REQUEST_MOVE_TO_ARCHIVE = "moveToArchive";
    public static final String REQUEST_COPY_FROM_ARCHIVE = "copyFromArchive";
    
    @Depends({"jboss:custom=ArchivalManager"})
    private ArchivalManagerManagement archivalManager;
    
    @Override
    public void onMessage(Message message) {
        try {
            String request = message.getStringProperty("REQUEST");
            List<String> filePathList = new ArrayList<String>();
            if (REQUEST_MOVE_TO_ARCHIVE.equals(request)) {
                logger.debug("Limiting archive access for moveToArchive request");
                String filepath = message.getStringProperty("FILE_PATH");
                if (filepath==null) {
                    String filepaths = message.getStringProperty("FILE_PATHS");
                    if (filepaths==null) {
                        throw new IllegalStateException("Both FILE_PATH and FILE_PATHS cannot be null when REQUEST="+request);
                    }
                    filePathList.addAll(Task.listOfStringsFromCsvString(filepaths));
                    archivalManager.moveToArchive(filePathList);      
                }
                else {
                    filePathList.add(filepath);
                    archivalManager.moveToArchive(filepath);      
                }
                
                List<String> targetFilepaths = new ArrayList<String>();
                for(String sourceFilepath : filePathList) {
                    targetFilepaths.add(sourceFilepath.replaceFirst("/archive", "/groups"));
                }
            }
            else if (REQUEST_COPY_FROM_ARCHIVE.equals(request)) {
                logger.debug("Limiting archive access for copyFromArchive request");
                String filepath = message.getStringProperty("SOURCE_FILE_PATH");
                String targetFilepaths = "";
                if (filepath==null) {
                    String filepaths = message.getStringProperty("SOURCE_FILE_PATHS");
                    if (filepaths==null) {
                        throw new IllegalStateException("Both SOURCE_FILE_PATH and SOURCE_FILE_PATHS cannot be null when REQUEST="+request);
                    }
                    targetFilepaths = message.getStringProperty("TARGET_FILE_PATHS");
                    archivalManager.copyFromArchive(Task.listOfStringsFromCsvString(filepaths), Task.listOfStringsFromCsvString(targetFilepaths));      
                }
                else {
                    targetFilepaths = message.getStringProperty("TARGET_FILE_PATH");
                    archivalManager.copyFromArchive(filepath, targetFilepaths);      
                }
            }
            else {
                logger.debug("Limiting archive access for message");
                super.onMessage(message);
                return;
            }
        }
        catch (Exception e) {
            throw new EJBException(e);
        }
    }
}
