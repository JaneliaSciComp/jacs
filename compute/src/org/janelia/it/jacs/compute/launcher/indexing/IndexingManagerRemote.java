package org.janelia.it.jacs.compute.launcher.indexing;

import javax.ejb.Remote;


/**
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@Remote
public interface IndexingManagerRemote {

	public void scheduleIndexing(Long entityId);
	
	public int runNextBatch();
}
