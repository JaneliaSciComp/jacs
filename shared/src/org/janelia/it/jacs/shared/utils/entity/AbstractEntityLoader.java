package org.janelia.it.jacs.shared.utils.entity;

import java.util.Set;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;

/**
 * An interface for loading related entities. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class AbstractEntityLoader {

	public abstract Set<EntityData> getParents(Entity entity);
	
	public abstract Entity populateChildren(Entity entity);

}
