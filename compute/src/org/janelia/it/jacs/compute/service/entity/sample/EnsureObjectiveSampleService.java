package org.janelia.it.jacs.compute.service.entity.sample;

import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Ensures that the sample id points to the correct objective sample.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EnsureObjectiveSampleService extends AbstractEntityService {

    public void execute() throws Exception {
        
        String objective = data.getRequiredItemAsString("OBJECTIVE");
    	Long sampleEntityId = data.getRequiredItemAsLong("SAMPLE_ENTITY_ID");
    	
    	Entity sample = entityBean.getEntityById(sampleEntityId);
    	    	
    	String sampleObjective = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE);
    	
    	if (sampleObjective==null || !sampleObjective.equals(objective)) {
    		boolean hasChildSamples = false;
    		entityLoader.populateChildren(sample);
            for(Entity childSample : EntityUtils.getChildrenOfType(sample, EntityConstants.TYPE_SAMPLE)) {
            	hasChildSamples = true;
            	String childSampleObjective = childSample.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE);
            	if (childSampleObjective!=null && childSampleObjective.equals(objective)) {
            		sampleEntityId = childSample.getId();
            		break;
            	}
            }
            if (hasChildSamples && sampleEntityId.equals(sample.getId())) {
            	throw new Exception("Sample has sub-samples, but none with objective: "+objective);
            }
    	}
    	
    	data.putItem("SAMPLE_ENTITY_ID", sampleEntityId.toString());
    }
}
