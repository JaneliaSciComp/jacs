package org.janelia.it.jacs.compute.access.solr;

import org.janelia.it.jacs.compute.api.support.SolrUtils;

/**
 * Simplified annotation representation for the purposes of SOLR indexing.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SimpleAnnotation extends KeyValuePair {

	private String owner;
	
	public SimpleAnnotation(String key, String value, String owner) {
		super(key, value);
		this.owner = owner;
	}
	
	public String getTag() {
    	return SolrUtils.getAnnotationTag(getKey(), getValue());
	}

	public String getOwner() {
		return owner;
	}
}
