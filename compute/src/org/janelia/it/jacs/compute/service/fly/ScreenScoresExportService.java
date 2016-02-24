package org.janelia.it.jacs.compute.service.fly;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.janelia.it.jacs.compute.access.large.MongoLargeOperations;
import org.janelia.it.jacs.compute.service.fileDiscovery.FileDiscoveryHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * This service exports the annotation scores to a file. It also corrects folder placement while its working. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ScreenScoresExportService extends ScreenScoresLoadingService {

    public void execute() throws Exception {
    	
        createDate = new Date();
        helper = new FileDiscoveryHelper(entityBean, computeBean, ownerKey, logger);
        
        // Process arguments
        
        String outputFilepath = (String)processData.getItem("OUTPUT_FILEPATH");
    	if (outputFilepath == null) {
    		throw new IllegalArgumentException("OUTPUT_FILEPATH may not be null");
    	}
    	
    	File outputFile = new File(outputFilepath);
    	if (!outputFile.getParentFile().canWrite()) {
    		throw new IllegalArgumentException("Cannot write to output file: "+outputFilepath);
    	}
    	
    	logger.info("Caching sample names and masks");
    	
    	MongoLargeOperations largeOp = new MongoLargeOperations();
    	Map<Long,String> sampleNameMap = new HashMap<Long,String>();
    	
    	for(Entity sample : entityBean.getUserEntitiesByTypeName(null, EntityConstants.TYPE_SCREEN_SAMPLE)) {
    		sampleNameMap.put(sample.getId(), sample.getName());
    		Map<Long,String> masks = getSampleMaskImages(sample);      
    		for(Long maskId : masks.keySet()) {
    			largeOp.putValue(MongoLargeOperations.SCREEN_SCORE_MAP, maskId, sample.getName());
    		}
    	}
    	
    	logger.info("    ... got "+sampleNameMap.size()+" sample names");
    	
    	logger.info("Processing folder-by-folder");
    	Map<String,String> evalMap = new HashMap<String,String>();
    	
    	Entity topLevelFolder = populateChildren(getRootEntity(ScreenScoresLoadingService.TOP_LEVEL_EVALUATION_FOLDER));
    	List<Entity> compartments = topLevelFolder.getOrderedChildren();
    	for(Entity compartmentEntity : compartments) {
    		
    		String compartment = compartmentEntity.getName();
    		logger.info("  Processing "+compartment);
    		
    		Map<String,Entity> folderMap = new HashMap<String,Entity>();
    		populateChildren(compartmentEntity);
    		for(Entity intEntity : compartmentEntity.getChildren()) {	
    			int i = getValueFromFolderName(intEntity);
    			populateChildren(intEntity);
    			for(Entity distEntity : intEntity.getChildren()) {	
    				int d = getValueFromFolderName(distEntity);
    				folderMap.put(i+"/"+d, distEntity);
    			}
    		}
    		
    		Map<String,String> currFolderMap = new HashMap<String,String>();
    		Map<String,String> movingMap = new HashMap<String,String>();
    		
    		for(String key : folderMap.keySet()) {
    			String[] parts = key.split("/");
    			int i = Integer.parseInt(parts[0]);
    			int d = Integer.parseInt(parts[1]);
    			Entity distFolder = folderMap.get(key);
    			logger.info("    Processing int="+i+" dist="+d);
    			
    			if (distFolder.getChildren().isEmpty()) {
    				// Skip empty folders
    				continue;
    			}
    			
        		Map<String,List<OntologyAnnotation>> annotMap = new HashMap<String,List<OntologyAnnotation>>();
        		List<Long> entityIds = new ArrayList<Long>();
        		
        		for(Entity annotEntity : annotationBean.getAnnotationsForChildren(ownerKey, distFolder.getId())) {
        			String targetId = annotEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_TARGET_ID);
        			List<OntologyAnnotation> entityAnnots = annotMap.get(targetId);
        			if (entityAnnots==null) {
        				entityAnnots = new ArrayList<OntologyAnnotation>();
        				annotMap.put(targetId, entityAnnots);
        			}
        			OntologyAnnotation annototation = new OntologyAnnotation();
        			annototation.init(annotEntity);
        			entityAnnots.add(annototation);
        			entityIds.add(Long.parseLong(targetId));
        		}
        		
        		if (annotMap.isEmpty()) continue;
        		
        		for(String entityId : annotMap.keySet()) {
        			
        			Long maskId = new Long(entityId);
        			String sampleName = (String)largeOp.getValue(MongoLargeOperations.SCREEN_SCORE_MAP, maskId);
        			logger.info("      Processing maskId="+entityId+" "+sampleName);
        			
					String maaIntensity = null;
					String maaDistribution = null;
					String caIntensity = null;
					String caDistribution = null;
					for(OntologyAnnotation annotation : annotMap.get(entityId)) {
						if (ScreenScoresLoadingService.MAA_INTENSITY_NAME.equals(annotation.getKeyString())) {
							maaIntensity = annotation.getValueString();
						}
						else if (ScreenScoresLoadingService.CA_INTENSITY_NAME.equals(annotation.getKeyString())) {
							caIntensity = annotation.getValueString();
						}
						else if (ScreenScoresLoadingService.MAA_DISTRIBUTION_NAME.equals(annotation.getKeyString())) {
							maaDistribution = annotation.getValueString();
						}
						else if (ScreenScoresLoadingService.CA_DISTRIBUTION_NAME.equals(annotation.getKeyString())) {
							caDistribution = annotation.getValueString();
						}
					}						

					if (maaIntensity==null) {
						logger.warn("        No MAA Intensity for "+entityId);
						for(OntologyAnnotation annotation : annotMap.get(entityId)) {
							logger.warn("            Got "+annotation.getKeyString());
						}
						continue;
					}
					
					if (maaDistribution==null) {
						logger.warn("        No MAA Distribution for "+entityId);
						for(OntologyAnnotation annotation : annotMap.get(entityId)) {
							logger.warn("            Got "+annotation.getKeyString());
						}
						continue;
					}
					
					// The current evaluation
					StringBuffer buf = new StringBuffer();
					int mi = getValueFromAnnotation(maaIntensity);
					int md = getValueFromAnnotation(maaDistribution);
					int fi = mi;
					int fd = md;
					
					buf.append(mi).append("\t");
					buf.append(md).append("\t");
					
					if (!StringUtils.isEmpty(caIntensity)) {
						int ci = getValueFromAnnotation(caIntensity);
						buf.append(ci);
						fi = ci;
					}
					buf.append("\t");
					
					if (!StringUtils.isEmpty(caDistribution)) {
						int cd = getValueFromAnnotation(caDistribution);
						buf.append(cd);
						fd = cd;
					}
					buf.append("\t");
					
					buf.append(fi).append("\t").append(fd);
					
					String evalKey = compartment+"\t"+sampleName;
					evalMap.put(evalKey, buf.toString());
        			
					logger.info("          \""+evalKey+"\" -> i"+fi+"/d"+fd);
        			
					if (fi!=i) {
						logger.warn("      Processing "+compartment+" "+sampleName);
						logger.warn("          Incorrect intensity folder ("+i+" but expecting "+fi+")");
					}
					if (fd!=d) {
						logger.warn("      Processing "+compartment+" "+sampleName);
						logger.warn("          Incorrect distribution folder ("+d+" but expecting "+fd+")");
					}
					
					if (fi!=i || fd!=d) {
						currFolderMap.put(entityId, key);
						movingMap.put(entityId, fi+"/"+fd);	
					}
        		}
    		}
    		
    		if (!movingMap.isEmpty()) {
        		logger.info("          Moving "+movingMap.size()+" images into correct folders");
        		for(String entityId : movingMap.keySet()) {
        			String currKey = currFolderMap.get(entityId);
        			String targetKey = movingMap.get(entityId);
        			Entity currFolder = folderMap.get(currKey);
        			Entity targetFolder = folderMap.get(targetKey);
        			moveToFolder(Long.parseLong(entityId), currFolder, targetFolder);
        		}
    		}
    	}
    		
    	logger.info("Writing output to "+outputFile.getAbsolutePath());
    	
    	FileWriter writer = new FileWriter(outputFile);
    	
    	writer.write("Compartment\tSample\tMAA Intensity\tMAA Distribution\tCA Intensity\tCA Distribution\tIntensity\tDistribution\n");

    	for(Entity compartmentEntity : compartments) {
    		String compartment = compartmentEntity.getName();
    		int c = 0;
    		for(String sampleName : sampleNameMap.values()) {
    			String key  = compartment+"\t"+sampleName;
    			String value = evalMap.get(key);
    			if (value!=null) {
        			writer.write(key+"\t");
            		writer.write((value==null?"":value)+"\n");	
            		c++;
    			}
    		}
    		logger.info("Wrote "+c+" sample lines for "+compartment);
    	}
    	writer.close();
    }

	private void moveToFolder(long entityId, Entity currFolder, Entity targetFolder) throws Exception {
		
			logger.info("        Moving "+entityId+" from "+currFolder.getId()+" to "+targetFolder.getId());
		
			EntityData existing = null;
			for(EntityData ed : new HashSet<EntityData>(targetFolder.getEntityData())) {
				if (ed.getChildEntity()!=null && ed.getChildEntity().getId()==entityId) {
					existing = ed;
				}
			}
			
			if (existing == null) {
				logger.info("          Adding to "+targetFolder.getId());
				
				// Add to new folder
				List<Long> childrenIds = new ArrayList<Long>();
				childrenIds.add(entityId);
				entityBean.addChildren(ownerKey, targetFolder.getId(), childrenIds, EntityConstants.ATTRIBUTE_ENTITY);
				
			}
			else {
				logger.info("          Already exists in "+targetFolder.getId());
			}
			
			// Remove from old folder
			EntityData toDelete = null;
			for(EntityData ed : new HashSet<EntityData>(currFolder.getEntityData())) {
				if (ed.getChildEntity()!=null && ed.getChildEntity().getId()==entityId) {
					toDelete = ed;
				}
			}
			
			if (toDelete!=null) {
				logger.info("          Deleting from "+currFolder.getId());
				entityBean.deleteEntityData(toDelete);
			}
			else {
				logger.info("          Already gone from "+currFolder.getId());
			}
			
	}
    
    public Entity getRootEntity(String topLevelFolderName) throws Exception {
        Set<Entity> topLevelFolders = entityBean.getEntitiesByName(topLevelFolderName);
        Entity topLevelFolder = null;
        if (topLevelFolders != null) {
            // Only accept the current user's top level folder
            for (Entity entity : topLevelFolders) {
                if (entity.getOwnerKey().equals(ownerKey)
                        && entity.getEntityTypeName().equals(EntityConstants.TYPE_FOLDER)
                        && entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_COMMON_ROOT) != null) {
                    topLevelFolder = entity;
                    logger.info("Found existing topLevelFolder common root, name=" + topLevelFolder.getName());
                    break;
                }
            }
        }
        return topLevelFolder;
    }
}
