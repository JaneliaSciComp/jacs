package org.janelia.it.jacs.compute.service.common;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.shared.utils.ReflectionUtils;


/**
 * Initializes any number of process variables from corresponding bean properties. Inputs:
 *   BEAN_PROPERTY_{X}
 *   PROCESS_VARIABLE_{X}
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class InitVariablesFromBeanService implements IService {

    public void execute(IProcessData processData) throws ServiceException {
        try {
            Logger logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            
            Object bean = processData.getItem("BEAN");
            
            if (bean==null) {
	            String beanName = (String)processData.getItem("BEAN_NAME");
	            
	            if (beanName==null) {
	            	throw new IllegalArgumentException("Both BEAN and BEAN_NAME may not be null");
	            }
	            
	            bean = processData.getItem(beanName);
            }

            if (bean==null) {
            	throw new IllegalArgumentException("Bean is null");
            }
            
        	int num = 1;
        	while (true) {
        		String beanPropertyName = (String)processData.getItem("BEAN_PROPERTY_"+num);	
        		if (beanPropertyName == null || num>100) break;
        		String processVarName = (String)processData.getItem("PROCESS_VARIABLE_"+num);
        		Object value = ReflectionUtils.get(bean, beanPropertyName);
            	logger.info("Putting value '"+value+"' in "+processVarName);
            	processData.putItem(processVarName, value);
                num++;
        	}
        } 
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }
}
