package org.janelia.it.jacs.compute.service.entity;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.fileDiscovery.TilingPattern;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.mortbay.log.Log;

/**
 * Extracts alignment metadata about the Sample from the entity model and loads it into simplified objects for use by other services.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class InitAlignmentParametersService implements IService {

    protected Logger logger;

    public void execute(IProcessData processData) throws ServiceException {
        try {
        	
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
        	
        	String sampleEntityId = (String)processData.getItem("SAMPLE_ENTITY_ID");
        	if (sampleEntityId == null || "".equals(sampleEntityId)) {
        		throw new IllegalArgumentException("SAMPLE_ENTITY_ID may not be null");
        	}
        	
        	Entity sampleEntity = EJBFactory.getLocalEntityBean().getEntityTree(new Long(sampleEntityId));
        	if (sampleEntity == null) {
        		throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
        	}
        	
    		String strTilingPattern = sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_TILING_PATTERN);
    		
    		if (strTilingPattern==null) {
    			Log.warn("Sample "+sampleEntityId+" has no tiling pattern. Cannot proceed with alignment.");
    			return;
    		}
    		
    		String run40xAligner = (String)processData.getItem("RUN_40X_ALIGNER");
    		String run63xAligner = (String)processData.getItem("RUN_63X_ALIGNER");
    		
    		if (run40xAligner!=null && "true".equalsIgnoreCase(run40xAligner)) {
            	processData.putItem("ALIGNMENT_SERVICE_CLASS", "org.janelia.it.jacs.compute.service.align.WholeBrain40xAlignmentService");
            	processData.putItem("ALIGNMENT_RESULT_NAME", "Whole Brain 40x Alignment");
    		}
    		else if (run63xAligner!=null && "true".equalsIgnoreCase(run63xAligner)) {
            	processData.putItem("ALIGNMENT_SERVICE_CLASS", "org.janelia.it.jacs.compute.service.align.WholeBrain63xAlignmentService");
            	processData.putItem("ALIGNMENT_RESULT_NAME", "Whole Brain 63x Alignment");
    		}
    		else {
        		TilingPattern pattern = TilingPattern.valueOf(strTilingPattern);

        		if (pattern == TilingPattern.OPTIC_TILE) {
                	processData.putItem("ALIGNMENT_SERVICE_CLASS", "org.janelia.it.jacs.compute.service.align.OpticLobeAlignmentService");
                	processData.putItem("ALIGNMENT_RESULT_NAME", "Optic Lobe Alignment");
                	// TODO: there should be a better way to get this...
                	String[] parts = sampleEntity.getName().split("-");
                	String tileName = parts[parts.length-1].replaceAll("_", " ");
                	processData.putItem("ALIGNMENT_TILE_NAME", tileName);
        		}
        		else {
                	processData.putItem("ALIGNMENT_SERVICE_CLASS", "org.janelia.it.jacs.compute.service.align.BrainAlignmentService");
                	processData.putItem("ALIGNMENT_RESULT_NAME", "Central Brain Alignment");
        		}
    		}
        	
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }
}
