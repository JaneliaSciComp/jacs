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
	private Long childId;
	
	public KeyValuePair(String key, String value, Long childId) {
		this.key = key;
		this.value = value;
		this.childId = childId;
	}

	public KeyValuePair(String key, String value) {
		this(key, value, null);
	}
	
	public KeyValuePair(String key, Long childId) {
		this(key, null, childId);
	}
	
	public String getKey() {
		return key;
	}
	
	public String getValue() {
		return value;
	}
	
	public Long getChildId() {
		return childId;
	}
}
