package org.janelia.it.jacs.compute.service.fly;

import java.io.File;
import java.util.*;

import org.janelia.it.jacs.compute.access.large.LargeOperations;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.fileDiscovery.FileDiscoveryHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * This service loads the ArnimUpdate1 masks into the evaluation folder hierarchy, along with their MA annotations. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ScreenScoresLoadingService2 extends ScreenScoresLoadingService {
	
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
            
            String acceptsFilepath = (String)processData.getItem("ACCEPTS_FILE_PATH");
        	if (acceptsFilepath == null) {
        		throw new IllegalArgumentException("ACCEPTS_FILE_PATH may not be null");
        	}
        	Set<String> accepted = readNameFile(new File(acceptsFilepath));
        	
        	// Preload entity types
        	
        	folderType = entityBean.getEntityTypeByName(EntityConstants.TYPE_FOLDER);
        	
        	// Get top level folder
        	
        	String topLevelFolderName = TOP_LEVEL_EVALUATION_FOLDER;
        	Entity topLevelFolder = populateChildren(helper.getRootEntity(topLevelFolderName, false));

            // Clear the cache 
        	
        	LargeOperations largeOp = new LargeOperations();
            largeOp.clearCache(LargeOperations.SCREEN_SCORE_MAP);
        	
        	// Process each screen sample and save off its expression scores for later use
        	
        	for(Entity sample : entityBean.getEntitiesByTypeName(EntityConstants.TYPE_SCREEN_SAMPLE)) {

        		Specimen specimen = Specimen.createSpecimenFromFullName(sample.getName());
        		if (!accepted.contains(specimen.getSpecimenName())) continue;
        		
        		logger.info("Processing "+sample);
        		
        		// First we read the scores from the score file
        		
        		populateChildren(sample);
        		Entity maskAnnotation = EntityUtils.findChildWithName(sample, "Mask Annotation");
        		if (maskAnnotation==null) {
        			logger.warn("Sample "+sample.getName()+" has no Mask Annotation folder");
        			continue;
        		}

        		populateChildren(maskAnnotation);
        		Entity updateFolder = EntityUtils.findChildWithName(maskAnnotation, "ArnimUpdate1");
        		if (updateFolder==null) {
        			logger.warn("Sample "+sample.getName()+" has no ArnimUpdate1 folder");
        			continue;
        		}

        		populateChildren(updateFolder);
        		Entity suppFiles = EntityUtils.findChildWithName(updateFolder, "supportingFiles");
        		if (suppFiles==null) {
        			logger.warn("Sample "+sample.getName()+" has no supportingFiles folder");
        			continue;
        		}

        		populateChildren(suppFiles);
        		Entity scoreFile = EntityUtils.findChildWithName(suppFiles, "arnimScores.txt");
        		if (scoreFile==null) {
        			logger.warn("Sample "+sample.getName()+" has no arnimScores.txt");
        			continue;
        		}
        		
        		String scoreFilepath = scoreFile.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        		Map<String,Score> scores = readScoresFile(new File(scoreFilepath));
        		
        		numSamples++;
        		if (scores == null) {
        			logger.info("  missing data");
        			numSamplesMissingData++;
        			continue;
        		}
        		
        		// We need to get all the individual mask images for the sample. This child set might contain extra 
        		// stuff we don't care about, but it will get filtered by the score map in the loop below
        		Map<Long,String> maskMap = entityBean.getChildEntityNames(updateFolder.getId());        		
        		for(Map.Entry<Long, String> entry : maskMap.entrySet()) {
        			Score score = scores.get(entry.getValue());
        			if (score==null) continue;
        			score.maskId = entry.getKey();
        		}
        		
        		// Now go through the scores for this sample, and hash them into the disk-based map for later use
        		for(String compartment : scores.keySet()) {
        			Score score = scores.get(compartment);
        			if (score.maskId==null) continue;
        			String key = compartment+"/"+score.intensity+"/"+score.distribution;
        			List<Long> sampleCompIds = (List<Long>)largeOp.getValue(LargeOperations.SCREEN_SCORE_MAP, key);
        			if (sampleCompIds==null) {
        				sampleCompIds = new ArrayList<Long>();
        			}
        			sampleCompIds.add(score.maskId);
        			largeOp.putValue(LargeOperations.SCREEN_SCORE_MAP,key,sampleCompIds);
        		}
        		
        		logger.info("  processed "+scores.size()+" compartments");
        	}
        	
        	// Get score ontology
        	getOrCreateOntology();
    		
        	// Create the folder structure and annotate each sample
        	
        	logger.info("Adding to folder structure under "+topLevelFolderName);

        	for(final String compartment : compartments) {
        		logger.info("Processing compartment "+compartment);
        		Entity compartmentFolder = helper.createOrVerifyFolderEntity(topLevelFolder, compartment);

            	for(int i=MAX_SCORE; i>=0; i--) {
            		logger.info("  Processing intensity "+i);
            		Entity intValueTerm = intValueItems.get(i);
            		Entity intValueFolder = helper.createOrVerifyFolderEntity(compartmentFolder, "Intensity "+i);
            		
                	for(int d=MAX_SCORE; d>=0; d--) {
                		logger.info("  Processing distribution "+d);	
                		Entity distValueTerm = distValueItems.get(d);
	            		Entity distValueFolder = helper.createOrVerifyFolderEntity(intValueFolder, "Distribution "+d);
	            		
	            		String key = compartment+"/"+i+"/"+d;
	            		List<Long> sampleCompIds = (List<Long>)largeOp.getValue(LargeOperations.SCREEN_SCORE_MAP, key);
	            		if (sampleCompIds!=null) {
		            		logger.info("    Sample count: "+sampleCompIds.size());
		            		
		            		entityBean.addChildren(user.getUserLogin(), distValueFolder.getId(), sampleCompIds, EntityConstants.ATTRIBUTE_ENTITY);
		            		for(Long sampleCompId : sampleCompIds) {
		            			annotate(sampleCompId, maaIntensityEnum, intValueTerm);
		            			annotate(sampleCompId, maaDistributionEnum, distValueTerm);
		            		}
	            		}
                	}
            	}
        	}
        	
        	logger.info("Processed "+numSamples+" samples, of which "+numSamplesMissingData+" were missing data and thus ignored. "+numAnnotationsCreated+" annotations were created.");
        } 
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }
}
