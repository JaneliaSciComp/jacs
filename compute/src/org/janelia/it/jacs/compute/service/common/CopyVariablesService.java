package org.janelia.it.jacs.compute.service.common;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;

/**
 * Copy any number of process variables to other variables. Inputs:
 *   PROCESS_VARIABLE_IN_{X}
 *   PROCESS_VARIABLE_OUT_{X}
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class CopyVariablesService implements IService {

    public void execute(IProcessData processData) throws ServiceException {
        try {
            Logger logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            
        	int num = 1;
        	while (true) {
        		String processVarIn = (String)processData.getItem("PROCESS_VARIABLE_IN_"+num);	
        		if (processVarIn == null || num>100) break;
        		String processVarOut = (String)processData.getItem("PROCESS_VARIABLE_OUT_"+num);	
        		Object value = processData.getItem(processVarIn);
        		if (value != null) {
	            	logger.info("Putting value '"+value+"' in "+processVarOut);
	            	processData.putItem(processVarOut, value);
        		}
        		else {
        		    logger.warn("Input variable ("+processVarIn+") is null, cannot populate "+processVarOut);
        		}
                num++;
        	}
        } 
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }
}
