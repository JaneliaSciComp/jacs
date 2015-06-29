package org.janelia.it.jacs.compute.service.entity.sample;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.scality.ScalityDAO;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;

/**
 * Restore files that were put into Scality incorrectly.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ScalityCorrectionService extends AbstractEntityService {

	private static final Logger logger = Logger.getLogger(ScalityCorrectionService.class);
	
	private boolean isDebug = false;
	
	private ScalityDAO dao;
    private int numProcessed = 0;
    private int numFixed = 0;
    
    public void execute() throws Exception {

    	this.dao = new ScalityDAO();
    	
        if (isDebug) {
            logger.info("This is a test run. No files will be moved.");
        }
        else {
            logger.info("This is the real thing. Files will be moved from Scality to the filestore!");
        }

        for(Entity entity : entityBean.getEntitiesByName(ownerKey, "ConsolidatedLabel.v3dpbd")) {
        	
        	String bpid = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_SCALITY_BPID);
        	if (bpid!=null) {
        		Entity alignment = entityBean.getAncestorWithType(ownerKey, entity.getId(), EntityConstants.TYPE_ALIGNMENT_RESULT);
        		String alignpath = alignment.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        		String filepath = alignpath+"/"+entity.getName();
        		
        		if (!isDebug) {
        			dao.get(entity, filepath);
//        			dao.delete(entity);
            		EntityData ed = entity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_SCALITY_BPID);
        			entity.getEntityData().remove(ed);
        			entityBean.deleteEntityData(ed);
        			entityBean.setOrUpdateValue(ownerKey, entity.getId(), EntityConstants.ATTRIBUTE_FILE_PATH, filepath);
        		}
        		
        		logger.info("Moved "+entity.getId()+" to filestore");
        		
        		numFixed++;
        		break;
        	}
        	
        	numProcessed++;
        }
        
		logger.info("Processed "+numProcessed+" entities, fixed "+numFixed+".");
    }
    
    
}
