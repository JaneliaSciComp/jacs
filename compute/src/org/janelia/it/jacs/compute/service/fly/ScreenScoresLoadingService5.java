package org.janelia.it.jacs.compute.service.fly;

import java.util.*;

import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.support.MappedId;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.fileDiscovery.FileDiscoveryHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * This service loads split lines into a hierarchy
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ScreenScoresLoadingService5 extends ScreenScoresLoadingService {
	
	public static final String TOP_LEVEL_SPLIT_FOLDER = "FlyLight Screen Split Lines";
	
    public void execute(IProcessData processData) throws ServiceException {
    	
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            task = ProcessDataHelper.getTask(processData);
            entityBean = EJBFactory.getLocalEntityBean();
            computeBean = EJBFactory.getLocalComputeBean();
            annotationBean = EJBFactory.getLocalAnnotationBean();
            user = computeBean.getUserByName(ProcessDataHelper.getTask(processData).getOwner());
            createDate = new Date();
            helper = new FileDiscoveryHelper(entityBean, computeBean, user);
            folderType = entityBean.getEntityTypeByName(EntityConstants.TYPE_FOLDER);
            
            Set<String> acceptedCompartments = new HashSet<String>();
            
            String compartmentList = (String)processData.getItem("COMPARTMENTS");
            if (compartmentList!=null) {
            	for(String c : compartmentList.split(",")) {
            		acceptedCompartments.add(c);		
            	}
            }
            
        	Entity topLevelSplitFolder = populateChildren(createOrVerifyRootEntity(TOP_LEVEL_SPLIT_FOLDER, user, createDate, true, false));
        	Entity topLevelEvalFolder = populateChildren(createOrVerifyRootEntity(TOP_LEVEL_EVALUATION_FOLDER, user, createDate, false, false));
        	
        	for(Entity compartmentFolder : topLevelEvalFolder.getOrderedChildren()) {
        		
        		String compartment = compartmentFolder.getName();
        		if (!acceptedCompartments.isEmpty() && !acceptedCompartments.contains(compartment)) continue;
        		
        		Entity splitCompFolder = verifyOrCreateChildFolder(topLevelSplitFolder, compartment);
        		populateChildren(splitCompFolder);
            	if (!splitCompFolder.getChildren().isEmpty()) {
            		logger.warn("Cannot reuse existing split compartment folder for "+splitCompFolder.getName()+", id="+splitCompFolder.getId());
            		continue;
            	}
            	
        		logger.info("Processing "+compartment);
        		populateChildren(compartmentFolder);
        		for(Entity intFolder : compartmentFolder.getOrderedChildren()) {
            		Entity intSplitFolder = null;
            		
            		logger.info("  Processing "+intFolder.getName());
            		populateChildren(intFolder);
            		for(Entity distFolder : intFolder.getOrderedChildren()) {
            			
            			logger.info("    Processing "+distFolder.getName());
            			
            			Map<Long,String> masks = entityBean.getChildEntityNames(distFolder.getId());
            			if (masks.isEmpty()) continue;
            			
            			logger.info("      Found "+masks.size()+" masks");
            			
            			List<Long> lineIds = new ArrayList<Long>();
            			List<Long> maskIds = new ArrayList<Long>(masks.keySet());
            			List<String> upMapping = new ArrayList<String>();
            			List<String> downMapping = new ArrayList<String>();
            			
            			upMapping.add(EntityConstants.TYPE_FOLDER); 
            			upMapping.add(EntityConstants.TYPE_SCREEN_SAMPLE); 
            			for(MappedId mappedId : entityBean.getProjectedResults(maskIds, upMapping, downMapping)) {
            				lineIds.add(mappedId.getMappedId());
            			}
            			
            			upMapping.add(0,EntityConstants.TYPE_FOLDER); // go up one more folder for Mask Annotation
            			for(MappedId mappedId : entityBean.getProjectedResults(maskIds, upMapping, downMapping)) {
            				lineIds.add(mappedId.getMappedId());
            			}
            			
            			logger.info("      Projected to "+lineIds.size()+" screen samples");
            			
        		    	List<Long> splitRepIds = new ArrayList<Long>();
        		    	for(Long screenSampleId : lineIds) {
        		    		Set<Long> parentIds = entityBean.getParentIdsForAttribute(screenSampleId, EntityConstants.ATTRIBUTE_REPRESENTATIVE_SAMPLE);
        		    		if (!parentIds.isEmpty()) {
        		    			splitRepIds.addAll(parentIds);
        		    		}
        		    	}
        		    	
        		    	logger.info("      Reduced to "+splitRepIds.size()+" represented fly lines");
        		    	
        		    	List<Long> splitLineIds = new ArrayList<Long>();
        		    	for(Entity line : entityBean.getEntitiesById(splitRepIds)) {
        		    		if (line.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_SPLIT_PART)!=null) {
        		    			splitLineIds.add(line.getId());
        		    		}
        		    	}
        		    	
        		    	logger.info("      Reduced to "+splitLineIds.size()+" split fly lines");
        		    	
        		    	if (!splitLineIds.isEmpty()) {
	        		    	if (intSplitFolder==null) {
	        		    		intSplitFolder = verifyOrCreateChildFolder(splitCompFolder, intFolder.getName());
	        		    	}
	        		    	Entity distSplitFolder = verifyOrCreateChildFolder(intSplitFolder, distFolder.getName());
	        		    	entityBean.addChildren(user.getUserLogin(), distSplitFolder.getId(), splitLineIds, EntityConstants.ATTRIBUTE_ENTITY);
        		    	}
            		}
        		}
        	}
        } 
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }
}
