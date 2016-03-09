
package org.janelia.it.jacs.compute.api;

import javax.ejb.Local;

/**
 * A local interface for invoking SOLR indexing. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@Local
public interface SolrBeanLocal extends SolrBeanRemote {
	
	public void indexAllEntities(boolean clearIndex) throws ComputeException;
	
	public void indexAllEntitiesInTree(Long entityId) throws ComputeException;
	
    public void mongoAllDomainObjects(boolean clearDb) throws ComputeException;

}
