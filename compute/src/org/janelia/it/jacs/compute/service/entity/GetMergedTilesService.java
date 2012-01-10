package org.janelia.it.jacs.compute.service.entity;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.lsm.MergedTile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Gets all the merged tiles for a given sample.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GetMergedTilesService implements IService {

    protected Logger logger;
    protected AnnotationBeanLocal annotationBean;

    public void execute(IProcessData processData) throws ServiceException {
    	
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            annotationBean = EJBFactory.getLocalAnnotationBean();
            
        	String sampleEntityId = (String)processData.getItem("SAMPLE_ENTITY_ID");
        	if (sampleEntityId == null) {
        		throw new IllegalArgumentException("SAMPLE_ENTITY_ID may not be null");
        	}
        	
        	Entity sampleEntity = annotationBean.getEntityTree(new Long(sampleEntityId));
        	if (sampleEntity == null) {
        		throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
        	}
        	
        	List<MergedTile> tiles = new ArrayList<MergedTile>();
        	
        	for(Entity lsmPairEntity : sampleEntity.getDescendantsOfType(EntityConstants.TYPE_LSM_STACK_PAIR)) {
        		Entity mergedStack = lsmPairEntity.getChildByAttributeName(EntityConstants.ATTRIBUTE_MERGED_STACK);
        		if (mergedStack != null) {
        			File mergedFile = new File(mergedStack.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
        			MergedTile tile = new MergedTile(lsmPairEntity.getName(), mergedFile);
        			logger.info("Adding merged tile: "+tile.getName());
        			tiles.add(tile);
        		}
        	}

        	logger.info("Putting "+tiles.size()+" tiles into TILE for sample id="+sampleEntityId);
        	processData.putItem("TILE", tiles);

        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }
    
}
