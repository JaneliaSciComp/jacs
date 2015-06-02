package org.janelia.it.jacs.compute.service.common;

import java.util.Map;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.shared.utils.ReflectionUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;


/**
 * Initializes any number of process variables with string values. Inputs:
 *   PROCESS_VARIABLE_{X}
 *   PROCESS_VARIABLE_VALUE_{X}
 *   OVERRIDE=true|false
 *   
 * The PROCESS_VARIABLE_{X} may contain a variable name, or a variable name followed by a property:
 * "VARIABLE"
 * "VARIABLE.propertyToSet"
 *   
 * Alternatively, the parameter names and values can be specified with PROCESS_VARIABLE_MAP
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class InitVariablesService implements IService {

    private IProcessData processData;
    private ContextLogger contextLogger;
    private Task task;
    private boolean override;
    
    public void execute(IProcessData processData) throws ServiceException {
        try {
            this.processData = processData;
            Logger logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            this.task = ProcessDataHelper.getTask(processData);
            this.contextLogger = new ContextLogger(logger);
            contextLogger.appendToLogContext(task);
            
            this.override = true;
            String overrideStr = (String)processData.getItem("OVERRIDE");
            if (!StringUtils.isEmpty(overrideStr)) {
                override = Boolean.parseBoolean(overrideStr);
            }
            contextLogger.debug("Will " + (override ? "" : "not ") + "override existing variables");
            
        	int num = 1;
        	while (true) {
        		String processVarName = (String)processData.getItem("PROCESS_VARIABLE_"+num);	
        		if (processVarName == null || num>100) break;
        		Object value = processData.getItem("PROCESS_VARIABLE_VALUE_"+num);
                if (value==null) continue;
        		processEntry(processVarName, value);
                num++;
        	}
        	
        	Map<String,String> processVarMap = (Map)processData.getItem("PROCESS_VARIABLE_MAP");
        	if (processVarMap!=null) {
        	    for(Map.Entry<String, String> entry : processVarMap.entrySet()) {
        	        processEntry(entry.getKey(), entry.getValue());
        	    }
        	}
        	
        } 
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }
    
    private void processEntry(String processVarName, Object value) throws Exception {
        
        // Convert boolean values
        if (value.equals("true")) {
            value = Boolean.TRUE;
        }
        else if (value.equals("false")) {
            value = Boolean.FALSE;
        }

        if (processVarName.contains(".")) {
            // This is a bean property
            String[] parts = processVarName.split("\\.");
            String beanName = parts[0];
            String beanPropertyName = parts[1];
            Object bean = processData.getItem(beanName);
            if (bean==null) {
                throw new IllegalArgumentException(beanName+" may not be null");
            }
            if (override || ReflectionUtils.get(bean, beanPropertyName)==null) {
                contextLogger.info("Setting "+beanPropertyName+" value on "+beanName+" to '"+value+"'");
                ReflectionUtils.set(bean, beanPropertyName, value);
            }
        }
        else {
            // This is a regular process data variable
            if (override || processData.getItem(processVarName)==null) {
                contextLogger.info("Putting value '"+value+"' in "+processVarName);
                processData.putItem(processVarName, value);    
            }
        }
    }
}
