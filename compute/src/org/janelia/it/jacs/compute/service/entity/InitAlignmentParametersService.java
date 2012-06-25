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

	private static final String ALIGN_TYPE_WHOLE_40X = "WHOLE_40X";
	private static final String ALIGN_TYPE_WHOLE_63X = "WHOLE_63X";
	private static final String ALIGN_TYPE_OPTIC = "OPTIC";
	private static final String ALIGN_TYPE_CENTRAL = "CENTRAL";
	
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
    		
    		String alignmentType = (String)processData.getItem("ALIGNMENT_TYPE");
    		
    		if (alignmentType!=null && ALIGN_TYPE_WHOLE_40X.equalsIgnoreCase(alignmentType)) {
            	processData.putItem("ALIGNMENT_SERVICE_CLASS", "org.janelia.it.jacs.compute.service.align.WholeBrain40xAlignmentService");
            	processData.putItem("ALIGNMENT_RESULT_NAME", "Whole Brain 40x Alignment");
    		}
    		else if (alignmentType!=null && ALIGN_TYPE_WHOLE_63X.equalsIgnoreCase(alignmentType)) {
            	processData.putItem("ALIGNMENT_SERVICE_CLASS", "org.janelia.it.jacs.compute.service.align.WholeBrain63xAlignmentService");
            	processData.putItem("ALIGNMENT_RESULT_NAME", "Whole Brain 63x Alignment");
    		}
    		else {
    			// TODO: move this into an earlier step which then sets ALIGNMENT_TYPE
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
