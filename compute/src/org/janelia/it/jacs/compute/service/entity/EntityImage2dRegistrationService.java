package org.janelia.it.jacs.compute.service.entity;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.user_data.User;

/**
 * Start at some entity and look for 2d images corresponding to 3d images and set them as attributes.
 * 
 * Parameters:
 *   ROOT_ENTITY_ID - the id of the root entity to start with
 *   DEFAULT_IMAGE_FILENAME - the file to use as the Default 2D Image for the root entity
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityImage2dRegistrationService implements IService {

    protected Logger logger;
    protected EntityBeanLocal entityBean;
    protected ComputeBeanLocal computeBean;
    protected User user;
	protected EntityHelper entityHelper;
	
	protected String defaultImageFilename;
	
	private List<Entity> images2d = new ArrayList<Entity>();
	private List<Entity> images3d = new ArrayList<Entity>();
	private Map<String,Entity> signalMipPrefixMap = new HashMap<String,Entity>();
	private Map<String,Entity> refMipPrefixMap = new HashMap<String,Entity>();
	
	public void execute(IProcessData processData) throws ServiceException {
        try {

	        logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
	        entityBean = EJBFactory.getLocalEntityBean();
	        computeBean = EJBFactory.getLocalComputeBean();
	        user = computeBean.getUserByName(ProcessDataHelper.getTask(processData).getOwner());
	        entityHelper = new EntityHelper(entityBean, computeBean, user);
	        
	        defaultImageFilename = (String)processData.getItem("DEFAULT_IMAGE_FILENAME");
        	if (defaultImageFilename == null || "".equals(defaultImageFilename)) {
        		throw new IllegalArgumentException("DEFAULT_IMAGE_FILENAME may not be null");
        	}
        	
        	String rootEntityId = (String)processData.getItem("ROOT_ENTITY_ID");
        	if (rootEntityId == null || "".equals(rootEntityId)) {
        		throw new IllegalArgumentException("ROOT_ENTITY_ID may not be null");
        	}
        	
        	Entity entity = entityBean.getEntityTree(new Long(rootEntityId));
        	if (entity == null) {
        		throw new IllegalArgumentException("Entity not found with id="+rootEntityId);
        	}
        	
        	logger.info("Finding images under "+entity.getName());
        	logger.info("Looking for default image: "+defaultImageFilename);
        	
        	findImages(entity);
        	
        	logger.info("Found "+images3d.size()+" 3d images and "+images2d.size()+" 2d images");
        	
        	Entity parentSignalMip = null;
        	Entity parentRefMIP = null;
        	
        	for(Entity image3d : images3d) {
    			String filepath = image3d.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
    			Pattern p = Pattern.compile("^(.*?)\\.(\\w+?)$");
    			Matcher m = p.matcher(filepath);
    			
    			if (m.matches()) {
    				String prefix = m.group(1);
    				
    				Entity signalMip = signalMipPrefixMap.get(prefix);
    				Entity refMip = refMipPrefixMap.get(prefix);

    				if (signalMip!=null) {
    					entityHelper.setImage(image3d, EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE, signalMip);
    					entityHelper.setImage(image3d, EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE, signalMip);
    					if (filepath.equals(defaultImageFilename)) {
    						parentSignalMip = signalMip;
    					}
    				}
    				
    				if (refMip!=null) {
    					entityHelper.setImage(image3d, EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE, refMip);
    					if (filepath.equals(defaultImageFilename)) {
    						parentRefMIP = refMip;
    					}
    				}
    			}	
        	}
        	
        	if (parentSignalMip!=null) {
            	entityHelper.setImage(entity, EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE, parentSignalMip);	
        	}
        	
        	Entity sample = null;
        	Set<Entity> parents = entityBean.getParentEntities(entity.getId());
        	if (parents != null) {
        		for(Entity parent : parents) {
        			if (parent.getEntityType().getName().equals(EntityConstants.TYPE_SAMPLE)) {
        				if (sample!=null) {
        					logger.error("Detected multiple parent samples for result "+entity.getName()+" (id="+entity.getId()+")");
        				}
        				sample = parent;
        			}
        		}
        		
        		if (sample != null) {
        			if (parentSignalMip!=null) {
	        			entityHelper.setImage(sample, EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE, parentSignalMip);
	        			entityHelper.setDefault2dImage(sample, parentSignalMip);
        			}
        			if (parentRefMIP!=null) {
        				entityHelper.setImage(sample, EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE, parentRefMIP);
        			}
        		}
        	}
            
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }
	
	private void findImages(Entity entity) {
		
		String entityType = entity.getEntityType().getName();
		if (entityType.equals(EntityConstants.TYPE_IMAGE_2D)) {
			String filepath = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
			Pattern p = Pattern.compile("^(.*?)_(\\w+)\\.(\\w+?)$");
			Matcher m = p.matcher(filepath);
			
			if (m.matches()) {
				String prefix = m.group(1);
				String type = m.group(2);
				
				if ("signal".equals(type)) {
					signalMipPrefixMap.put(prefix, entity);
				}
				else if ("reference".equals(type)) {
					refMipPrefixMap.put(prefix, entity);
				}
			}
			
			images2d.add(entity);
		}
		else if (entityType.equals(EntityConstants.TYPE_IMAGE_3D)) {
			images3d.add(entity);
		}
		else {
			for(Entity child : entity.getChildren()) {
				findImages(child);
			}
		}
	}
	
}
