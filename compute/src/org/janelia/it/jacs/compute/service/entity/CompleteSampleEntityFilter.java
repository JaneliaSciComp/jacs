package org.janelia.it.jacs.compute.service.entity;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * An entity filter which accepts completed Samples only.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class CompleteSampleEntityFilter implements EntityFilter {

	public boolean includeEntity(IProcessData processData, Entity entity) {
		
		// Do not include non-samples
		if (!entity.getEntityType().getName().equals(EntityConstants.TYPE_SAMPLE)) {
			return false;
		}
		
		// Include any sample that has a result
		for(Entity child : entity.getChildren()) {
			if (child.getEntityType().getName().equals(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
				return true;
			}
		}
		
		// Exclude everything by default
		return false;
	}
}
