package org.janelia.it.jacs.compute.service.entity;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * Get the id of a particular ancestor.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GetAncestorIdService extends AbstractDomainService {

	@Override 
    public void execute() throws Exception {
		
		String entityId = (String)processData.getItem("ENTITY_ID");
		
		Entity result = entityBean.getEntityById(entityId);
		Entity sample = entityBean.getAncestorWithType(result, EntityConstants.TYPE_SAMPLE);
		
        String outputVar = (String)processData.getItem("OUTPUT_VAR_NAME");
        
        if (outputVar!=null) {
        	processData.putItem(outputVar, sample.getId()+"");
        }
        else {
        	processData.putItem("ANCESTOR_ENTITY_ID", sample.getId()+"");
        }
    }
}