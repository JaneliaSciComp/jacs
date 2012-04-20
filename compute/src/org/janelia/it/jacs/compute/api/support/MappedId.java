package org.janelia.it.jacs.compute.api.support;

import java.io.Serializable;

/**
 * A mapping between two ids.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MappedId implements Serializable {

	private Long originalId;
	private Long mappedId;
	
	public MappedId(Long originalId, Long mappedId) {
		this.originalId = originalId;
		this.mappedId = mappedId;
	}
	
	public Long getOriginalId() {
		return originalId;
	}
	public Long getMappedId() {
		return mappedId;
	}
}
