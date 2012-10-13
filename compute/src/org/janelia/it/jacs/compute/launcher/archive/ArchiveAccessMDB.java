package org.janelia.it.jacs.compute.launcher.archive;

import java.io.File;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJBException;
import javax.ejb.MessageDriven;
import javax.jms.Message;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.engine.launcher.ejb.SeriesLauncherMDB;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.shared.utils.SystemCall;
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

	protected Logger logger = Logger.getLogger(ArchiveAccessMDB.class);
	
	protected static final String JACS_DATA_DIR =
        SystemConfigurationProperties.getString("JacsData.Dir.Linux");
	
    protected static final String JACS_DATA_ARCHIVE_DIR =
            SystemConfigurationProperties.getString("JacsData.Dir.Archive.Linux");
    
    protected static final String COPY_COMMAND = "cp -a"; 
    protected static final String SYNC_COMMAND = "rsync -aW"; 
    
	@Override
	public void onMessage(Message message) {
        try {
        	String request = message.getStringProperty("REQUEST");
			if (request==null) {
				super.onMessage(message);
			}
			else if ("syncDir".equals(request)) {
	    		String filePath = message.getStringProperty("FILE_PATH");
				syncDir(filePath);
			}
        }
        catch (Exception e) {
        	throw new EJBException(e);
        }
	}

    public void syncDir(String filePath) throws Exception {
        
    	logger.info("Synchronizing with archive: "+filePath);
    	
    	String archivePath = null;
    	String truePath = null;
    	if (filePath.contains(JACS_DATA_DIR)) {
    		truePath = filePath;
    		archivePath = filePath.replaceFirst(JACS_DATA_DIR, JACS_DATA_ARCHIVE_DIR);
    	}
    	else if (filePath.contains(JACS_DATA_ARCHIVE_DIR)) {
    		archivePath = filePath;
    		truePath = filePath.replaceFirst(JACS_DATA_ARCHIVE_DIR, JACS_DATA_DIR);
    	}
    	else {
    		throw new ServiceException("Unrecognized path: "+filePath);
    	}
    	
    	File file = new File(truePath);
    	StringBuffer script = new StringBuffer();
    	
    	if (file.exists()) {
    		// Destination already exists, just update it
    		script.append(SYNC_COMMAND+" "+archivePath+" "+file.getParent());
    	}
    	else {
    		// Destination does not exist, sync to a temp directory and then move it into place
    		File tempFile = new File(file.getParent(),"tmp-"+System.nanoTime());
        	script.append(COPY_COMMAND+" "+archivePath+" "+tempFile.getAbsolutePath()+"; ");
        	script.append("mv "+tempFile.getAbsolutePath()+" "+truePath);
    	}
    	        	
        SystemCall call = new SystemCall(logger);
        int exitCode = call.emulateCommandLine(script.toString(), true, 60);

        if (0!=exitCode) {
        	throw new ServiceException("Synchronization from archive failed with exitCode "+exitCode);
        }
    }
}
