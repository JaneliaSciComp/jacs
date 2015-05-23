package org.janelia.it.jacs.compute.service.entity;

/**
 * Sets an attribute on an entity.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SetEntityAttributeValueService extends AbstractEntityService {

    public void execute() throws Exception {
    	Long entityId = data.getRequiredItemAsLong("ENTITY_ID");
    	String attributeName = data.getRequiredItemAsString("ATTRIBUTE_NAME");
    	String value = data.getRequiredItemAsString("ATTRIBUTE_VALUE");
    	logger.info("Setting '"+attributeName+"' to '"+value+"' on entity "+entityId);
    	entityBean.setOrUpdateValue(entityId, attributeName, value);
    }
}
