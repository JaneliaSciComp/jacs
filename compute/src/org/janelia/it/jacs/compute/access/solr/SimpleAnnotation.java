package org.janelia.it.jacs.compute.access.solr;

/**
 * Simplified annotation representation for the purposes of SOLR indexing.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SimpleAnnotation extends KeyValuePair {

	private String tag;
	private String owner;
	
	public SimpleAnnotation(String tag, String key, String value, String owner) {
		super(key, value);
		this.tag = tag;
		this.owner = owner;
	}
	
	public String getTag() {
		return tag;
	}

	public String getOwner() {
		return owner.contains(":") ? owner.split(":")[1] : owner;
	}
}
