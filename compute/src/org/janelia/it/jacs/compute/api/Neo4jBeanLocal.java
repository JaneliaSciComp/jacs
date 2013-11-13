
package org.janelia.it.jacs.compute.api;

import javax.ejb.Local;

/**
 * A local interface to the Neo4j database.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@Local
public interface Neo4jBeanLocal {
		
	public void neo4jAllEntities(boolean clearDb) throws ComputeException;

}
