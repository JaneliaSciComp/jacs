package org.janelia.it.jacs.shared.utils.entity;

import java.util.Set;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;

/**
 * An interface for loading related entities. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface AbstractEntityLoader {

	public Set<EntityData> getParents(Entity entity) throws Exception;
	
	public Entity populateChildren(Entity entity) throws Exception;

}
