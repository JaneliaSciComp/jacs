
package org.janelia.it.jacs.compute.api;

import javax.ejb.Local;

/**
 * A local interface for invoking SOLR indexing. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@Local
public interface SolrBeanLocal {
	
	public void indexAllEntities(boolean clearIndex) throws ComputeException;

	public void mongoAllEntities(boolean clearDb) throws ComputeException;
	
	public void neo4jAllEntities(boolean clearDb) throws ComputeException;

}
