package org.janelia.it.jacs.compute.service.fly;

import java.io.File;
import java.io.FileWriter;
import java.util.*;

import org.janelia.it.jacs.compute.access.large.LargeOperations;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.fileDiscovery.FileDiscoveryHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * This service walks through the evaluation folder hierarchy and removes samples which Arnim has deemed unsuited for
 * annotation.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ScreenScoresLoadingService3 extends ScreenScoresLoadingService2 {
	
    private Set<String> rejects = new HashSet<String>();
    private SortedSet<String> accepted = new TreeSet<String>();
    
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

            String outputFilepath = (String)processData.getItem("ACCEPTS_FILE_PATH");
        	if (outputFilepath == null) {
        		throw new IllegalArgumentException("ACCEPTS_FILE_PATH may not be null");
        	}
        	
        	File outputFile = new File(outputFilepath);
        	if (!outputFile.getParentFile().canWrite()) {
        		throw new IllegalArgumentException("Cannot write to output file: "+outputFilepath);
        	}
        	
        	readRejects(new File(rejectsFile));
        	
        	// Precache mask image parent relationships
        	
        	logger.info("Precaching mask parents...");
        	
        	LargeOperations largeOp = new LargeOperations();
        	largeOp.clearCache(LargeOperations.SCREEN_SCORE_MAP);

        	for(Entity sample : entityBean.getEntitiesByTypeName(EntityConstants.TYPE_SCREEN_SAMPLE)) {
        		String specimenName = Specimen.createSpecimenFromFullName(sample.getName()).getSpecimenName();
        		// Get all mask images
        		Map<Long,String> masks = getSampleMaskImages(sample);
        		     		
        		logger.info("  Precaching "+masks.size()+" potential masks for "+specimenName);
        		for(Long maskId : masks.keySet()) {
        			largeOp.putValue(LargeOperations.SCREEN_SCORE_MAP, maskId, specimenName);
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
                    			if (specimenName!=null) {
                        			if (rejects.contains(specimenName)) {
                        				toDelete.add(maskImageEd);
                        			}
                        			else {
                        				accepted.add(specimenName);
                        			}
                    			}
                    		}
                    	}
                    	
                    	logger.info("      Deleting "+toDelete.size()+" mask images");
                    	for(EntityData ed : toDelete) {
                    		entityBean.deleteEntityData(ed);
                    	}
                	}
            	}
        	}
        	
        	logger.info("Done cleaning rejects from "+topLevelFolder.getName());

        	logger.info("Writing output to "+outputFile.getAbsolutePath());
        	
        	FileWriter writer = new FileWriter(outputFile);
        	
        	for(String name : accepted) {
        		writer.write(name+"\n");
        	}
        	writer.close();
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

    protected Map<Long,String> getSampleMaskImages(Entity sample) {

    	Map<Long,String> childNames = new HashMap<Long,String>();

		populateChildren(sample);
		Entity patternAnnotation = EntityUtils.findChildWithName(sample, "Pattern Annotation");
		if (patternAnnotation!=null) {
			childNames.putAll(entityBean.getChildEntityNames(patternAnnotation.getId()));
		}
		
		populateChildren(sample);
		Entity maskAnnotation = EntityUtils.findChildWithName(sample, "Mask Annotation");
		if (maskAnnotation!=null) {
			populateChildren(maskAnnotation);
			for(Entity updateFolder : maskAnnotation.getChildren()) {
				if (updateFolder.getName().startsWith("ArnimUpdate")) {
					childNames.putAll(entityBean.getChildEntityNames(updateFolder.getId()));
				}
			}
		}
		
		return childNames;
    }
}
