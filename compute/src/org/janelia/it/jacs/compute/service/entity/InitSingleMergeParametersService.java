package org.janelia.it.jacs.compute.service.entity;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.v3d.MergedLsmPair;

import java.util.List;

/**
 * Merge paired LSMs into v3draw files. Parameters:
 *   RESULT_FILE_NODE - the directory to use for SGE config and output
 *   BULK_MERGE_PARAMETERS - a list of MergedLsmPair
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class InitSingleMergeParametersService implements IService {

    protected Logger logger;
    protected AnnotationBeanLocal annotationBean;


    public void execute(IProcessData processData) throws ServiceException {
        try {
        	
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            annotationBean = EJBFactory.getLocalAnnotationBean();

            Object bulkMergeParamObj = processData.getItem("BULK_MERGE_PARAMETERS");
            if (bulkMergeParamObj==null) {
            	throw new ServiceException("Input parameter BULK_MERGE_PARAMETERS may not be null");
            }
            
            if (bulkMergeParamObj instanceof List) {
            	List<MergedLsmPair> mergedLsmPairs = (List<MergedLsmPair>)bulkMergeParamObj;
            	if (mergedLsmPairs.size() != 1) {
            		throw new ServiceException("BULK_MERGE_PARAMETERS must contain exactly one merged pair");
            	}
            	processData.putItem("MERGED_FILENAME", mergedLsmPairs.get(0).getMergedFilepath());
            }
            
        	
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }
}
