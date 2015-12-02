package org.janelia.it.jacs.compute.service.fly;

import org.janelia.it.jacs.compute.access.large.LargeOperations;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.compute.service.fileDiscovery.FileDiscoveryHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.jacs.model.ontology.types.OntologyElementType;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.*;

/**
 * This service loads screen scores from *.arnimScoreOutput files generated by Sean's Perl scripts, and organizes the 
 * screen samples into intensity and distribution score folders. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ScreenScoresLoadingService extends AbstractEntityService {
	
	private static final boolean DEBUG = false;
	
	public static final String TOP_LEVEL_EVALUATION_FOLDER = "FlyLight Pattern Evaluation";
	public static final String SCORE_ONTOLOGY_NAME = "Expression Pattern Evaluation";

	public static final String MAA_USERNAME = User.SYSTEM_USER_LOGIN;
	public static final String MAA_INTENSITY_NAME = "MAA Intensity Score";
	public static final String MAA_DISTRIBUTION_NAME = "MAA Distribution Score";
	public static final String CA_INTENSITY_NAME = "CA Intensity Score";
	public static final String CA_DISTRIBUTION_NAME = "CA Distribution Score";
	
	public static final int MAX_SCORE = 5;
	
    protected Date createDate;
	protected FileDiscoveryHelper helper;
    
    // The raw data
    protected Set<String> compartments = new LinkedHashSet<String>();
    
    // Lookup tables for ontology terms used in machine assisted annotation
    protected Entity maaIntensityEnum;
    protected Entity maaDistributionEnum;
    protected Map<Integer,Entity> intValueItems = new HashMap<Integer,Entity>();
    protected Map<Integer,Entity> distValueItems = new HashMap<Integer,Entity>();
    
    protected Set<Long> maskIdsNeedingAnnotations = new HashSet<Long>();
    protected SortedSet<String> loaded = new TreeSet<String>();
    
	protected int numSamples;
	protected int numSamplesMissingData;
	protected int numAnnotationsCreated;
	protected int numAnnotatedMasks;
	
    public void execute() throws Exception {
    	
        createDate = new Date();
        helper = new FileDiscoveryHelper(entityBean, computeBean, ownerKey, logger);
        
        String acceptsFilepath = (String)processData.getItem("ACCEPTS_FILE_PATH");
    	if (acceptsFilepath == null) {
    		throw new IllegalArgumentException("ACCEPTS_FILE_PATH may not be null");
    	}

        String outputFilepath = (String)processData.getItem("LOADED_FILE_PATH");
    	if (outputFilepath == null) {
    		throw new IllegalArgumentException("LOADED_FILE_PATH may not be null");
    	}
    	
    	File outputFile = new File(outputFilepath);
    	if (!outputFile.getParentFile().canWrite()) {
    		throw new IllegalArgumentException("Cannot write to output file: "+outputFilepath);
    	}
    	
    	// Read input file
    	Set<String> accepted = readNameFile(new File(acceptsFilepath));
    	
    	// Create top level folder
    	Entity topLevelFolder = populateChildren(createOrVerifyRootEntity(TOP_LEVEL_EVALUATION_FOLDER, ownerKey, createDate, true, false));
    	
    	if (!topLevelFolder.getChildren().isEmpty()) {
    		throw new IllegalStateException("Cannot reuse existing top level folder, id="+topLevelFolder);
    	}
    	
    	// Process each screen sample and save off its expression scores for later use
    	
    	LargeOperations largeOp = new LargeOperations();
    	
    	for(Entity sample : entityBean.getEntitiesByTypeName(EntityConstants.TYPE_SCREEN_SAMPLE)) {

    		Specimen specimen = Specimen.createSpecimenFromFullName(sample.getName());
    		if (!accepted.contains(specimen.getSpecimenName())) continue;
    		
    		logger.info("Processing "+sample);
    		
    		// Don't read the score files until we need them
    		Map<String,Score> sampleScores = new HashMap<String,Score>();
    		boolean sampleScoresLoaded = false;
    		
    		// Have to read at least one score file set to get all the compartment names
    		if (compartments.isEmpty()) {
				sampleScoresLoaded = true;
				sampleScores.putAll(getSampleScores(sample));
    		}
    		
    		numSamples++;        		
    		
    		// We need to get all the individual mask images for the sample. This child set might contain extra 
    		// stuff we don't care about, but it will get filtered by the score map in the loop below
    		Map<Long,String> masks = getSampleMaskImages(sample);        		

    		// Get annotations for all mask images
    		Map<Long,List<OntologyAnnotation>> annotMap = getAnnotationMap(masks.keySet());
    		
    		for(Long maskId : masks.keySet()) {
    			String maskName = masks.get(maskId);
    			
    			if (DEBUG) logger.info("  Processing "+maskName);
    			
    			List<OntologyAnnotation> annots = annotMap.get(maskId);
    			
				String maaIntensity = null;
				String maaDistribution = null;
				String caIntensity = null;
				String caDistribution = null;
				
				if (annots!=null) {
					for(OntologyAnnotation annotation : annots) {
						if (ScreenScoresLoadingService.MAA_INTENSITY_NAME.equals(annotation.getKeyString())) {
							maaIntensity = annotation.getValueString();
						}
						else if (ScreenScoresLoadingService.MAA_DISTRIBUTION_NAME.equals(annotation.getKeyString())) {
							maaDistribution = annotation.getValueString();
						}
						else if (ScreenScoresLoadingService.CA_INTENSITY_NAME.equals(annotation.getKeyString())) {
							caIntensity = annotation.getValueString();
						}
						else if (ScreenScoresLoadingService.CA_DISTRIBUTION_NAME.equals(annotation.getKeyString())) {
							caDistribution = annotation.getValueString();
						}
					}	
				}
    			
				if (!StringUtils.isEmpty(maaIntensity) && !StringUtils.isEmpty(maaDistribution)) {
					// The current evaluation
					int mi = getValueFromAnnotation(maaIntensity);
					int md = getValueFromAnnotation(maaDistribution);
					int fi = mi;
					int fd = md;
					
					if (!StringUtils.isEmpty(caIntensity)) {
						fi = getValueFromAnnotation(caIntensity);
					}
					
					if (!StringUtils.isEmpty(caDistribution)) {
						fd = getValueFromAnnotation(caDistribution);
					}
					
					Score score = new Score();
					score.maskId = maskId;
					score.intensity = fi;
        			score.distribution = fd;
        			sampleScores.put(maskName,score);
        			
        			if (DEBUG) logger.info("    Already annotated: i"+score.intensity+"/d"+score.distribution);
				}
				else {
					// No annotations yet
        			maskIdsNeedingAnnotations.add(maskId);
        			
        			// Lazy load the scores
					if (!sampleScoresLoaded) {
						sampleScoresLoaded = true;
						Map<String,Score> savedScores = new HashMap<String,Score>(sampleScores);
						sampleScores.putAll(getSampleScores(sample));
						sampleScores.putAll(savedScores); // in case we already had some in there
					}
					
					// Update our mask id
        			Score score = sampleScores.get(maskName);
        			if (score==null) continue;
        			score.maskId = maskId;
					
        			if (DEBUG) logger.info("    Needs annotation: i"+score.intensity+"/d"+score.distribution);
				}
    		}
    		
    		// Now go through the scores for this sample, and hash them into the disk-based map for later use
    		for(String maskName : sampleScores.keySet()) {
    			Score score = sampleScores.get(maskName);
    			if (score==null || score.maskId==null) continue;
    			String key = maskName+"/"+score.intensity+"/"+score.distribution;
    			List<Long> sampleCompIds = (List<Long>)largeOp.getValue(LargeOperations.SCREEN_SCORE_MAP, key);
    			if (sampleCompIds==null) {
    				sampleCompIds = new ArrayList<Long>();
    			}
    			sampleCompIds.add(score.maskId);
    			largeOp.putValue(LargeOperations.SCREEN_SCORE_MAP,key,sampleCompIds);
    		}
    		
    		if (!sampleScores.isEmpty()) {
    			loaded.add(specimen.getSpecimenName());
    		}
    		logger.info("  Processed "+sampleScores.size()+" compartments");
    	}
    	
    	// Get or create score ontology
    	getOrCreateOntology();
		
    	// Create the folder structure and annotate each sample
    	
    	logger.info("Creating folder structure under "+TOP_LEVEL_EVALUATION_FOLDER);

    	for(final String compartment : compartments) {
    		logger.info("Processing compartment "+compartment);
    		Entity compartmentFolder = verifyOrCreateChildFolder(topLevelFolder, compartment);

        	for(int i=MAX_SCORE; i>=0; i--) {
        		logger.info("  Processing intensity "+i);
        		Entity intValueTerm = intValueItems.get(i);
        		Entity intValueFolder = verifyOrCreateChildFolder(compartmentFolder, "Intensity "+i);
        		
            	for(int d=MAX_SCORE; d>=0; d--) {
            		logger.info("  Processing distribution "+d);	
            		Entity distValueTerm = distValueItems.get(d);
            		Entity distValueFolder = verifyOrCreateChildFolder(intValueFolder, "Distribution "+d);
            		
            		String key = compartment+"/"+i+"/"+d;
            		List<Long> maskIds = (List<Long>)largeOp.getValue(LargeOperations.SCREEN_SCORE_MAP, key);
            		if (maskIds!=null) {
	            		logger.info("    Sample count: "+maskIds.size());
	            		
	            		entityBean.addChildren(ownerKey, distValueFolder.getId(), maskIds, EntityConstants.ATTRIBUTE_ENTITY);
	            		
	            		for(Long maskId : maskIds) {
		            		if (maskIdsNeedingAnnotations.contains(maskId)) {
		            			annotate(maskId, maaIntensityEnum, intValueTerm);
		            			annotate(maskId, maaDistributionEnum, distValueTerm);
		            			numAnnotatedMasks++;
		            		}
	            		}
            		}
            	}
        	}
    	}
    	
    	logger.info("Processed "+numSamples+" samples, loaded "+loaded.size()+". "+
    			numAnnotationsCreated+" annotations were created. "+numAnnotatedMasks+" mask images were annotated.");

    	logger.info("Writing output to "+outputFile.getAbsolutePath());
    	
    	FileWriter writer = new FileWriter(outputFile);
    	
    	for(String name : loaded) {
    		writer.write(name+"\n");
    	}
    	writer.close();
    }
    
    protected Map<String,Score> getSampleScores(Entity sample) throws Exception {

    	Map<String,Score> sampleScores = new HashMap<String,Score>();
    	
		populateChildren(sample);
		List<Entity> stacks = EntityUtils.getChildrenOfType(sample, EntityConstants.TYPE_ALIGNED_BRAIN_STACK);
		if (stacks.isEmpty()) return sampleScores;
		if (stacks.size()>1) {
			logger.warn("More than one aligned brain stack for "+sample.getName()+"");
		}
		
		// Read main score file
		
		Entity stack = stacks.get(0);
		String stackFilepath = stack.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
		String scoreFilepath = stackFilepath.substring(0,stackFilepath.indexOf("reg.local"))+"arnimScoreOutput";
		
		Map<String,Score> mainScores = readScoresFile(new File(scoreFilepath));
		
		if (mainScores == null) {
			logger.info("  missing score file");
			return sampleScores;
		}

		sampleScores.putAll(mainScores);
		
		// Read Arnim updates
		
		populateChildren(sample);
		
		Entity patternAnnotation = EntityUtils.findChildWithName(sample, "Pattern Annotation");
		if (patternAnnotation==null) {
			logger.warn("  missing Pattern Annotation folder");
			return sampleScores;
		}
		
		Entity maskAnnotation = EntityUtils.findChildWithName(sample, "Mask Annotation");
		if (maskAnnotation!=null) {
			populateChildren(maskAnnotation);
			for(Entity updateFolder : maskAnnotation.getOrderedChildren()) {
				if (updateFolder.getName().startsWith("ArnimUpdate")) {

	        		populateChildren(updateFolder);
	        		Entity suppFiles = EntityUtils.findChildWithName(updateFolder, "supportingFiles");
	        		if (suppFiles==null) {
	        			logger.warn("  missing supportingFiles folder");
	        			continue;
	        		}

	        		populateChildren(suppFiles);
	        		Entity scoreFile = EntityUtils.findChildWithName(suppFiles, "arnimScores.txt");
	        		if (scoreFile==null) {
	        			logger.warn("  missing arnimScores.txt");
	        			continue;
	        		}
	        	
	        		String updateScoreFilepath = scoreFile.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
	        		Map<String,Score> updateScores = readScoresFile(new File(updateScoreFilepath));

	        		if (updateScores == null) {
	        			logger.info("  missing update score file");
	        			continue;
	        		}
	        		
        			sampleScores.putAll(updateScores);	
				}	
			}
		}
		
		return sampleScores;
    }
    
    protected void getOrCreateOntology() throws Exception {

		Set<Entity> matching = entityBean.getUserEntitiesByName(ownerKey, SCORE_ONTOLOGY_NAME);
		if (matching.size()>1) {
			throw new Exception("More than one ontology named "+SCORE_ONTOLOGY_NAME);
		}
		
		if (matching.isEmpty()) {
    		logger.info("Creating ontology called '"+SCORE_ONTOLOGY_NAME+"'");
    		
    		Entity ontologyTree = annotationBean.createOntologyRoot(ownerKey, SCORE_ONTOLOGY_NAME);

        	maaIntensityEnum = newTerm(ontologyTree, MAA_INTENSITY_NAME, "Enum");
        	maaDistributionEnum = newTerm(ontologyTree, MAA_DISTRIBUTION_NAME, "Enum");
        	Entity caIntensityEnum = newTerm(ontologyTree, CA_INTENSITY_NAME, "Enum");
        	Entity caDistributionEnum = newTerm(ontologyTree, CA_DISTRIBUTION_NAME, "Enum");
        	
        	for(int i=MAX_SCORE; i>=0; i--) {
        		Entity intTerm = newTerm(maaIntensityEnum, "i"+i, "EnumItem");
        		Entity distTerm = newTerm(maaDistributionEnum, "d"+i, "EnumItem");
        		intValueItems.put(i, intTerm);
        		distValueItems.put(i, distTerm);
        		newTerm(caIntensityEnum, "i"+i, "EnumItem");
        		newTerm(caDistributionEnum, "d"+i, "EnumItem");
        	}
		}
		else {
			long ontologyId = matching.iterator().next().getId();
			logger.info("Reusing existing ontology called '"+SCORE_ONTOLOGY_NAME+"' (id="+ontologyId+")");
			
			Entity ontologyTree = entityBean.getEntityTree(ownerKey, ontologyId);

			maaIntensityEnum = EntityUtils.findChildWithName(ontologyTree, ScreenScoresLoadingService.MAA_INTENSITY_NAME);
			maaDistributionEnum = EntityUtils.findChildWithName(ontologyTree, ScreenScoresLoadingService.MAA_DISTRIBUTION_NAME);
			
	    	for(int i=ScreenScoresLoadingService.MAX_SCORE; i>=0; i--) {
	    		intValueItems.put(i, EntityUtils.findChildWithName(maaIntensityEnum, "i"+i));
	    		distValueItems.put(i, EntityUtils.findChildWithName(maaDistributionEnum, "d"+i));
	    	}
		}
    }
    
    protected void annotate(Long targetId, Entity key, Entity value) throws ComputeException {
		OntologyAnnotation annotation = new OntologyAnnotation(null, targetId, key.getId(), key.getName(), value.getId(), value.getName());
		annotationBean.createSilentOntologyAnnotation(MAA_USERNAME, annotation);
		numAnnotationsCreated++;
    }
    
    protected Entity newTerm(Entity parent, String name, OntologyElementType type) throws ComputeException {
    	EntityData ed = annotationBean.createOntologyTerm(ownerKey, parent.getId(), name, type, parent.getMaxOrderIndex()+1);
    	parent.getEntityData().add(ed);
    	return ed.getChildEntity();
    }

    protected Entity newTerm(Entity parent, String name, String type) throws ComputeException {
    	return newTerm(parent, name, OntologyElementType.createTypeByName(type));
    }
    
    protected Map<String,Score> readScoresFile(File scoresFile) throws Exception {
    	
    	Map<String,Score> scores = new HashMap<String,Score>();
    	
    	// The 5 numbers in these files for each compartment should be interpreted as:
        // <intensity 40-threshold> <intensity 10-threshold> <intensity 0-threshold> <distribution-medium-slope> <distribution no-slope>    	
    	// The "official" intensity and distribution values are defined as:
    	// Intensity = <intensity 40-threshold>
    	// Distribution = <distribution medium-slope>
    	// I.e., we should be using the 1st and 4th of the 5 values.
    	
		Scanner scanner = null;
        try {
        	scanner = new Scanner(scoresFile);
            while (scanner.hasNextLine()){
            	
                String[] parts = scanner.nextLine().split(" ");
                int c = 0;
                String num = getCol(parts, c++);
                String compartment = getCol(parts, c++);
                String int40Threshold = getCol(parts, c++);
                String int10Threshold = getCol(parts, c++);
                String int0Threshold = getCol(parts, c++);
                String distMediumSlope = getCol(parts, c++);
                String distNoSlope = getCol(parts, c++);
                Score score = new Score();
            	score.intensity = new Integer(int40Threshold);	
                score.distribution = new Integer(distMediumSlope);
                scores.put(compartment,score);
                compartments.add(compartment);
            }
        }
        catch (FileNotFoundException e) {
        	return null;
        }
        catch (Exception e) {
        	logger.warn("Problem reading score file: "+scoresFile.getAbsolutePath());
        	return scores;
        }
        finally {
        	if (scanner!=null) scanner.close();
        }
        
        return scores;
    }

    protected class Score {
    	Long maskId;
    	Integer distribution;
    	Integer intensity; 
    }

    protected String getCol(String[] cols, int index) {
    	if (index > cols.length-1) return null;
    	return cols[index];
    }
    
    protected Map<Long,String> getSampleMaskImages(Entity sample) throws ComputeException {

    	Map<Long,String> childNames = new HashMap<Long,String>();

		populateChildren(sample);
		Entity patternAnnotation = EntityUtils.findChildWithName(sample, "Pattern Annotation");
		if (patternAnnotation!=null) {
			childNames.putAll(entityBean.getChildEntityNames(patternAnnotation.getId()));
		}
		
		Entity maskAnnotation = EntityUtils.findChildWithName(sample, "Mask Annotation");
		if (maskAnnotation!=null) {
			populateChildren(maskAnnotation);
			for(Entity updateFolder : maskAnnotation.getOrderedChildren()) {
				if (updateFolder.getName().startsWith("ArnimUpdate")) {
					childNames.putAll(entityBean.getChildEntityNames(updateFolder.getId()));
				}
			}
		}
		
		return childNames;
    }
    
    protected Map<Long,List<OntologyAnnotation>> getAnnotationMap(Collection<Long> entityIds) throws Exception {
		List<Long> maskIds = new ArrayList<Long>(entityIds);
		Map<Long,List<OntologyAnnotation>> annotMap = new HashMap<Long,List<OntologyAnnotation>>();
		for(Entity annotEntity : annotationBean.getAnnotationsForEntities(ownerKey, maskIds)) {
			OntologyAnnotation annototation = new OntologyAnnotation();
			annototation.init(annotEntity);
			List<OntologyAnnotation> entityAnnots = annotMap.get(annototation.getTargetEntityId());
			if (entityAnnots==null) {
				entityAnnots = new ArrayList<OntologyAnnotation>();
				annotMap.put(annototation.getTargetEntityId(), entityAnnots);
			}
			entityAnnots.add(annototation);
		}
		return annotMap;
    }

    protected Set<String> readNameFile(File nameFile) throws Exception {
    	Set<String> names = new HashSet<String>();
		Scanner scanner = new Scanner(nameFile);
        try {
            while (scanner.hasNextLine()){
            	names.add(scanner.nextLine());
            }
        }
        finally {
        	scanner.close();
        }
        return names;
    }

    protected int getValueFromAnnotation(String annotationValue) {
		return Integer.parseInt(""+annotationValue.charAt(1));
	}
	
    protected int getValueFromFolderName(Entity entity) {
		return getValueFromFolderName(entity.getName());
	}
	
    protected int getValueFromFolderName(String folderName) {
		try {
			return Integer.parseInt(""+folderName.charAt(folderName.length()-1));
		}
		catch (Exception e) {
			logger.error("Error parsing folder name "+folderName+": "+e.getMessage());
		}
		return -1;
	}
    
    protected Entity populateChildren(Entity entity) throws ComputeException {
    	if (entity==null || EntityUtils.areLoaded(entity.getEntityData())) return entity;
		EntityUtils.replaceChildNodes(entity, entityBean.getChildEntities(entity.getId()));
		return entity;
    }
    
    protected Entity createOrVerifyRootEntity(String topLevelFolderName, String ownerKey, Date createDate, boolean createIfNecessary, boolean loadTree) throws Exception {
        Set<Entity> topLevelFolders = entityBean.getEntitiesByName(topLevelFolderName);
        Entity topLevelFolder = null;
        if (topLevelFolders != null) {
            // Only accept the current user's top level folder
            for (Entity entity : topLevelFolders) {
                if (entity.getOwnerKey().equals(ownerKey)
                        && entity.getEntityTypeName().equals(EntityConstants.TYPE_FOLDER)
                        && entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_COMMON_ROOT) != null) {
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
            topLevelFolder = entityBean.createFolderInDefaultWorkspace(ownerKey, topLevelFolderName).getChildEntity();
            logger.info("Saved top level folder as " + topLevelFolder.getId());
        }

        return topLevelFolder;
    }

    protected Entity verifyOrCreateChildFolder(Entity parent, String childName) throws Exception {

        logger.info("Looking for child entity "+childName+" in parent entity "+parent.getId());
        for (EntityData ed : parent.getEntityData()) {
            Entity child = ed.getChildEntity();
            if (child != null && child.getEntityTypeName().equals(EntityConstants.TYPE_FOLDER) && child.getName().equals(childName)) {
            	Entity folder = ed.getChildEntity();	
                logger.info("Found folder with id="+folder.getId());
                return folder;
            }
        }
    
        // We need to create a new folder
        Entity child = new Entity();
        child.setCreationDate(createDate);
        child.setUpdatedDate(createDate);
        child.setOwnerKey(ownerKey);
        child.setName(childName);
        child.setEntityTypeName(EntityConstants.TYPE_FOLDER);
        child = entityBean.saveOrUpdateEntity(child);
        logger.info("Saved child as "+child.getId());
        addToParent(parent, child, parent.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
    
        return child;
    }

    protected void addToParent(Entity parent, Entity entity, Integer index, String attrName) throws Exception {
        entityBean.addEntityToParent(parent, entity, index, EntityConstants.ATTRIBUTE_ENTITY);
        logger.info("Added "+entity.getEntityTypeName()+"#"+entity.getId()+
        		" as child of "+parent.getEntityTypeName()+"#"+parent.getId());
    }
}
