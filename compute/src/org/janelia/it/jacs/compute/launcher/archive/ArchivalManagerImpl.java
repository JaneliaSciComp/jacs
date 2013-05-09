package org.janelia.it.jacs.compute.launcher.archive;

import java.util.Collection;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.service.utility.SyncToArchiveService;
import org.jboss.annotation.ejb.Management;
import org.jboss.annotation.ejb.Service;

/**
 * The archival manager is a singleton service responsible for archiving filepaths. 
 * It can be injected into other beans via the @Depends annotation.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@Service(objectName = "jboss:custom=ArchivalManager")
@Management(ArchivalManagerManagement.class)
public class ArchivalManagerImpl implements ArchivalManagerManagement {

	private static Logger logger = Logger.getLogger(ArchivalManagerImpl.class);
	
	public void create() throws Exception {
	}

	public void start() throws Exception {
	}

	public void stop() {
	}

	public void destroy() {
	}

	public void moveToArchive(String filepath) throws Exception {
        logger.debug("Executing archival of "+filepath);
	    SyncToArchiveService service = new SyncToArchiveService();
	    service.execute(filepath);
	}

    public void moveToArchive(Collection<String> filepaths) throws Exception {
        logger.debug("Executing archival of "+filepaths.size()+" file paths");
        SyncToArchiveService service = new SyncToArchiveService();
        service.execute(filepaths);
    }
}
