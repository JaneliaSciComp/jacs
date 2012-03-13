package org.janelia.it.jacs.compute.access.solr;

/**
 * Simplified annotation representation for the purposes of SOLR indexing.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SimpleAnnotation extends KeyValuePair {

	public SimpleAnnotation(String key, String value) {
		super(key, value);
	}
	
	public String getTag() {
    	return (getValue() == null) ? getKey() : getKey() + " = " + getValue();
	}

}
