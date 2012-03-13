
package org.janelia.it.jacs.compute.api;

import javax.ejb.Remote;

import org.apache.solr.client.solrj.SolrQuery;
import org.janelia.it.jacs.compute.api.support.SolrResults;

/**
 * A remote interface to querying the SOLR index server.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@Remote
public interface SolrBeanRemote {

	/**
	 * Execute the query and return SOLR's response. 
	 * Optionally map the returned documents to Entity objects.
	 * @param query The SOLR query to execute.
	 * @param mapToEntities also return the corresponding Entity objects?
	 * @return An object containing the SOLR response and possibly a set of matched Entity objects.
	 * @throws ComputeException
	 */
	public SolrResults search(SolrQuery query, boolean mapToEntities) throws ComputeException;
}
