package org.janelia.it.jacs.compute.service.common;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;


/**
 * Initializes any number of process variables with string values. Inputs:
 *   PROCESS_VARIABLE_{X}
 *   PROCESS_VARIABLE_VALUE_{X}
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class InitVariablesService implements IService {

    public void execute(IProcessData processData) throws ServiceException {
        try {
            Logger logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());

        	int num = 1;
        	while (true) {
        		String processVarName = (String)processData.getItem("PROCESS_VARIABLE_"+num);	
        		if (processVarName == null || num>100) break;
        		String value = (String)processData.getItem("PROCESS_VARIABLE_VALUE_"+num);        		
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
