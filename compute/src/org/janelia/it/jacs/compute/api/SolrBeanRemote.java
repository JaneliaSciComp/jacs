
package org.janelia.it.jacs.compute.api;

import org.apache.solr.client.solrj.SolrQuery;
import org.janelia.it.jacs.compute.access.ComputeException;
import org.janelia.it.jacs.shared.solr.SageTerm;
import org.janelia.it.jacs.shared.solr.SolrResults;

import javax.ejb.Remote;
import java.util.Map;

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
	public SolrResults search(String subjectKey, SolrQuery query, boolean mapToEntities) throws ComputeException;
	
	/**
	 * Returns the imagery vocabulary from Sage.
	 * @return Map of simple vocabulary term names to objects describing them more fully.
	 */
	public Map<String, SageTerm> getImageVocabulary() throws ComputeException;
}
