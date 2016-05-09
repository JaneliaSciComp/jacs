package org.janelia.it.jacs.compute.service.domain;

/**
 * Sets an attribute on an domain object.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SetAttributeValueService extends AbstractDomainService {

    public void execute() throws Exception {
        String className = data.getRequiredItemAsString("OBJECT_CLASS");
    	Long id = data.getRequiredItemAsLong("OBJECT_ID");
    	String attributeName = data.getRequiredItemAsString("ATTRIBUTE_NAME");
    	String value = data.getRequiredItemAsString("ATTRIBUTE_VALUE");
    	contextLogger.info("Setting '"+attributeName+"' to '"+value+"' on "+className+"#"+id);
    	domainDao.updateProperty(ownerKey, className, id, attributeName, value);
    }
}
