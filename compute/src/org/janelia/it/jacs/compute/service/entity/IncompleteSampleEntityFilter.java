package org.janelia.it.jacs.compute.service.entity;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * An entity filter which accepts non-completed Samples, unless the REFRESH parameter is set in the ProcessData.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class IncompleteSampleEntityFilter implements EntityFilter {

	private static final Logger logger = Logger.getLogger(IncompleteSampleEntityFilter.class);
	
	public boolean includeEntity(IProcessData processData, Entity entity) {
		
		// Do not include non-samples
		if (!entity.getEntityType().getName().equals(EntityConstants.TYPE_SAMPLE)) {
			return false;
		}
		
		// Include every sample if we're doing a refresh
		Object refreshParam = processData.getItem("REFRESH");
		if ((refreshParam instanceof String && "true".equals((String)refreshParam)) || 
				(refreshParam instanceof Boolean && (Boolean)refreshParam)) {
			return true;
		}
		
		// If we're not doing a refresh, do not include any sample that already has a result
		for(Entity child : entity.getChildren()) {
			if (child.getEntityType().getName().equals(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
				logger.info("Skipping completed sample "+entity.getName()+" (id="+entity.getId()+")");
				return false;
			}
		}
		
		// Include everything by default
		return true;
	}
}
