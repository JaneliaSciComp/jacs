package org.janelia.it.jacs.compute.launcher.indexing;

import org.jboss.annotation.ejb.Management;

/**
 * The EJB management interface for the indexing manager.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@Management
public interface IndexingManagerManagement {
	
	void create() throws Exception;

	void start() throws Exception;

	void stop();

	void destroy();

	public void scheduleIndexing(Long entityId);

	public int runNextBatch();
}
