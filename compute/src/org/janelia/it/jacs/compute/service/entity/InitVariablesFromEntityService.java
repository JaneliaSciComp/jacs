package org.janelia.it.jacs.compute.service.entity;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.shared.utils.StringUtils;


/**
 * Initializes any number of process variables from corresponding entity attributes. Inputs:
 *   ENTITY_ATTRIBUTE_{X}
 *   PROCESS_VARIABLE_{X}
 *   OVERRIDE=true|false
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class InitVariablesFromEntityService extends AbstractDomainService {

    public void execute() throws Exception {
        
        boolean override = true;
        String overrideStr = data.getItemAsString("OVERRIDE");
        if (!StringUtils.isEmpty(overrideStr)) {
            override = Boolean.parseBoolean(overrideStr);
        }
        contextLogger.debug("Will " + (override ? "" : "not ") + "override existing variables");
        
        Entity entity = (Entity)processData.getItem("ENTITY");
        
        if (entity==null) {
            String entityId = (String)processData.getItem("ENTITY_ID");
            
            if (entityId==null) {
            	throw new IllegalArgumentException("Both ENTITY and ENTITY_ID may not be null");
            }
            
            entity = entityBean.getEntityById(entityId);
        }

        if (entity==null) {
        	throw new IllegalArgumentException("Entity is null");
        }

        for (int i = 1; i < 100; i++) {
            String enttiyAttName = (String) processData.getItem("ENTITY_ATTRIBUTE_" + i);
            if (enttiyAttName == null) {
                break;
            }
            String processVarName = (String) processData.getItem("PROCESS_VARIABLE_" + i);
            String value = entity.getValueByAttributeName(enttiyAttName);
            contextLogger.debug("Init "+processVarName+" with value '"+value+"' from task parameter "+enttiyAttName);
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
