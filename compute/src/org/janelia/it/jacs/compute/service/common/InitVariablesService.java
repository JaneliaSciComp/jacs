package org.janelia.it.jacs.compute.service.common;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.shared.utils.ReflectionUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;


/**
 * Initializes any number of process variables with string values. Inputs:
 *   PROCESS_VARIABLE_{X}
 *   PROCESS_VARIABLE_VALUE_{X}
 *   
 * The PROCESS_VARIABLE_{X} may contain a variable name, or a variable name followed by a property:
 * "VARIABLE"
 * "VARIABLE.propertyToSet"
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class InitVariablesService implements IService {

    public void execute(IProcessData processData) throws ServiceException {
        try {
            Logger logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());

            boolean override = true;
            String overrideStr = (String)processData.getItem("OVERRIDE");
            if (!StringUtils.isEmpty(overrideStr)) {
                override = Boolean.parseBoolean(overrideStr);   
                logger.info("Will "+(override?"":"not ")+"override existing variables");
            }
            
        	int num = 1;
        	while (true) {
        		String processVarName = (String)processData.getItem("PROCESS_VARIABLE_"+num);	
        		if (processVarName == null || num>100) break;
        		Object value = processData.getItem("PROCESS_VARIABLE_VALUE_"+num);
        		
        		if (processVarName.contains(".")) {
        		    String[] parts = processVarName.split("\\.");
        		    String beanName = parts[0];
        		    String beanPropertyName = parts[1];
        		    Object bean = processData.getItem(beanName);
        		    if (bean==null) {
        		        throw new IllegalArgumentException(beanName+" may not be null");
        		    }
                    if (override || ReflectionUtils.get(bean, beanPropertyName)==null) {
                        logger.info("Setting "+beanPropertyName+" value on "+beanName+" to '"+value+"'");
                        ReflectionUtils.set(bean, beanPropertyName, value);
                    }
        		}
        		else {
        		    if (override || processData.getItem(processVarName)==null) {
                        logger.info("Putting value '"+value+"' in "+processVarName);
                        processData.putItem(processVarName, value);    
        		    }
        		}
        		
                num++;
        	}
        } 
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }
}
