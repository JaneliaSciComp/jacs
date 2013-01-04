package org.janelia.it.jacs.compute.service.fileDiscovery;

import java.io.File;
import java.util.Date;
import java.util.List;

import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.compute.service.vaa3d.CombinedFile;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityType;

/**
 * File discovery service for sample crossing results.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ScreenSampleCrossResultsDiscoveryService extends AbstractEntityService {

    protected Date createDate;
	protected Long parentEntityId;
	
	@Override
    public void execute() throws Exception {
        
        String outputParentIdListStr = (String)processData.getItem("OUTPUT_ENTITY_ID_LIST");
        if (outputParentIdListStr==null) {
        	throw new ServiceException("Input parameter OUTPUT_ENTITY_ID_LIST may not be null");
        }
        
        String[] outputParentIds = outputParentIdListStr.split(",");
        
        List<CombinedFile> filePairs = (List<CombinedFile>)processData.getItem("FILE_PAIRS");
        if (filePairs==null) {
        	throw new ServiceException("Input parameter FILE_PAIRS may not be null");
        }
        
        if (outputParentIds.length!=filePairs.size()) {
        	throw new ServiceException("OUTPUT_ENTITY_ID_LIST must contain the same number of ids as the input lists");
        }

        int i = 0;
        for(CombinedFile combinedFile : filePairs) {
        	Long parentId = new Long(outputParentIds[i++]);
	        try {
	            	Entity resultEntity = entityBean.getEntityTree(parentId);
	            	if (resultEntity == null) {
	            		throw new IllegalArgumentException("Result entity not found with id="+parentId);
	            	}
	            	
	                File outputStack = new File(combinedFile.getOutputFilepath());
	                if (!outputStack.exists()) {
	                	throw new ServiceException("Missing output stack: "+outputStack.getAbsolutePath());
	                }
	                
	                File outputMip = new File(combinedFile.getOutputFilepath().replaceAll("v3dpbd", "png"));
	                if (!outputMip.exists()) {
	                	throw new ServiceException("Missing output MIP: "+outputMip.getAbsolutePath());
	                }
	
	                Entity stack = entityHelper.create3dImage(outputStack.getAbsolutePath(), "Intersection Stack");
	                entityBean.addEntityToParent(resultEntity, stack, resultEntity.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
	                
	                // Add default images
	                Entity mip = entityHelper.create2dImage(outputMip.getAbsolutePath(), "Intersection MIP");
	                entityHelper.setDefault2dImage(stack, mip);
	                entityHelper.setDefault3dImage(resultEntity, stack);
	        }
	        catch (Exception e) {
	        	throw new Exception("Error processing cross results for id="+parentId, e);
	        }
        }
    }

    protected Entity createFileEntity(EntityType type, File file, String entityName) throws Exception {
        Entity entity = new Entity();
        entity.setOwnerKey(ownerKey);
        entity.setCreationDate(createDate);
        entity.setUpdatedDate(createDate);
        entity.setEntityType(type);
        entity.setName(entityName);
        entity.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, file.getAbsolutePath());

        if (type.getName().equals(EntityConstants.TYPE_IMAGE_2D) || type.getName().equals(EntityConstants.TYPE_IMAGE_3D)) {
        	String filename = file.getName();
        	String fileFormat = filename.substring(filename.lastIndexOf('.')+1);
        	entity.setValueByAttributeName(EntityConstants.ATTRIBUTE_IMAGE_FORMAT, fileFormat);
        }
        
        entity = entityBean.saveOrUpdateEntity(entity);
        logger.info("Saved "+type.getName()+" as "+entity.getId());
        return entity;
    }
}
