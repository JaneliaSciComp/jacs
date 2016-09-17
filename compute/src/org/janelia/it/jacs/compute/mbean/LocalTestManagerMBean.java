package org.janelia.it.jacs.compute.mbean;

import javax.management.MXBean;

/**
 * Interface for tests of local bean interfaces.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@MXBean
public interface LocalTestManagerMBean {
    	
	public void testBulkLoadEntityTree(String ownerKey) throws Exception;

}