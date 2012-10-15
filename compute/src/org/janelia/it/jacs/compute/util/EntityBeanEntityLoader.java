package org.janelia.it.jacs.compute.util;

import java.util.Set;

import org.janelia.it.jacs.compute.api.EntityBeanRemote;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.entity.AbstractEntityLoader;

/**
 * Server-side implementation of the entity loader interface, using the remote EntityBean.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityBeanEntityLoader extends AbstractEntityLoader {

	private EntityBeanRemote entityBean;
	
	public EntityBeanEntityLoader(EntityBeanRemote entityBean) {
		this.entityBean = entityBean;
	}
	
	@Override
	public Set<EntityData> getParents(Entity entity) {
		return entityBean.getParentEntityDatas(entity.getId());
	}

	@Override
	public Entity populateChildren(Entity entity) {
		if (entity==null || EntityUtils.areLoaded(entity.getEntityData())) return entity;
		EntityUtils.replaceChildNodes(entity, entityBean.getChildEntities(entity.getId()));
		return entity;
	}	
}
