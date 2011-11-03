package org.janelia.it.jacs.compute.service.entity;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.model.entity.Entity;

/**
 * An interface for filtering entities. Used by the EntityTreeTraversalService to choose which entities to return.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface EntityFilter {

	public boolean includeEntity(IProcessData processData, Entity entity);
	
}
