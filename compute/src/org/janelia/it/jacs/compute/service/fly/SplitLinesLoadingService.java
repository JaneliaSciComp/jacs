package org.janelia.it.jacs.compute.service.fly;

import java.io.File;
import java.util.*;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.entity.EntityHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * This service combines the Screen data and Arnim's representatives and split constructs spreadsheets. Everything is 
 * loaded into a top level structure. The following parameters are required:
 * TOP_LEVEL_FOLDER_NAME - the name of the top level folder to stick everything in
 * REPRESENTATIVES_FILEPATH - absolute path to Arnim's representatives.txt
 * SPLIT_CONSTRUCTS_FILEPATH - absolute path to Arnim's splitConstructs.txt
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SplitLinesLoadingService implements IService {
	
	private static final boolean DEBUG = true;
	
    protected Logger logger;
    protected Task task;
    protected User user;
    protected Date createDate;
    protected EntityBeanLocal entityBean;
    protected ComputeBeanLocal computeBean;
    
    protected Set<Specimen> representatives = new HashSet<Specimen>();
    protected Map<String, String> splitConstructs = new HashMap<String,String>();
    protected Map<String, String> splitRepresentatives = new HashMap<String,String>();
    protected Map<String, String> splitBalancedLines = new HashMap<String,String>();
	
    public void execute(IProcessData processData) throws ServiceException {
    	
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            task = ProcessDataHelper.getTask(processData);
            entityBean = EJBFactory.getLocalEntityBean();
            computeBean = EJBFactory.getLocalComputeBean();
            user = computeBean.getUserByName(ProcessDataHelper.getTask(processData).getOwner());
            createDate = new Date();
            
            String topLevelFolderName = (String)processData.getItem("TOP_LEVEL_FOLDER_NAME");
        	if (topLevelFolderName == null) {
        		throw new IllegalArgumentException("TOP_LEVEL_FOLDER_NAME may not be null");
        	}
        	
        	String representativesFilepath = (String)processData.getItem("REPRESENTATIVES_FILEPATH");
        	if (representativesFilepath == null) {
        		throw new IllegalArgumentException("REPRESENTATIVES_FILEPATH may not be null");
        	}
        	
        	String splitConstructsFilepath = (String)processData.getItem("SPLIT_CONSTRUCTS_FILEPATH");
        	if (splitConstructsFilepath == null) {
        		throw new IllegalArgumentException("SPLIT_CONSTRUCTS_FILEPATH may not be null");
        	}
        	
        	readRepresentatives(new File(representativesFilepath));
        	readSplitConstructs(new File(splitConstructsFilepath));
        	
        	Entity topLevelFolder = populateChildren(createOrVerifyRootEntity(topLevelFolderName, user, createDate, true, false));
            logger.info("Using topLevelFolder with id=" + topLevelFolder.getId());
            
        	// First create any necessary folders, and add any normal screen flyline that is not already there
        	
        	logger.info("Adding screen flylines");
        	
        	Map<String,Entity> flylineMap = new HashMap<String,Entity>();
        	
        	Map<String,Entity> fragmentFolders = new HashMap<String,Entity>();
        	List<Entity> flylines = entityBean.getEntitiesByTypeName(EntityConstants.TYPE_FLY_LINE);
        	for(Entity flyline : flylines) {
        		logger.info("  Processing screen data fly line : "+flyline.getName());
        		flylineMap.put(flyline.getName(), flyline);
        		
        		populateChildren(flyline);
        		String flylineName = flyline.getName();
        		Specimen specimen = Specimen.createSpecimenFromFullName(flyline.getName());
    			if (!specimen.isValidLine()) {
    				logger.info("    Invalid line name: "+flylineName);
    				continue;
    			}
    			
        		Entity prefixFolder = verifyOrCreateChild(topLevelFolder, specimen.getLab()+"_"+specimen.getPlate(), EntityConstants.TYPE_FOLDER);
        		populateChildren(prefixFolder);
        		
        		Entity fragmentFolder = fragmentFolders.get(specimen.getFragmentName());
        		if (fragmentFolder == null) {
        			fragmentFolder = verifyOrCreateChild(prefixFolder, specimen.getFragmentName(), EntityConstants.TYPE_FOLDER);
            		fragmentFolders.put(specimen.getFragmentName(), fragmentFolder);	
        		}
        		populateChildren(fragmentFolder);
        		
        		boolean foundFlyline = false;
        		for (Entity child : fragmentFolder.getChildren()) {
        			if (child.getName().equals(flyline.getName())) {
        				foundFlyline = true;
        				break;
        			}
        		}
        		    	
        		if (!foundFlyline) { 
        			addToParent(fragmentFolder, flyline, flyline.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
        		}
        		
        		if (splitConstructs.containsKey(flylineName)) {
        			logger.info("    "+flyline+" is a split part: " +splitConstructs.get(flylineName));
        			String newSplitPart = splitConstructs.get(flylineName);
        			setFlylineSplitPart(flyline, newSplitPart);
        		}
        		
        		// Fix flylines with more than 1 representative sample
        		List<EntityData> reps = EntityUtils.getOrderedEntityDataForAttribute(flyline, EntityConstants.ATTRIBUTE_REPRESENTATIVE_SAMPLE);
        		if (reps.size()>1) {
        			logger.warn("    "+flyline+" has >1 representative samples so we're deleting them all. A single correct representative will be added later.");
        			for (EntityData ed : new ArrayList<EntityData>(reps)) {
        				flyline.getEntityData().remove(ed); // Need to remove it from the object as well, since we'll be saving it later
        				entityBean.deleteEntityData(ed);
        			}
        		}
        		
        		// Add representatives
        		for(EntityData ed : new ArrayList<EntityData>(flyline.getEntityData())) {
        			if (!ed.getEntityAttribute().getName().equals(EntityConstants.ATTRIBUTE_ENTITY)) continue;
        			Entity screenSample = ed.getChildEntity();
        			if (screenSample==null) continue;
        			if (!screenSample.getEntityType().getName().equals(EntityConstants.TYPE_SCREEN_SAMPLE)) continue;
        			if (!representatives.contains(Specimen.createSpecimenFromFullName(screenSample.getName()))) continue;
        			setFlylineRepresentative(flyline, screenSample);
        			break;
        		}
        	}
        	
        	// Add balanced lines
        	logger.info("Adding balanced lines");
        	
    		for(String flylineName : new HashSet<String>(splitBalancedLines.values())) {
    			Specimen specimen = Specimen.createSpecimenFromFullName(flylineName);
    			if (!specimen.isValidLine()) {
    				logger.info("  Got invalid balanced line: "+flylineName);
    				continue;
    			}
    			
    			logger.info("  Got balanced line: " +flylineName);
    			
    			Entity flyline = flylineMap.get(flylineName);
    			if (flyline == null) {
    				flyline = createFlylineEntity(flylineName, null);
            		Entity fragmentFolder = fragmentFolders.get(specimen.getFragmentName());
            		if (fragmentFolder == null) {
            			logger.warn("    No existing fragment folder: "+specimen.getFragmentName());
            		}
            		else {
            			if (EntityUtils.findChildEntityDataWithNameAndType(fragmentFolder, flyline.getName(), EntityConstants.TYPE_FLY_LINE) == null) {
                			logger.warn("    Adding to fragment folder: "+specimen.getFragmentName());
                			addToParent(fragmentFolder, flyline, flyline.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);	
            			}
            			else {
            				logger.warn("    Already part of fragment folder: "+specimen.getFragmentName());
            			}
            		}
    			}
    			
        		flylineMap.put(flyline.getName(), flyline);
    		}        	
        	
    		// Now add split lines 
        	logger.info("Adding split constructs");
        	
    		for(String flylineName : splitConstructs.keySet()) {
    			Specimen specimen = Specimen.createSpecimenFromFullName(flylineName);
    			if (!specimen.isValidLine()) {
    				logger.info("  Got invalid split line: "+flylineName);
    				continue;
    			}
    			
    			logger.info("  Got split line: " +flylineName+" ("+splitConstructs.get(flylineName)+")");
    			
    			Entity flyline = flylineMap.get(flylineName);
    			if (flyline == null) {
    				flyline = createFlylineEntity(flylineName, splitConstructs.get(flylineName));
            		Entity fragmentFolder = fragmentFolders.get(specimen.getFragmentName());
            		if (fragmentFolder == null) {
            			logger.warn("    No existing fragment folder: "+specimen.getFragmentName());
            		}
            		else {
            			if (EntityUtils.findChildEntityDataWithNameAndType(fragmentFolder, flyline.getName(), EntityConstants.TYPE_FLY_LINE) == null) {
                			logger.warn("    Adding to fragment folder: "+specimen.getFragmentName());
                			addToParent(fragmentFolder, flyline, flyline.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);	
            			}
            			else {
            				logger.warn("    Already part of fragment folder: "+specimen.getFragmentName());
            			}
            		}
    			}
    			else {
        			setFlylineSplitPart(flyline, splitConstructs.get(flylineName));
    			}
    			
    			Entity balancedFlyline = flylineMap.get(splitBalancedLines.get(flylineName));
    			setFlylineBalance(flyline, balancedFlyline);
    		}

    		// Renumber the children, re-add the new ones, and update the tree

    		logger.info("Renumbering flylines");
    		
    		for(Entity fragmentFolder : fragmentFolders.values()) {
    			
    	    	List<EntityData> orderedData = new ArrayList<EntityData>(fragmentFolder.getEntityData());
    	    	Collections.sort(orderedData, new Comparator<EntityData>() {
    				@Override
    				public int compare(EntityData o1, EntityData o2) {
    					return o1.getChildEntity().getName().compareTo(o2.getChildEntity().getName());
    				}
    			});
    			
    	    	int i = 1;
    			for(EntityData ed : orderedData) {
    				if (i!=ed.getOrderIndex()) {
						ed.setOrderIndex(i);
						entityBean.saveOrUpdateEntityData(ed);
    				}
    				i++;
    			}
    		}

    		logger.info("Marking reverse representatives");
    		
    		// All representatives are within the same fragment
    		for(Entity fragment : fragmentFolders.values()) {

    			logger.info("---------------------------------");
    			logger.info(fragment.getName());
    			
        		// First cache all the non-split lines for this fragment
        		Map<String,Map<String,Entity>> vectors = new HashMap<String,Map<String,Entity>>();
        		Map<String,Map<String,Map<String,Entity>>> vectorsFull = new HashMap<String,Map<String,Map<String,Entity>>>();
    			for(Entity flyline : fragment.getChildrenOfType(EntityConstants.TYPE_FLY_LINE)) {
        			if (flyline.getValueByAttributeName(EntityConstants.ATTRIBUTE_SPLIT_PART)!=null) continue;
        			populateChildren(flyline);
        			
        			Specimen specimen = Specimen.createSpecimenFromFullName(flyline.getName());
        			if (!specimen.isValidLine()) {
        				continue;
        			}
        			
        			Map<String,Entity> inserts = vectors.get(specimen.getVector());
        			if (inserts==null) {
        				inserts = new HashMap<String,Entity>();
        				vectors.put(specimen.getVector(), inserts);
        			}
        			
        			Entity sample = inserts.get(specimen.getInsertionSite());
        			if (sample==null) {
            			EntityData repEd = flyline.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_REPRESENTATIVE_SAMPLE);
            			if (repEd!=null) {
            				Entity rep = repEd.getChildEntity();
            				if (rep!=null) {
            					inserts.put(specimen.getInsertionSite(), rep);	
            				}
            			}
        			}
        			
        			Map<String,Map<String,Entity>> fullInserts = vectorsFull.get(specimen.getVector());
        			if (fullInserts==null) {
        				fullInserts = new HashMap<String,Map<String,Entity>>();
        				vectorsFull.put(specimen.getVector(), fullInserts);
        			}
        			
        			Map<String,Entity> samples = fullInserts.get(specimen.getInsertionSite());
        			if (samples==null) {
        				samples = new HashMap<String,Entity>();
        				fullInserts.put(specimen.getInsertionSite(), samples);
        			}
        			
        			for(Entity child : flyline.getChildren()) {
        				if (child.getEntityType().getName().equals(EntityConstants.TYPE_SCREEN_SAMPLE)) {
        					samples.put(child.getName(),child);
        				}
        			}
    			}
    			
        		// Now assign representatives to the split lines from the non-split lines
    			for(Entity flyline : fragment.getChildrenOfType(EntityConstants.TYPE_FLY_LINE)) {
        			String splitPart = flyline.getValueByAttributeName(EntityConstants.ATTRIBUTE_SPLIT_PART);
	    			if (splitPart!=null) {
	    				// This is a split line which needs a representative
        				Specimen line = Specimen.createSpecimenFromFullName(flyline.getName());
            			if (!line.isValidLine()) {
            				continue;
            			}
    					String specimenName = splitRepresentatives.get(flyline.getName());
    					if (specimenName!=null) {
    						Specimen repSpecimen = Specimen.createSpecimenFromFullName(specimenName);
    	        			if (!repSpecimen.isValidLine()) {
    	        				continue;
    	        			}
    						Map<String,Map<String,Entity>> fullInserts = vectorsFull.get(repSpecimen.getVector());
    						
    						boolean found = false;
    						if (fullInserts != null) {
    							Map<String,Entity> samples = fullInserts.get(repSpecimen.getInsertionSite());
    							if (samples != null) {
        							Entity rep = samples.get(specimenName);
        							if (rep!=null) {
            	            			setFlylineRepresentative(flyline, rep);
            	            			found = true;
            	    				}
    							}
    						}    	
    						
    						if (!found) {
    							logger.warn("Representative for "+flyline.getName()+" should be "+specimenName+", but it couldn't be found.");	
    						}
    					}
    					else {
    						// TODO: this is for testing, and should be removed later
    						Entity rep = findBestRepresentative(vectors, splitPart, line);
    						if (rep!=null) {
    							logger.warn("No representative for "+flyline.getName()+", but we found a decent candidate: "+rep.getName());
    						}
    					}
	    			}
    			}
    		
    			if (DEBUG) {
        			
	        		Map<String,Map<String,Entity>> vectors2 = new HashMap<String,Map<String,Entity>>();
	    			for(Entity flyline : fragment.getChildrenOfType(EntityConstants.TYPE_FLY_LINE)) {
	    				populateChildren(flyline);
	    				
	    				Specimen specimen = Specimen.createSpecimenFromFullName(flyline.getName());
	        			if (!specimen.isValidLine()) {
	        				continue;
	        			}
	        			
	        			Map<String,Entity> inserts = vectors2.get(specimen.getVector());
	        			if (inserts==null) {
	        				inserts = new HashMap<String,Entity>();
	        				vectors2.put(specimen.getVector(), inserts);
	        			}
	        		
						inserts.put(specimen.getInsertionSite(), flyline);	
	    			}
	
	    			for(String vector : vectors2.keySet()) {
	        			logger.info("  "+vector);
						for(String insert : keys(vectors2.get(vector))) {
							logger.info("    "+insert);
							Entity flyline = vectors2.get(vector).get(insert);
							
							String splitPart = flyline.getValueByAttributeName(EntityConstants.ATTRIBUTE_SPLIT_PART);
	
							Entity rep = null;
	            			EntityData repEd = flyline.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_REPRESENTATIVE_SAMPLE);
	            			if (repEd!=null) {
	            				rep = repEd.getChildEntity();
	            			}
	            			
							if (splitPart!=null) {
								logger.info("      "+flyline.getName()+" ("+splitPart+")"+(rep!=null?" -> "+rep.getName():""));
							}
							else {
								logger.info("      "+flyline.getName()+(rep!=null?" -> "+rep.getName():""));
								
								for(EntityData ed : flyline.getOrderedEntityData()) {
									if (ed.getEntityAttribute().getName().equals(EntityConstants.ATTRIBUTE_ENTITY)) {
										Entity screenSample = ed.getChildEntity();
										logger.info("        "+screenSample.getName()+(rep!=null&&screenSample.getId().equals(rep.getId())?" (REP)":""));
									}
								}
							}
						}
	    			}
    			}
    		}
    		
        } 
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    private void readRepresentatives(File representativesFile) throws Exception {
		Scanner scanner = new Scanner(representativesFile);
        try {
            while (scanner.hasNextLine()){
                String rep = scanner.nextLine();
                Specimen specimen = Specimen.createSpecimenFromFullName(rep);
                if (specimen.isValidLine() && "b".equals(specimen.getAnatomicalArea())) { // brain specimens only
                	representatives.add(specimen);
                }
            }
        }
        finally {
        	scanner.close();
        }
    }
    
    private String getCol(String[] cols, int index) {
    	if (index > cols.length-1) return null;
    	return cols[index];
    }
    
    private void readSplitConstructs(File splitConstructsFile) throws Exception {
    	
		Scanner scanner = new Scanner(splitConstructsFile);
        try {
        	String currFragmentId = null;
        	
            while (scanner.hasNextLine()){
                String[] parts = scanner.nextLine().split("\t");
                int c = 0;
                String splitPart = getCol(parts, c++);
                String fragmentId = getCol(parts, c++);
                String splitLineRobotId = getCol(parts, c++);
                String splitLineTransId = getCol(parts, c++);
                String balancedLineRobotId = getCol(parts, c++);
                String balancedLineTransId = getCol(parts, c++);
                String repLineTransId = getCol(parts, c++);
                String repSpecimen = getCol(parts, c++);
                String regImagePath = getCol(parts, c++);
                	
                if (StringUtils.isEmpty(fragmentId)) {
                	fragmentId = currFragmentId;
                }
                else {
                	currFragmentId = fragmentId;	
                }
                
                if (!StringUtils.isEmpty(splitLineTransId)) {
                    if (splitConstructs.containsKey(splitLineTransId)) {
                    	String registeredSplitPart = splitConstructs.get(splitLineTransId);
                    	if (!registeredSplitPart.equals(splitPart)) {
                    		logger.error("Already registered flyline "+splitLineTransId+" as "+registeredSplitPart+". Now it's "+splitPart);	
                    	}
                    }
                    else {
                    	splitConstructs.put(splitLineTransId, splitPart);
                    }
                    
                    if (!StringUtils.isEmpty(balancedLineTransId)) {
                    	splitBalancedLines.put(splitLineTransId, balancedLineTransId);
                    }
                    
                    if (!StringUtils.isEmpty(repSpecimen)) {
                    	splitRepresentatives.put(splitLineTransId, repSpecimen);
                    }
                }
            }
        }
        finally {
        	scanner.close();
        }
    }

    private void setFlylineBalance(Entity flyline, Entity balancedFlyline) throws Exception {
    	
		if (balancedFlyline!=null) {
			EntityData repEd2 = balancedFlyline.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_ORIGINAL_FLYLINE);
			if (repEd2!=null) {
				if (!repEd2.getChildEntity().getId().equals(flyline.getId())) {
					logger.info("FlyLine "+balancedFlyline.getName()+" has a new original flyline: "+flyline.getName()+" (old one was "+repEd2.getChildEntity().getId()+")");
					repEd2.setChildEntity(flyline);
					entityBean.saveOrUpdateEntityData(repEd2);
				}
			}
			else {
				logger.info("FlyLine "+balancedFlyline.getName()+" has original flyline: "+flyline.getName());
				addToParent(balancedFlyline, flyline, null, EntityConstants.ATTRIBUTE_ORIGINAL_FLYLINE);
				entityBean.saveOrUpdateEntity(balancedFlyline);
			}
	    }
    }
    
    private void setFlylineRepresentative(Entity flyline, Entity screenSample) throws Exception {
    	
    	boolean updated = false;
    	
		EntityData repEd = flyline.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_REPRESENTATIVE_SAMPLE);
		if (repEd!=null) {
			if (!repEd.getChildEntity().getId().equals(screenSample.getId())) {
				logger.info("FlyLine "+flyline.getName()+" has a new representative: "+screenSample.getName()+" (old one was "+repEd.getChildEntity().getId()+")");
				repEd.setChildEntity(screenSample);
				entityBean.saveOrUpdateEntityData(repEd);
				updated = true;
			}
		}
		else {
			logger.info("FlyLine "+flyline.getName()+" has representative: "+screenSample.getName());
			addToParent(flyline, screenSample, null, EntityConstants.ATTRIBUTE_REPRESENTATIVE_SAMPLE);
			entityBean.saveOrUpdateEntity(flyline);
			updated = true;
		}

		if (updated) {
			populateChildren(flyline);
			populateChildren(screenSample);
			EntityHelper helper = new EntityHelper();
			EntityData paFolderEd = EntityUtils.findChildEntityDataWithNameAndType(screenSample, "Pattern Annotation", EntityConstants.TYPE_FOLDER);
			if (paFolderEd!=null) {
				populateChildren(paFolderEd.getChildEntity());
				EntityData heatmapEd = EntityUtils.findChildEntityDataWithNameAndType(paFolderEd.getChildEntity(), "Heatmap", EntityConstants.TYPE_ALIGNED_BRAIN_STACK);
				if (heatmapEd!=null) {
					populateChildren(heatmapEd.getChildEntity());
					helper.setDefault3dImage(flyline, heatmapEd.getChildEntity());
				}
			}
		}
    }
    
    private void setFlylineSplitPart(Entity flyline, String splitPart) throws Exception {

		String currSplitPart = flyline.getValueByAttributeName(EntityConstants.ATTRIBUTE_SPLIT_PART);
		if (currSplitPart != null) {
			if (!currSplitPart.equals(splitPart)) {
				logger.warn("FlyLine "+flyline.getName()+" used to be "+currSplitPart+" but now "+splitPart);
				flyline.setValueByAttributeName(EntityConstants.ATTRIBUTE_SPLIT_PART, splitPart);
    			entityBean.saveOrUpdateEntity(flyline);
			}
		}
		else {
			logger.info("FlyLine "+flyline.getName()+" is now marked as "+splitPart);
			flyline.setValueByAttributeName(EntityConstants.ATTRIBUTE_SPLIT_PART, splitPart);
			entityBean.saveOrUpdateEntity(flyline);	
		}
    }
    
    /**
     * Returns the "best" representative for the given split line (AD or DBD). 
     */
    private Entity findBestRepresentative(Map<String,Map<String,Entity>> vectors, String splitPart, Specimen line) {
    	
		if ("DBD".equals(splitPart)) {
			// Prefer Gal4 representative for DBD, with a matching insert
			for(String vector : vectors.keySet()) {
				if (!isLexA(vector)) {
					for(String insert : keys(vectors.get(vector))) {
						if (line.getInsertionSite().equals(insert)) {
							return chooseInsert(vectors.get(vector), insert);
						}
					}
				}
			}
			// If not, try for any Gal4
			for(String vector : keys(vectors)) {
				if (!isLexA(vector)) {
					for(String insert : keys(vectors.get(vector))) {
						return chooseInsert(vectors.get(vector), insert);
					}
				}
			}
		}
		else {
			// Prefer a LexA/21 representative for AD lines
			for(String vector : keys(vectors)) {
				if (isLexA(vector)) {
					for(String insert : keys(vectors.get(vector))) {
						if ("21".equals(insert)) {
							return chooseInsert(vectors.get(vector), insert);
						}
					}
				}
			}
			
			// If not, try for a LexA with a matching insert
			for(String vector : keys(vectors)) {
				if (isLexA(vector)) {
					for(String insert : keys(vectors.get(vector))) {
						if (line.getInsertionSite().equals(insert)) {
							return chooseInsert(vectors.get(vector), insert);
						}
					}
				}
			}

			// If not, try for any LexA 
			for(String vector : keys(vectors)) {
				if (isLexA(vector)) {
					for(String insert : keys(vectors.get(vector))) {
						return chooseInsert(vectors.get(vector), insert);
					}
				}
			}

			// If not, try for a Gal4 with a matching insert
			for(String vector : keys(vectors)) {
				if (!isLexA(vector)) {
					for(String insert : keys(vectors.get(vector))) {
						if (line.getInsertionSite().equals(insert)) {
							return chooseInsert(vectors.get(vector), insert);
						}
					}
				}
			}

			// If not, try for any Gal4 
			for(String vector : keys(vectors)) {
				if (!isLexA(vector)) {
					for(String insert : keys(vectors.get(vector))) {
						return chooseInsert(vectors.get(vector), insert);
					}
				}
			}
		}

		return null;
    }
    
    /**
     * Does the given vector imply a LexA line?
     */
    private boolean isLexA(String vector) {
    	return vector.startsWith("L");
    }
    
    /**
     * Return the given insert from the map.  
     */
    private Entity chooseInsert(Map<String,Entity> inserts, String insert) {
		Entity rep = inserts.get(insert);
//		inserts.remove(insert);
		return rep;
    }
    
    /**
     * Returns a copy of the keySet for the given map, so that the map can be modified while being iterated.
     */
    private Set<String> keys(Map<String,? extends Object> map) {
    	return new HashSet<String>(map.keySet());
    }
    
    private Entity populateChildren(Entity entity) {
    	if (entity==null || EntityUtils.areLoaded(entity.getEntityData())) return entity;
		EntityUtils.replaceChildNodes(entity, entityBean.getChildEntities(entity.getId()));
		return entity;
    }
    
    public Entity createOrVerifyRootEntity(String topLevelFolderName, User user, Date createDate, boolean createIfNecessary, boolean loadTree) throws Exception {
        Set<Entity> topLevelFolders = entityBean.getEntitiesByName(topLevelFolderName);
        Entity topLevelFolder = null;
        if (topLevelFolders != null) {
            // Only accept the current user's top level folder
            for (Entity entity : topLevelFolders) {
                if (entity.getUser().getUserLogin().equals(user.getUserLogin())
                        && entity.getEntityType().getName().equals(entityBean.getEntityTypeByName(EntityConstants.TYPE_FOLDER).getName())
                        && entity.getAttributeByName(EntityConstants.ATTRIBUTE_COMMON_ROOT) != null) {
                    // This is the folder we want, now load the entire folder hierarchy
                    if (loadTree) {
                        topLevelFolder = entityBean.getEntityTree(entity.getId());
                    } else {
                        topLevelFolder = entity;
                    }
                    logger.info("Found existing topLevelFolder common root, name=" + topLevelFolder.getName());
                    break;
                }
            }
        }

        if (topLevelFolder == null && createIfNecessary) {
            logger.info("Creating new topLevelFolder with name=" + topLevelFolderName);
            topLevelFolder = new Entity();
            topLevelFolder.setCreationDate(createDate);
            topLevelFolder.setUpdatedDate(createDate);
            topLevelFolder.setUser(user);
            topLevelFolder.setName(topLevelFolderName);
            topLevelFolder.setEntityType(entityBean.getEntityTypeByName(EntityConstants.TYPE_FOLDER));
            topLevelFolder.addAttributeAsTag(EntityConstants.ATTRIBUTE_COMMON_ROOT);
            topLevelFolder = entityBean.saveOrUpdateEntity(topLevelFolder);
            logger.info("Saved top level folder as " + topLevelFolder.getId());
        }

        return topLevelFolder;
    }

    protected Entity verifyOrCreateChild(Entity parent, String childName, String type) throws Exception {

        logger.info("Looking for child entity "+childName+" in parent entity "+parent.getId());
        for (EntityData ed : parent.getEntityData()) {
            Entity child = ed.getChildEntity();
            if (child != null && child.getEntityType().getName().equals(type) && child.getName().equals(childName)) {
            	Entity folder = ed.getChildEntity();	
                logger.info("Found folder with id="+folder.getId());
                return folder;
            }
        }
    
        // We need to create a new folder
        Entity child = new Entity();
        child.setCreationDate(createDate);
        child.setUpdatedDate(createDate);
        child.setUser(user);
        child.setName(childName);
        child.setEntityType(entityBean.getEntityTypeByName(type));
        child = entityBean.saveOrUpdateEntity(child);
        logger.info("Saved child as "+child.getId());
        addToParent(parent, child, parent.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
    
        return child;
    }
    
    protected Entity addNewChildFolderToEntity(Entity parent, String name) throws Exception {
        Entity folder = new Entity();
        folder.setCreationDate(createDate);
        folder.setUpdatedDate(createDate);
        folder.setUser(user);
        folder.setName(name);
        folder.setEntityType(entityBean.getEntityTypeByName(EntityConstants.TYPE_FOLDER));
        folder = entityBean.saveOrUpdateEntity(folder);
        logger.info("Saved folder " + name+" as " + folder.getId()+" , will now add as child to parent entity name="+parent.getName()+" parentId="+parent.getId());
        addToParent(parent, folder, null, EntityConstants.ATTRIBUTE_ENTITY);
        return folder;
    }

    protected Entity createFlylineEntity(String entityName, String splitPart) throws Exception {
        Entity flyline = new Entity();
        flyline.setUser(user);
        flyline.setEntityType(entityBean.getEntityTypeByName(EntityConstants.TYPE_FLY_LINE));
        flyline.setCreationDate(createDate);
        flyline.setUpdatedDate(createDate);
        flyline.setName(entityName);
        if (splitPart!=null) {
        	flyline.setValueByAttributeName(EntityConstants.ATTRIBUTE_SPLIT_PART, splitPart);
        }
        flyline = entityBean.saveOrUpdateEntity(flyline);
        logger.info("Saved flyline " + flyline.getName() + " as "+flyline.getId());
        return flyline;
    }


    protected void addToParent(Entity parent, Entity entity, Integer index, String attrName) throws Exception {
        EntityData ed = parent.addChildEntity(entity, attrName);
        ed.setOrderIndex(index);
        entityBean.saveOrUpdateEntityData(ed);
        logger.info("Added "+entity.getEntityType().getName()+"#"+entity.getId()+
        		" as child of "+parent.getEntityType().getName()+"#"+parent.getId());
    }
}
