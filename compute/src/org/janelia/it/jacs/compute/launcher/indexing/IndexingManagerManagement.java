package org.janelia.it.jacs.compute.launcher.indexing;

//import org.jboss.ejb3.annotation.Management;

import javax.management.MXBean;

/**
 * The EJB management interface for the indexing manager.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
//@Management
@MXBean
public interface IndexingManagerManagement {
	
	void create() throws Exception;

	void start() throws Exception;

	void stop();

	void destroy();

	public void scheduleIndexing(Long domainObjectId, String clazz);

	public void scheduleRemoval(Long domainObjectId);

	public void scheduleAddNewAncestor(Long domainObjectId, Long newAncestorId);
	
	public int runNextBatch();
}
