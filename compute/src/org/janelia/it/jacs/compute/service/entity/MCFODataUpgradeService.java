package org.janelia.it.jacs.compute.service.entity;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.util.FileUtils;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.*;
import org.janelia.it.jacs.model.entity.cv.PipelineProcess;
import org.janelia.it.jacs.shared.utils.EntityUtils;
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
        
        EntityType lsmStackPairType = entityBean.getEntityTypeByName(EntityConstants.TYPE_LSM_STACK_PAIR);
        if (lsmStackPairType!=null) {
        	logger.info("Renaming '"+EntityConstants.TYPE_LSM_STACK_PAIR+"' to '"+EntityConstants.TYPE_IMAGE_TILE+"'");
            lsmStackPairType.setName(EntityConstants.TYPE_IMAGE_TILE);
            if (!isDebug) {
            	computeBean.genericSave(lsmStackPairType);
            }
        }
        
        logger.info("Adding data sets...");
        if ("asoy".equals(username)) {
        	createDataSet("MB Flp-out 63X", PipelineProcess.YoshiMB63x, true);
        	createDataSet("MB LexA-GAL4 63x", PipelineProcess.YoshiMB63x, true);
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
			dataSet.setValueByAttributeName(EntityConstants.ATTRIBUTE_PIPELINE_PROCESS, process.toString());
			if (sageSync) {
				dataSet.setValueByAttributeName(EntityConstants.ATTRIBUTE_SAGE_SYNC, EntityConstants.ATTRIBUTE_SAGE_SYNC);
			}
			dataSet = entityBean.saveOrUpdateEntity(dataSet);
		}
    }

	public void processSamples() throws ComputeException {
    	List<Entity> samples = entityBean.getUserEntitiesByTypeName(username, EntityConstants.TYPE_SAMPLE);
        for(Entity sample : samples) {
            processSample(sample);
        }
    }
    
    public void processSample(Entity sample) throws ComputeException {
    	
    	if (!sample.getUser().getUserLogin().equals(username)) return;
    	
    	if (visited.contains(sample.getId())) return;
    	visited.add(sample.getId());
    	
		logger.info("Processing "+sample.getName()+" (id="+sample.getId()+")");
		numSamples++;
		
		entityBean.loadLazyEntity(sample, false);
		
		cleanupBrokenFastLoads(sample);
		migrateTilelessLsms(sample);
		migratePairsToTiles(sample);
		migrateSampleStructure(sample);
		cleanMergedFiles(sample);
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
	    				logger.info("Deleting duplicate fast load image: "+child.getName());
	                    if (!isDebug) {
		    				entityBean.deleteEntityData(ed);
		    				entityBean.deleteEntityById(ed.getChildEntity().getId());
	                    }
    				}
    			}
    			
    			logger.info("Deleting broken fast load folder for separation: "+separation.getId());
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
			logger.info("Found old-style LSMs, id="+sample.getId()+" name="+sample.getName());
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
    private void migratePairsToTiles(Entity sample) throws ComputeException {
    	
    	Entity supportingFiles = EntityUtils.getSupportingData(sample);
    	if (supportingFiles==null) return; 
    	
        populateChildren(supportingFiles);

        EntityAttribute attr = entityBean.getEntityAttributeByName(EntityConstants.ATTRIBUTE_ENTITY);
        
		for(Entity entity : supportingFiles.getChildrenOfType(EntityConstants.TYPE_IMAGE_TILE)) {
			
			boolean fileWasDiscoveredNotSaged = false;
			if (entity.getName().equals("Scans")) {
				fileWasDiscoveredNotSaged = true;
				entity.setName("Tile 1");
				logger.info("Renaming tile (id="+entity.getId()+") from 'Scans' to 'Tile 1'");
                if (!isDebug) {
                	entityBean.saveOrUpdateEntity(entity);
                }
			}
			
            populateChildren(entity);
            
            List<EntityData> children = EntityUtils.getOrderedEntityDataForAttribute(entity, EntityConstants.ATTRIBUTE_ENTITY);
            
            if (children.isEmpty()) {
                EntityData stack1ed = entity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_LSM_STACK_1);

                if (stack1ed!=null) {
                	logger.info("Found old-style LSM Pair, id="+entity.getId()+" name="+entity.getName());
                    stack1ed.setEntityAttribute(attr);
                    stack1ed.setOrderIndex(0);
                    logger.info("    Updating stack 1, id="+stack1ed.getId());
                    if (!isDebug) {
                    	entityBean.saveOrUpdateEntityData(stack1ed);
                    }
                    
                    if (fileWasDiscoveredNotSaged) {
    	                Entity lsmStack = stack1ed.getChildEntity();
    	                if (lsmStack!=null) {
    	                	populateLsmStackAttributes(lsmStack, "ssr");
    	                }
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

                    if (fileWasDiscoveredNotSaged) {
    	                Entity lsmStack = stack1ed.getChildEntity();
    	                if (lsmStack!=null) {
    	                	populateLsmStackAttributes(lsmStack, "sr");
    	                }
                    }
                }
            }
            else {
            	int i = 0;
            	for(EntityData childEd : children) {
                    if (fileWasDiscoveredNotSaged) {
    	                Entity lsmStack = childEd.getChildEntity();
    	                if (lsmStack!=null) {
    	                	populateLsmStackAttributes(lsmStack, i<1?"ssr":"sr");
    	                	i++;
    	                }
                    }
            	}
            }
            
		}
    }


    protected Entity populateLsmStackAttributes(Entity lsmStack, String channelSpec) throws ComputeException {
    	logger.info("      Setting stack properties: channels="+channelSpec.length()+", spec="+channelSpec);
    	if (lsmStack.getValueByAttributeName(EntityConstants.ATTRIBUTE_NUM_CHANNELS)==null) {
    		lsmStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_NUM_CHANNELS, ""+channelSpec.length());	
    	}
    	if (lsmStack.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION)==null) {
    		lsmStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION, channelSpec);	
    	}
    	if (!isDebug) {
    		lsmStack = entityBean.saveOrUpdateEntity(lsmStack);
    	}
        return lsmStack;
    }
    
    /**
     * Check for Samples with the old result structure.
     */
    private void migrateSampleStructure(Entity sample) throws ComputeException {
    	
    	if (EntityUtils.findChildEntityDataWithType(sample, EntityConstants.TYPE_PIPELINE_RUN) != null) {
    		// this is a new-style sample, no need to migrate it
    		return;
    	}
    	
    	logger.info("Found old-style sample results, id="+sample.getId()+" name="+sample.getName());
    	
    	EntityData atEd = sample.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_TYPES);
    	if (atEd!=null) {
    		logger.info("    Removing defunct attribute: ALIGNMENT_TYPES (value="+atEd.getValue()+")");
    		entityBean.deleteEntityData(atEd);
    		sample.getEntityData().remove(atEd);
    	}
    	
    	Entity defaultImage = null;
    	Entity signalMIP = null;
    	Entity referenceMIP = null;
    	
    	List<EntityData> results = new ArrayList<EntityData>();
    	List<EntityData> sepResults = new ArrayList<EntityData>();
    	
    	Date lastDate = null;
    	
    	for (EntityData ed : sample.getOrderedEntityData()) {
    		
    		Entity entity = ed.getChildEntity();
    		if (entity==null) continue;
    		
    		populateChildren(entity);
    		
    		if (EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE.equals(ed.getEntityAttribute().getName())) {
    			defaultImage = entity;
    		}
    		else if (EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE.equals(ed.getEntityAttribute().getName())) {
    			signalMIP = entity;
    		}
    		else if (EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE.equals(ed.getEntityAttribute().getName())) {
    			referenceMIP = entity;
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
    	
    	if (isDebug || results.isEmpty()) return;
    	
    	Entity pipelineRun = entityBean.createEntity(username, EntityConstants.TYPE_PIPELINE_RUN, "FlyLight Pipeline Results");

    	// Pretend like this run was created back when it should have been
    	pipelineRun.setCreationDate(lastDate);
    	pipelineRun.setUpdatedDate(lastDate);
    	entityBean.saveOrUpdateEntity(pipelineRun);
    	
    	entityHelper.setImage(pipelineRun, EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE, signalMIP);
		entityHelper.setImage(pipelineRun, EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE, referenceMIP);	
		entityHelper.setDefault2dImage(pipelineRun, defaultImage);
    	
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
					moved = true;
				}
			}
			
			if (!moved) {
				logger.warn("Could not find a place for neuron separation id="+sep.getId()+", moving to pipeline run.");
				sepEd.setParentEntity(pipelineRun);
				entityBean.saveOrUpdateEntityData(sepEd);
				sample.getEntityData().remove(sepEd);
			}
		}

		Collections.reverse(results);
		
		for(EntityData resEd : results) {
			resEd.setParentEntity(pipelineRun);
			entityBean.saveOrUpdateEntityData(resEd);
			sample.getEntityData().remove(resEd);
		}
		
		entityBean.addEntityToParent(username, sample, pipelineRun, sample.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
		
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
