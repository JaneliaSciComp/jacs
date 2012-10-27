package org.janelia.it.jacs.compute.service.entity;

import java.io.File;
import java.util.*;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.service.entity.sample.ResultImageRegistrationService;
import org.janelia.it.jacs.compute.util.FileUtils;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityAttribute;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.cv.PipelineProcess;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.jacs.shared.utils.entity.EntityVisitor;
import org.janelia.it.jacs.shared.utils.entity.EntityVistationBuilder;

/**
 * Upgrade the model to use the most current entity structure.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MCFODataUpgradeService extends AbstractEntityService {

	private static final Logger logger = Logger.getLogger(MCFODataUpgradeService.class);

	public transient static final String CENTRAL_DIR_PROP = "FileStore.CentralDir";
	
    public transient static final String PARAM_testRun = "is test run";
	
    protected int numSamples;
    protected int numChanges;
    
    private Set<Long> visited = new HashSet<Long>();
    private boolean isDebug = false;
    private String username;
    private File userFilestore;
    private ResultImageRegistrationService resultImageRegService;
    
    public void execute() throws Exception {
            
    	this.username = user.getUserLogin();
        this.userFilestore = new File(SystemConfigurationProperties.getString(CENTRAL_DIR_PROP),username);
        
        final String serverVersion = computeBean.getAppVersion();
        logger.info("Updating data model to latest version: "+serverVersion);
        
        if (isDebug) {
        	logger.info("This is a test run. No entities will be moved or deleted.");
        }
        else {
        	logger.info("This is the real thing. Entities will be moved and/or deleted!");
        }
        
        logger.info("Adding data sets...");
        if ("asoy".equals(username)) {
        	createDataSet("MB Flp-out 63X", PipelineProcess.YoshiMB63xFlpout, true);
        	createDataSet("MB LexA-GAL4 63x", PipelineProcess.YoshiMB63xLexAGal4, true);
        }
        else if ("leetlab".equals(username)) {
        	createDataSet("Pan Lineage 40x", PipelineProcess.LeetWholeBrain40x, true);
        	createDataSet("Central Brain 63x", PipelineProcess.LeetCentralBrain63x, true);
        }
        else if ("wolfft".equals(username)) {
        	createDataSet("Central Complex Tanya", PipelineProcess.FlyLightUnaligned, false);
        }
        
        logger.info("Processing samples...");
        processSamples();
		logger.info("Processed "+numSamples+" samples.");
    }
    
    private void createDataSet(String dataSetName, PipelineProcess process, boolean sageSync) throws ComputeException {

    	if (annotationBean.getUserDataSetByName(username, dataSetName)!=null) {
    		logger.info("Data set already exists: "+dataSetName);
    		return;
    	}
    	
		logger.info("Creating new data set: "+dataSetName);
		if (!isDebug) {
			Entity dataSet = annotationBean.createDataSet(username, dataSetName);
			dataSet.setValueByAttributeName(EntityConstants.ATTRIBUTE_PIPELINE_PROCESS, process==null?"":process.toString());
			if (sageSync) {
				dataSet.setValueByAttributeName(EntityConstants.ATTRIBUTE_SAGE_SYNC, EntityConstants.ATTRIBUTE_SAGE_SYNC);
			}
			dataSet = entityBean.saveOrUpdateEntity(dataSet);
		}
    }

	public void processSamples() throws Exception {
    	List<Entity> samples = entityBean.getUserEntitiesByTypeName(username, EntityConstants.TYPE_SAMPLE);
        for(Entity sample : samples) {
            processSample(sample);
        }
    }
    
    public void processSample(Entity sample) throws Exception {
    	
    	if (!sample.getUser().getUserLogin().equals(username)) return;
    	
    	if (visited.contains(sample.getId())) return;
    	visited.add(sample.getId());
    	
		logger.info("Processing "+sample.getName()+" (id="+sample.getId()+")");
		numSamples++;
		
		if (deleteSampleIfUnreferencedByOwner(sample)) {
			return;
		}

		entityBean.loadLazyEntity(sample, false);
		
		cleanupBrokenFastLoads(sample);
		migrateTilelessLsms(sample);
		removeOldTiles(sample);
		migratePairsToTiles(sample);
		migrateSampleStructure(sample);
		cleanMergedFiles(sample);
    }
    
    private boolean deleteSampleIfUnreferencedByOwner(Entity sample) throws ComputeException {
    	
    	for(EntityData ed : entityBean.getParentEntityDatas(sample.getId())) {
    		if (ed.getUser().getUserLogin().equals(sample.getUser().getUserLogin())) {
    			return false;
    		}
    	}
    	
    	logger.info("  Sample is not referenced by owner: "+sample.getName()+" (id="+sample.getId()+")");

    	long numAnnotated = annotationBean.getNumDescendantsAnnotated(sample.getId());
    	if (numAnnotated>0) {
    		logger.warn("  Cannnot delete sample because "+numAnnotated+" descendants are annotated");
    		return false;
    	}
    	
    	logger.info("  Removing unreferenced sample entirely: "+sample.getId());
        entityBean.deleteSmallEntityTree(sample.getUser().getUserLogin(), sample.getId(), true);
        
    	return true;
    }
    
    /**
     * Clean up Fast Load entities that were created with a broken fastLoad.sh
     */
    private void cleanupBrokenFastLoads(Entity sample) throws ComputeException {
    
    	for(Entity separation : sample.getChildrenOfType(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
    		populateChildren(separation);

        	Entity supportingFiles = EntityUtils.getSupportingData(separation);
        	if (supportingFiles==null) return; 
        	populateChildren(supportingFiles);
        	
    		Entity fastLoad = EntityUtils.findChildWithName(supportingFiles, "Fast Load");
    		if (fastLoad==null) continue;
    		
    		String filepath = fastLoad.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
    		
    		if (filepath==null || !(new File(filepath).exists())) {
    			// Delete it
    			
    			populateChildren(fastLoad);
    			for(Entity child : fastLoad.getChildren()) {
    				// See if there's a duplicate under the separation itself
    				EntityData ed = EntityUtils.findChildEntityDataWithName(separation, child.getName());
    				if (ed!=null) {
	    				logger.info("  Deleting duplicate fast load image: "+child.getName());
	                    if (!isDebug) {
		    				entityBean.deleteEntityData(ed);
		    				entityBean.deleteEntityById(ed.getChildEntity().getId());
	                    }
    				}
    			}
    			
    			logger.info("  Deleting broken fast load folder for separation: "+separation.getId());
                if (!isDebug) {
                	entityBean.deleteSmallEntityTree(username, fastLoad.getId());
                }
    		}
    	}
    }
    
    /**
     * Check for Samples with LSMs that are not inside Tile entities.
     */
    private void migrateTilelessLsms(Entity sample) throws ComputeException {
    	
    	Entity supportingFiles = EntityUtils.getSupportingData(sample);
    	if (supportingFiles==null) return; 
    	
        populateChildren(supportingFiles);
        
    	List<EntityData> toMove = new ArrayList<EntityData>();
		for(EntityData ed : supportingFiles.getEntityData()) {
			Entity child = ed.getChildEntity();
			if (child!=null && child.getEntityType().getName().equals(EntityConstants.TYPE_LSM_STACK)) {
				toMove.add(ed);
			}
		}
		
		if (!toMove.isEmpty()) {
			logger.info("  Found old-style LSMs, id="+sample.getId()+" name="+sample.getName());
        	int i = 1;
        	List<Long> toAdd = new ArrayList<Long>();
        	
    		for(EntityData ed : toMove) {
                if (!isDebug) {
                	// Update database
                	Entity tile = entityBean.createEntity(username, EntityConstants.TYPE_IMAGE_TILE, "Tile "+i);
                	ed.setParentEntity(tile);
                	entityBean.saveOrUpdateEntityData(ed);
                	// Update in-memory model
                	tile.getEntityData().add(ed);
                	supportingFiles.getEntityData().remove(ed);
                	toAdd.add(tile.getId());
                }
                i++;
                numChanges++;
    		}
    		
    		if (!isDebug) {
    			entityBean.addChildren(username, supportingFiles.getId(), toAdd, EntityConstants.ATTRIBUTE_ENTITY);
    		}
		}
    }

    /**
     * Check for Samples with LSM Pairs and convert them to Image Tiles.
     */
    private void removeOldTiles(Entity sample) throws ComputeException {
    	
    	Entity supportingFiles = EntityUtils.getSupportingData(sample);
    	if (supportingFiles==null) return; 
    	
        populateChildren(supportingFiles);

		for(Entity entity : supportingFiles.getChildrenOfType(EntityConstants.TYPE_FOLDER)) {
			
			if (entity.getName().endsWith("(old)")) {
				logger.info("  Deleting old tile '"+entity.getName()+"' (id="+entity.getId()+")");
                if (!isDebug) {
                	entityBean.deleteSmallEntityTree(user.getUserLogin(), entity.getId());	
                }
			}
		}
    }
    
    /**
     * Check for Samples with LSM Pairs and convert them to Image Tiles.
     */
    private void migratePairsToTiles(Entity sample) throws ComputeException {
    	
    	Entity supportingFiles = EntityUtils.getSupportingData(sample);
    	if (supportingFiles==null) return; 
    	
        populateChildren(supportingFiles);

        EntityAttribute attr = entityBean.getEntityAttributeByName(EntityConstants.ATTRIBUTE_ENTITY);
        
		for(Entity entity : supportingFiles.getChildrenOfType(EntityConstants.TYPE_IMAGE_TILE)) {
			
			if (entity.getName().equals("Scans")) {
				entity.setName("Tile 1");
				logger.info("  Renaming tile (id="+entity.getId()+") from 'Scans' to 'Tile 1'");
                if (!isDebug) {
                	entityBean.saveOrUpdateEntity(entity);
                }
			}
			
            populateChildren(entity);
            
            List<EntityData> children = EntityUtils.getOrderedEntityDataForAttribute(entity, EntityConstants.ATTRIBUTE_ENTITY);
            
            if (children.isEmpty()) {
                EntityData stack1ed = entity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_LSM_STACK_1);

                if (stack1ed!=null) {
                	logger.info("  Found old-style LSM Pair, id="+entity.getId()+" name="+entity.getName());
                    stack1ed.setEntityAttribute(attr);
                    stack1ed.setOrderIndex(0);
                    logger.info("    Updating stack 1, id="+stack1ed.getId());
                    if (!isDebug) {
                    	entityBean.saveOrUpdateEntityData(stack1ed);
                    }
                }
                
                EntityData stack2ed = entity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_LSM_STACK_2);
                if (stack2ed!=null) {
                    stack2ed.setEntityAttribute(attr);
                    stack2ed.setOrderIndex(1);
                    logger.info("    Updating stack 2, id="+stack2ed.getId());
                    if (!isDebug) {
                    	entityBean.saveOrUpdateEntityData(stack2ed);
                    }
                }
            }
            
		}
    }
    
    /**
     * Check for Samples with the old result structure.
     */
    private void migrateSampleStructure(Entity sample) throws Exception {
    	
    	if (EntityUtils.findChildEntityDataWithType(sample, EntityConstants.TYPE_PIPELINE_RUN) != null) {
    		// this is a new-style sample, no need to migrate it
    		return;
    	}
    	
    	logger.info("  Found old-style sample, id="+sample.getId()+" name="+sample.getName());
    	
    	EntityData atEd = sample.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_TYPES);
    	if (atEd!=null) {
    		logger.info("    Removing defunct attribute: ALIGNMENT_TYPES (value="+atEd.getValue()+")");
    		entityBean.deleteEntityData(atEd);
    		sample.getEntityData().remove(atEd);
    	}
    	
    	List<EntityData> results = new ArrayList<EntityData>();
    	List<EntityData> sepResults = new ArrayList<EntityData>();
    	
    	Date lastDate = null;
    	
    	for (EntityData ed : sample.getOrderedEntityData()) {
    		
    		Entity entity = ed.getChildEntity();
    		if (entity==null) continue;
    		
    		populateChildren(entity);
    		
    		if (EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE.equals(ed.getEntityAttribute().getName())) {
    			// Ignore
    		}
    		else if (EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE.equals(ed.getEntityAttribute().getName())) {
    			// Ignore
    		}
    		else if (EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE.equals(ed.getEntityAttribute().getName())) {
    			// Ignore
    		}
    		else {
    			if (EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT.equals(entity.getEntityType().getName())) {
    				lastDate = ed.getCreationDate();
    				sepResults.add(ed);
    			}
    			else if (EntityConstants.TYPE_SUPPORTING_DATA.equals(entity.getEntityType().getName())) {
    				// Don't move the Supporting Data, it stays at Sample level
    			}
    			else {
    				results.add(ed);
    			}
    		}
    	}
    	
    	if (isDebug) return;
    	
    	if (results.isEmpty()) {
    		logger.info("  No results found");
    		return;
    	}
    	
    	Entity pipelineRun = entityBean.createEntity(username, EntityConstants.TYPE_PIPELINE_RUN, "FlyLight Pipeline Results");

    	// Pretend like this run was created back when it should have been
    	pipelineRun.setCreationDate(lastDate);
    	pipelineRun.setUpdatedDate(lastDate);
    	entityBean.saveOrUpdateEntity(pipelineRun);
    	
		Collections.reverse(results);
		Collections.reverse(sepResults);
		
		boolean moved = false;
		for(EntityData sepEd : sepResults) {
			Entity sep = sepEd.getChildEntity();
			
			for(EntityData resultEd : results) {	
				Entity result = resultEd.getChildEntity();
				String type = result.getEntityType().getName();
				
				if ((sep.getName().startsWith("Prealigned") && type.equals(EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT)) || 
						(sep.getName().startsWith("Aligned") && type.equals(EntityConstants.TYPE_ALIGNMENT_RESULT) 
								&& ((sep.getName().contains("63x") && result.getName().contains("63x")) ||
										(!sep.getName().contains("63x") && !result.getName().contains("63x"))))) {
					sepEd.setParentEntity(result);
					entityBean.saveOrUpdateEntityData(sepEd);
					sample.getEntityData().remove(sepEd);
					result.getEntityData().add(sepEd);
					moved = true;
				}
			}
			
			if (!moved) {
				logger.warn("Could not find a place for neuron separation id="+sep.getId()+", moving to pipeline run.");
				sepEd.setParentEntity(pipelineRun);
				entityBean.saveOrUpdateEntityData(sepEd);
				sample.getEntityData().remove(sepEd);
				pipelineRun.getEntityData().add(sepEd);
			}
		}

		Collections.reverse(results);
		
		
		for(EntityData resEd : results) {
			resEd.setParentEntity(pipelineRun);
			entityBean.saveOrUpdateEntityData(resEd);
			sample.getEntityData().remove(resEd);
			pipelineRun.getEntityData().add(resEd);
		}
		
		entityBean.addEntityToParent(username, sample, pipelineRun, sample.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
		
		for (EntityData resultEd : EntityUtils.getOrderedEntityDataForAttribute(pipelineRun, EntityConstants.ATTRIBUTE_RESULT)) {

			Entity result = resultEd.getChildEntity();
			
			logger.info("   Upgrading "+result.getName()+" (id="+result.getId()+")");
			
			Entity supportingFiles = EntityUtils.getSupportingData(result);
	        populateChildren(supportingFiles);
			
	        String resultDefault2dImage = result.getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
			if (resultDefault2dImage!=null) {
				logger.info("    Default 2d image: "+resultDefault2dImage);
				
		        String defaultImageFilename = null;
		        int priority = 0;
				for (Entity file : supportingFiles.getChildren()) {
					String filename = file.getName();
					String filepath = file.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
					String fileDefault2dImage = file.getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
					if (StringUtils.isEmpty(filepath)) continue;
					
					logger.debug("    Considering "+filename);
					logger.debug("      filepath: "+filepath);
					if (fileDefault2dImage!=null) {
						logger.debug("      default 2d image: "+fileDefault2dImage);
					}
					
					if (resultDefault2dImage!=null && resultDefault2dImage.equals(fileDefault2dImage) && priority < 20) {
						defaultImageFilename = filepath;
						priority = 20;
					}
					if (filename.matches("Aligned.v3d(raw|pbd)") && priority < 10) {
						defaultImageFilename = filepath;
						priority = 10;
					}
					else if (filename.matches("stitched-(\\w+?).v3d(raw|pbd)") && priority < 9) {
						defaultImageFilename = filepath;
						priority = 9;
					}
					else if (filename.matches("tile-(\\w+?).v3d(raw|pbd)") && priority < 8) {
						defaultImageFilename = filepath;
						priority = 8;
					}
					else if (filename.matches("merged-(\\w+?).v3d(raw|pbd)") && priority < 7) {
						defaultImageFilename = filepath;
						priority = 7;
					}
				}
				
				if (defaultImageFilename==null) {
					logger.warn("  Could not find default image for result "+result.getId());
				}
				else {
					logger.debug("  Found default image "+defaultImageFilename+" with priority "+priority);
					logger.info("  Running result image registration with "+defaultImageFilename);
			        resultImageRegService = new ResultImageRegistrationService();
					resultImageRegService.execute(processData, result, sample, defaultImageFilename);
				}
			}
			else {
				logger.info("    No default 2d image found, attempting to use neuron separation");
				
				// No MIPs for this result, use the neuron separation mips
				Entity sep = result.getLatestChildOfType(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT);
				
				if (sep!=null) {
					Entity signalMip = sep.getChildByAttributeName(EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE);
					Entity refMip = sep.getChildByAttributeName(EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE);
	
					if (signalMip!=null) {
						entityHelper.setImage(result, EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE, signalMip);
						entityHelper.setDefault2dImage(result, signalMip);
						entityHelper.setImage(pipelineRun, EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE, signalMip);
						entityHelper.setDefault2dImage(pipelineRun, signalMip);
					}
					
					if (refMip!=null) {
						entityHelper.setImage(result, EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE, refMip);
						entityHelper.setImage(pipelineRun, EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE, refMip);
					}
				}
			}
		}		
		
		logger.info("    Moved results to pipeline run, id="+pipelineRun.getId()+" name="+pipelineRun.getName());
    }

    private void cleanMergedFiles(Entity sample) throws ComputeException {
    	
    	final List<EntityData> stitched = new ArrayList<EntityData>();
    	final List<EntityData> merged = new ArrayList<EntityData>();
    	
    	EntityVistationBuilder iterator = new EntityVistationBuilder(entityLoader);
    	iterator.startAt(sample)
			.childrenOfType(EntityConstants.TYPE_PIPELINE_RUN)
			.childrenOfType(EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT)
			.childOfType(EntityConstants.TYPE_SUPPORTING_DATA)
			.childrenOfType(EntityConstants.TYPE_IMAGE_3D).visit(new EntityVisitor() {
				@Override
				public void visit(EntityData entityData) {
					Entity entity = entityData.getChildEntity();
					if (entity.getName().startsWith("stitched-")) {
						stitched.add(entityData);
					}
					else if (entity.getName().startsWith("merged-")||entity.getName().startsWith("tile-")) {
						merged.add(entityData);
					}
				}
			});
    	
		if (!stitched.isEmpty()) {

	    	logger.info("    Found "+merged.size()+" candidates for deletion for sample id="+sample.getId());
	    	
			for(EntityData mergedFileEd : merged) {
				
				Entity mergedFile = mergedFileEd.getChildEntity();
				File file = new File(mergedFile.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
				
				if (!file.getAbsolutePath().startsWith(userFilestore.getAbsolutePath())) {
					logger.info("    Skipping file which is not in the user's filestore: "+file.getAbsolutePath());
					continue;
				}

				try {
		    		File symlink = new File(file.getAbsolutePath().replace("merge", "group"));
		    		if (symlink.exists()) {
			    		logger.info("    Cleaning up symlink to merged tile: "+symlink.getAbsolutePath());
			    		FileUtils.forceDelete(symlink);
		    		}
				}
				catch (Exception e) {
					logger.error("    Error deleting symlink: "+file.getAbsolutePath(),e);
				}
				
				try {
		    		if (file.exists()) {
			    		logger.info("    Cleaning up merged tile: "+file.getAbsolutePath());
			    		FileUtils.forceDelete(file);
		    		}
				}
				catch (Exception e) {
					logger.error("    Error deleting file: "+file.getAbsolutePath(),e);
				}

				entityBean.deleteEntityData(mergedFileEd);
				entityBean.deleteSmallEntityTree(username, mergedFile.getId());
			}
		}
    }
}
