package org.janelia.it.jacs.compute.service.common;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.scality.ScalityDAO;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * Standardize the given filepath and put it in the given output variable. Inputs:
 *   FILE_PATH
 *   OUTPUT_VAR_NAME
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class StandardizePathService implements IService {

    public void execute(IProcessData processData) throws ServiceException {
        try {
            Logger logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            
            String filepath = (String)processData.getItem("FILE_PATH"); 
            if (filepath==null) {
                throw new IllegalArgumentException("FILE_PATH may not be null");
            }
            
            String processVarOut = (String)processData.getItem("OUTPUT_VAR_NAME");
            if (processVarOut==null) {
                throw new IllegalArgumentException("OUTPUT_VAR_NAME may not be null");
            }
            
            if (filepath.startsWith(EntityConstants.SCALITY_PATH_PREFIX)) {
                String bpid = filepath.replaceFirst(EntityConstants.SCALITY_PATH_PREFIX,"");
                filepath = ScalityDAO.getUrlFromBPID(bpid);    
            }
            
        	logger.info("Putting standard path '"+filepath+"' in "+processVarOut);
        	processData.putItem(processVarOut, filepath);
        } 
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }
}
