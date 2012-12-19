package org.janelia.it.jacs.shared.utils.entity;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;

/**
 * A visitor for EntityDatas and Entities. The implementator can choose which visit events to listen to.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityVisitor {

	public void visit(EntityData entityData) throws Exception {
	}

	public void visit(Entity entity) throws Exception {
	}
}
