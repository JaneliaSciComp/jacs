package org.janelia.it.jacs.compute.access.solr;

import java.io.Serializable;

/**
 * Self-explanatory.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class KeyValuePair implements Serializable {

	private String key;
	private String value;
	
	public KeyValuePair(String key, String value) {
		this.key = key;
		this.value = value;
	}
	
	public String getKey() {
		return key;
	}
	
	public String getValue() {
		return value;
	}
}
