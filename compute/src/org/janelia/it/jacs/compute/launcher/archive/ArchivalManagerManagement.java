package org.janelia.it.jacs.compute.launcher.archive;

import java.util.Collection;

import org.jboss.annotation.ejb.Management;

/**
 * The EJB management interface for the archival manager.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@Management
public interface ArchivalManagerManagement {
	
	void create() throws Exception;

	void start() throws Exception;

	void stop();

	void destroy();

	public void moveToArchive(String sourceFilepath) throws Exception;
	
	public void moveToArchive(Collection<String> sourceFilepaths) throws Exception;
	
	public void copyFromArchive(String sourceFilepath, String targetFilepath) throws Exception;
    
    public void copyFromArchive(Collection<String> sourceFilepaths, Collection<String> targetFilepaths) throws Exception;
}
