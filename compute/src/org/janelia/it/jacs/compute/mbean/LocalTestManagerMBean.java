package org.janelia.it.jacs.compute.mbean;

/**
 * Interface for tests of local bean interfaces.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface LocalTestManagerMBean {
    	
	public void testBulkLoadEntityTree(String ownerKey) throws Exception;

}