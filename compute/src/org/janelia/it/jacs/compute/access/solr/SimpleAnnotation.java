package org.janelia.it.jacs.compute.access.solr;

import java.io.Serializable;

/**
 * Simplified annotation representation for the purposes of SOLR indexing.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SimpleAnnotation implements Serializable {

	private String tag;
	private String subjectsCsv;
	
	public SimpleAnnotation(String tag, String subjectsCsv) {
		this.tag = tag;
		this.subjectsCsv = subjectsCsv;
	}
	
	public String getTag() {
		return tag;
	}

	public String getSubjectsCsv() {
        return subjectsCsv;
    }
}
