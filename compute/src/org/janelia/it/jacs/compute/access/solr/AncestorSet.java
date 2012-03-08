package org.janelia.it.jacs.compute.access.solr;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * A little helper class that gets serialized in EhCache to keep track of ancestors.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AncestorSet implements Serializable {

	private Set<Long> ancestors = new HashSet<Long>();
	private boolean complete = false;

	public Set<Long> getAncestors() {
		return ancestors;
	}
	public void setAncestors(Set<Long> ancestors) {
		this.ancestors = ancestors;
	}
	public boolean isComplete() {
		return complete;
	}
	public void setComplete(boolean complete) {
		this.complete = complete;
	}
}
