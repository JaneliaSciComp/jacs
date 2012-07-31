package org.janelia.it.jacs.compute.service.fly;

import java.io.File;
import java.io.FileWriter;
import java.util.*;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.api.support.MappedId;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * This service exports the annotation scores to a file. It also corrects folder placement while its working. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ScreenScoresExportService implements IService {

    protected Logger logger;
    protected Task task;
    protected User user;
    protected Date createDate;
    protected EntityBeanLocal entityBean;
    protected ComputeBeanLocal computeBean;
    protected AnnotationBeanLocal annotationBean;

	
    public void execute(IProcessData processData) throws ServiceException {
    	
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            task = ProcessDataHelper.getTask(processData);
            entityBean = EJBFactory.getLocalEntityBean();
            computeBean = EJBFactory.getLocalComputeBean();
            annotationBean = EJBFactory.getLocalAnnotationBean();
            user = computeBean.getUserByName(ProcessDataHelper.getTask(processData).getOwner());
            
            // Process arguments
            
            String topLevelFolderName = (String)processData.getItem("TOP_LEVEL_FOLDER_NAME");
        	if (topLevelFolderName == null) {
        		throw new IllegalArgumentException("TOP_LEVEL_FOLDER_NAME may not be null");
        	}
            String outputFilepath = (String)processData.getItem("OUTPUT_FILEPATH");
        	if (outputFilepath == null) {
        		throw new IllegalArgumentException("OUTPUT_FILEPATH may not be null");
        	}
        	
        	File outputFile = new File(outputFilepath);
        	if (!outputFile.getParentFile().canWrite()) {
        		throw new IllegalArgumentException("Cannot write to output file: "+outputFilepath);
        	}
        	
        	logger.info("Caching sample names");
        	
        	Map<Long,String> sampleNameMap = new HashMap<Long,String>();
        	for(Entity sample : entityBean.getUserEntitiesByTypeName("system", EntityConstants.TYPE_SCREEN_SAMPLE)) {
        		sampleNameMap.put(sample.getId(), sample.getName());
        	}
        	
        	
        	logger.info("    ... got "+sampleNameMap.size()+" sample names");
        	
        	logger.info("Processing folder-by-folder");
        	Map<String,String> evalMap = new HashMap<String,String>();
        	
        	Entity topLevelFolder = populateChildren(getRootEntity(topLevelFolderName));
        	List<Entity> compartments = topLevelFolder.getOrderedChildren();
        	for(Entity compartmentEntity : compartments) {
        		
        		String compartment = compartmentEntity.getName();
        		logger.info("    Processing "+compartment);
        		
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
        			
        			if (distFolder.getChildren().isEmpty()) {
        				// Skip empty folders
        				continue;
        			}
        			
            		Map<String,List<OntologyAnnotation>> annotMap = new HashMap<String,List<OntologyAnnotation>>();
            		List<Long> entityIds = new ArrayList<Long>();
            		
            		for(Entity annotEntity : annotationBean.getAnnotationsForChildren(user.getUserLogin(), distFolder.getId())) {
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
            		
            		List<String> upMapping = new ArrayList<String>();
            		upMapping.add(EntityConstants.TYPE_FOLDER);
            		upMapping.add(EntityConstants.TYPE_SCREEN_SAMPLE);
            		List<String> downMapping = new ArrayList<String>();
            		List<MappedId> mappedIds = entityBean.getProjectedResults(entityIds, upMapping, downMapping);
            		List<Long> sampleIds = new ArrayList<Long>();
            		for(MappedId mappedId : mappedIds) {
            			sampleIds.add(mappedId.getMappedId());
            		}
            		
            		Map<String,String> maskNameMap = new HashMap<String,String>();
            		
            		for(MappedId mappedId : mappedIds) {
            			String sampleName = sampleNameMap.get(mappedId.getMappedId());
            			if (sampleName==null) {
            				logger.warn("Could not find sample with id="+mappedId.getMappedId());
            				continue;
            			}
            			maskNameMap.put(mappedId.getOriginalId()+"", sampleName);
            		}
            		
            		for(String entityId : annotMap.keySet()) {
            			
            			String sampleName = maskNameMap.get(entityId);
//            			logger.warn("    Processing "+compartment+" "+sampleName);
            			
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
							if (ScreenScoresLoadingService.MAA_DISTRIBUTION_NAME.equals(annotation.getKeyString())) {
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
							logger.warn("No MAA Distribution for "+entityId);
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
						
						evalMap.put(compartment+"\t"+sampleName, buf.toString());
						
						if (fi!=i) {
							logger.warn("    Processing "+compartment+" "+sampleName);
							logger.warn("        Incorrect intensity folder ("+i+" but expecting "+fi+")");
						}
						if (fd!=d) {
							logger.warn("    Processing "+compartment+" "+sampleName);
							logger.warn("        Incorrect distribution folder ("+d+" but expecting "+fd+")");
						}
						
						if (fi!=i || fd!=d) {
							currFolderMap.put(entityId, key);
							movingMap.put(entityId, fi+"/"+fd);	
						}
            		}
        		}
        		
        		if (!movingMap.isEmpty()) {
	        		logger.info("        Moving "+movingMap.size()+" images into correct folders");
	        		for(String entityId : movingMap.keySet()) {
	        			String currKey = currFolderMap.get(entityId);
	        			String targetKey = movingMap.get(entityId);
	        			Entity currFolder = folderMap.get(currKey);
	        			Entity targetFolder = folderMap.get(targetKey);
	        			moveToFolder(Long.parseLong(entityId), currFolder, targetFolder);
	        		}
        		}
        	}
        	
        	logger.info("Verifying that we have all the samples");
        	
        	for(Entity compartmentEntity : compartments) {
        		String compartment = compartmentEntity.getName();
        		for(String sampleName : sampleNameMap.values()) {	
        			if (!evalMap.containsKey(compartment+"\t"+sampleName)) {
        				logger.warn("Missing "+compartment+" for "+sampleName);
        			}
        		}
        	}
        		
        	logger.info("Writing output to "+outputFile.getAbsolutePath());
        	
        	FileWriter writer = new FileWriter(outputFile);
        	
        	writer.write("Compartment\tSample\tMAA Intensity\tMAA Distribution\tCA Intensity\tCA Distribution\tIntensity\tDistribution\n");
        	
        	for(String key : evalMap.keySet()) {
        		writer.write(key+"\t");
        		writer.write(evalMap.get(key)+"\n");
        	}
        	writer.close();
        } 
        catch (Exception e) {
            throw new ServiceException(e);
        }
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
				entityBean.addChildren(user.getUserLogin(), targetFolder.getId(), childrenIds, EntityConstants.ATTRIBUTE_ENTITY);
				
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
    
    private Entity populateChildren(Entity entity) {
    	if (entity==null || EntityUtils.areLoaded(entity.getEntityData())) return entity;
		EntityUtils.replaceChildNodes(entity, entityBean.getChildEntities(entity.getId()));
		return entity;
    }
    
    public Entity getRootEntity(String topLevelFolderName) throws Exception {
        Set<Entity> topLevelFolders = entityBean.getEntitiesByName(topLevelFolderName);
        Entity topLevelFolder = null;
        if (topLevelFolders != null) {
            // Only accept the current user's top level folder
            for (Entity entity : topLevelFolders) {
                if (entity.getUser().getUserLogin().equals(user.getUserLogin())
                        && entity.getEntityType().getName().equals(EntityConstants.TYPE_FOLDER)
                        && entity.getAttributeByName(EntityConstants.ATTRIBUTE_COMMON_ROOT) != null) {
                    topLevelFolder = entity;
                    logger.info("Found existing topLevelFolder common root, name=" + topLevelFolder.getName());
                    break;
                }
            }
        }
        return topLevelFolder;
    }

	private int getValueFromAnnotation(String annotationValue) {
		return Integer.parseInt(""+annotationValue.charAt(1));
	}
	
	private int getValueFromFolderName(Entity entity) {
		return getValueFromFolderName(entity.getName());
	}
	
	private int getValueFromFolderName(String folderName) {
		return Integer.parseInt(""+folderName.charAt(folderName.length()-1));
	}
}
