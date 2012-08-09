package org.janelia.it.jacs.compute.service.fly;

import java.io.File;
import java.util.*;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.large.LargeOperations;
import org.janelia.it.jacs.compute.api.AnnotationBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.fileDiscovery.FileDiscoveryHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * This service walks through the evaluation folder hierarchy and removes samples which Arnim has deemed unsuited for
 * annotation.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ScreenScoresLoadingService3 implements IService {
	
	private static final boolean DEBUG = false;
	
    protected Logger logger;
    protected Task task;
    protected User user;
    protected Date createDate;
    protected EntityBeanLocal entityBean;
    protected ComputeBeanLocal computeBean;
    protected AnnotationBeanLocal annotationBean;
	protected FileDiscoveryHelper helper;
    
    private Set<String> rejects = new HashSet<String>();
    	
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
            
            // Process arguments
            
            String rejectsFile = (String)processData.getItem("REJECTS_FILE_PATH");
        	if (rejectsFile == null) {
        		throw new IllegalArgumentException("REJECTS_FILE_PATH may not be null");
        	}
        	
        	readRejects(new File(rejectsFile));
        	
        	// Precache mask image parent relationships
        	
        	logger.info("Precaching mask parents...");
        	
        	LargeOperations largeOp = new LargeOperations();
        	largeOp.clearCache(LargeOperations.SCREEN_SCORE_MAP);

        	for(Entity sample : entityBean.getEntitiesByTypeName(EntityConstants.TYPE_SCREEN_SAMPLE)) {

        		populateChildren(sample);
        		Entity patternAnnotation = EntityUtils.findChildWithName(sample, "Pattern Annotation");
        		if (patternAnnotation==null) {
        			continue;
        		}
        		
        		Specimen specimen = Specimen.createSpecimenFromFullName(sample.getName());
        		
        		Map<Long,String> maskMap = entityBean.getChildEntityNames(patternAnnotation.getId());        		
        		logger.info("  Precaching "+maskMap.size()+" children for "+specimen.getSpecimenName());
        		
        		for(Map.Entry<Long, String> entry : maskMap.entrySet()) {
        			largeOp.putValue(LargeOperations.SCREEN_SCORE_MAP, entry.getKey(), specimen.getSpecimenName());
        		}
        	}
        	
        	Entity topLevelFolder = helper.getRootEntity(ScreenScoresLoadingService.TOP_LEVEL_EVALUATION_FOLDER, false);
        	populateChildren(topLevelFolder);

        	logger.info("Will clean "+rejects.size()+" rejected specimens from "+topLevelFolder.getName());
        	
        	for(Entity compartment : topLevelFolder.getOrderedChildren()) {
        		populateChildren(compartment);
        		logger.info("Processing "+compartment.getName());
        		
            	for(Entity intFolder : compartment.getOrderedChildren()) {
            		populateChildren(intFolder);
            		logger.info("  "+intFolder.getName());
            		
                	for(Entity distFolder : intFolder.getOrderedChildren()) {
                		populateChildren(distFolder);
                		logger.info("    "+distFolder.getName());
                		
                		List<EntityData> toDelete = new ArrayList<EntityData>();
                    	for(EntityData maskImageEd : distFolder.getEntityData()) {
                    		if (maskImageEd.getChildEntity()!=null) {
                    			String specimenName = (String)largeOp.getValue(LargeOperations.SCREEN_SCORE_MAP, maskImageEd.getChildEntity().getId());
                    			if (rejects.contains(specimenName)) {
                    				toDelete.add(maskImageEd);
                    			}
                    		}
                    	}
                    	
                    	logger.info("      Deleting "+toDelete.size()+" mask images");
                    	for(EntityData ed : toDelete) {
                    		if (!DEBUG) entityBean.deleteEntityData(ed);
                    	}
                	}
            	}
        	}
        	
        	logger.info("Done cleaning rejects from "+topLevelFolder.getName());

        } 
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    private void readRejects(File rejectsFile) throws Exception {
		Scanner scanner = new Scanner(rejectsFile);
        try {
            while (scanner.hasNextLine()){
                String specimen = scanner.nextLine();
                rejects.add(specimen);
            }
        }
        finally {
        	scanner.close();
        }
    }

    private Entity populateChildren(Entity entity) {
    	if (entity==null || EntityUtils.areLoaded(entity.getEntityData())) return entity;
		EntityUtils.replaceChildNodes(entity, entityBean.getChildEntities(entity.getId()));
		return entity;
    }
}
