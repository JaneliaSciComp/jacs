package org.janelia.it.jacs.compute.service.entity;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Initializes any number of process variables from corresponding entity attributes. Inputs:
 *   ENTITY_ATTRIBUTE_{X}
 *   PROCESS_VARIABLE_{X}
 *   OVERRIDE=true|false
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class InitVariablesFromDomainObjectService extends AbstractDomainService {

    public void execute() throws Exception {
        
        boolean override = true;
        String overrideStr = data.getItemAsString("OVERRIDE");
        if (!StringUtils.isEmpty(overrideStr)) {
            override = Boolean.parseBoolean(overrideStr);
        }
        contextLogger.debug("Will " + (override ? "" : "not ") + "override existing variables");
        
        String domainObjectClassName = data.getRequiredItemAsString("DOMAIN_OBJECT_CLASS_NAME");
        Long domainObjectId = data.getRequiredItemAsLong("DOMAIN_OBJECT_ID");
        
        if (domainObjectId==null) {
        	throw new IllegalArgumentException("Both ENTITY and ENTITY_ID may not be null");
        }
        
        DomainObject domainObject = domainDao.getDomainObject(ownerKey, Reference.createFor(domainObjectClassName, domainObjectId));

        if (domainObject==null) {
        	throw new IllegalArgumentException("Entity is null");
        }

        for (int i = 1; i < 100; i++) {
            String domainObjectAttrName = (String) processData.getItem("DOMAIN_ATTRIBUTE_" + i);
            if (domainObjectAttrName == null) {
                break;
            }
            String processVarName = (String) processData.getItem("PROCESS_VARIABLE_" + i);
            
            Object value = DomainUtils.getAttributeValue(domainObject, domainObjectAttrName);
            
            contextLogger.debug("Init "+processVarName+" with value '"+value+"' from task parameter "+domainObjectAttrName);
            if (value != null) {
                // We specifically avoid overriding existing values in ProcessData, because if the process file
                // is being <include>'d, then the task may not contain the parameter which is already in
                // ProcessData.
                if (override || processData.getItem(processVarName)==null) {
                    data.putItem(processVarName, value);
                }
                else {
                    contextLogger.debug("  Will not override existing value '"+processData.getItem(processVarName)+"'");
                }
            }
        }
    }
}
