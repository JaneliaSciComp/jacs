package org.janelia.it.jacs.compute.service.fly;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.large.LargeOperations;
import org.janelia.it.jacs.compute.api.*;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.entity.EntityHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.EntityType;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.jacs.model.ontology.types.OntologyElementType;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * This service loads the "new" masks into the evaluation folder hierarchy, along with their MA annotations. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ScreenScoresLoadingService2 implements IService {
	
	public static final String SCORE_ONTOLOGY_NAME = "Expression Pattern Evaluation";

	public static final String MAA_INTENSITY_NAME = "MAA Intensity Score";
	public static final String MAA_DISTRIBUTION_NAME = "MAA Distribution Score";
	public static final String CA_INTENSITY_NAME = "CA Intensity Score";
	public static final String CA_DISTRIBUTION_NAME = "CA Distribution Score";
	
	public static final int MAX_SCORE = 5;
	
	private static final String maaUsername = "system";
	
    protected Logger logger;
    protected Task task;
    protected User user;
    protected Date createDate;
    protected EntityBeanLocal entityBean;
    protected ComputeBeanLocal computeBean;
    protected AnnotationBeanLocal annotationBean;
	protected EntityHelper helper;
    
    protected EntityType folderType;
    
    // The raw data
    protected Set<String> compartments = new LinkedHashSet<String>();

    // Lookup tables for ontology terms used in machine assisted annotation
    protected Entity maaIntensityEnum;
    protected Entity maaDistributionEnum;
    protected Map<Integer,Entity> intValueItems = new HashMap<Integer,Entity>();
    protected Map<Integer,Entity> distValueItems = new HashMap<Integer,Entity>();
    
	protected int numSamples;
	protected int numSamplesMissingData;
	protected int numAnnotationsCreated;
	
    public void execute(IProcessData processData) throws ServiceException {
    	
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            task = ProcessDataHelper.getTask(processData);
            entityBean = EJBFactory.getLocalEntityBean();
            computeBean = EJBFactory.getLocalComputeBean();
            annotationBean = EJBFactory.getLocalAnnotationBean();
            user = computeBean.getUserByName(ProcessDataHelper.getTask(processData).getOwner());
            createDate = new Date();
            helper = new EntityHelper(entityBean, computeBean, user);
            
            // Process arguments
            
            String topLevelFolderName = (String)processData.getItem("TOP_LEVEL_FOLDER_NAME");
        	if (topLevelFolderName == null) {
        		throw new IllegalArgumentException("TOP_LEVEL_FOLDER_NAME may not be null");
        	}
        	
        	// Preload entity types
        	
        	folderType = entityBean.getEntityTypeByName(EntityConstants.TYPE_FOLDER);
        	
        	// Create top level folder
        	Entity topLevelFolder = populateChildren(createOrVerifyRootEntity(topLevelFolderName, user, createDate, true, false));
        	
        	if (!topLevelFolder.getChildren().isEmpty()) {
        		throw new IllegalStateException("Cannot reuse existing top level folder, id="+topLevelFolder);
        	}
        	
        	// Process each screen sample and save off its expression scores for later use
        	
        	LargeOperations largeOp = new LargeOperations();
        	
        	for(Entity sample : entityBean.getEntitiesByTypeName(EntityConstants.TYPE_SCREEN_SAMPLE)) {
        		
        		logger.info("Processing "+sample);
        		
        		// First we read the scores from the score file
        		
        		populateChildren(sample);
        		List<Entity> stacks = sample.getChildrenOfType(EntityConstants.TYPE_ALIGNED_BRAIN_STACK);
        		if (stacks.isEmpty()) continue;
        		if (stacks.size()>1) {
        			logger.warn("More than one aligned brain stack for "+sample.getName()+"");
        		}
        		Entity stack = stacks.get(0);
        		String stackFilepath = stack.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        		String scoreFilepath = stackFilepath.substring(0,stackFilepath.indexOf("reg.local"))+"arnimScoreOutput";
        		
        		Map<String,Score> scores = readScoresFile(new File(scoreFilepath));
        		
        		numSamples++;
        		if (scores == null) {
        			logger.info("  missing data");
        			numSamplesMissingData++;
        			continue;
        		}
        		
        		Entity patternAnnotation = EntityUtils.findChildWithName(sample, "Pattern Annotation");
        		if (patternAnnotation==null) {
        			logger.warn("Sample "+sample.getName()+" has no Pattern Annotation folder");
        			continue;
        		}
        		
        		// We need to get all the individual mask images for the sample. This child set might contain extra 
        		// stuff we don't care about, but it will get filtered by the score map in the loop below
        		Map<Long,String> maskMap = entityBean.getChildEntityNames(patternAnnotation.getId());        		
        		for(Map.Entry<Long, String> entry : maskMap.entrySet()) {
        			Score score = scores.get(entry.getValue());
        			if (score==null) continue;
        			score.compartmentId = entry.getKey();
        		}
        		
        		// Now go through the scores for this sample, and hash them into the disk-based map for later use
        		for(String compartment : scores.keySet()) {
        			Score score = scores.get(compartment);
        			if (score.compartmentId==null) continue;
        			String key = compartment+"/"+score.intensity+"/"+score.distribution;
        			List<Long> sampleCompIds = (List<Long>)largeOp.getValue(LargeOperations.SCREEN_SCORE_MAP, key);
        			if (sampleCompIds==null) {
        				sampleCompIds = new ArrayList<Long>();
        			}
        			sampleCompIds.add(score.compartmentId);
        			largeOp.putValue(LargeOperations.SCREEN_SCORE_MAP,key,sampleCompIds);
        		}
        		
        		logger.info("  processed "+scores.size()+" compartments");
        	}
        	
        	// Get or create score ontology

        	Entity ontologyTree = null;
        		
        	Set<Entity> matchingOntologies = entityBean.getUserEntitiesByName(user.getUserLogin(), SCORE_ONTOLOGY_NAME);
        	
        	if (matchingOntologies!=null && !matchingOntologies.isEmpty()) {
        		//ontologyTree = matchingOntologies.iterator().next();
        		//ontologyTree = annotationBean.getOntologyTree(user.getUserLogin(), ontologyTree.getId());
        		throw new Exception("Reusing an existing ontology is not yet supported. Delete the ontology first.");
        	}

    		logger.info("Creating ontology called '"+SCORE_ONTOLOGY_NAME+"'");
    		
        	ontologyTree = annotationBean.createOntologyRoot(user.getUserLogin(), SCORE_ONTOLOGY_NAME);

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
    		
        	// Create the folder structure and annotate each sample
        	
        	logger.info("Creating folder structure under "+topLevelFolderName);

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
    
    private void annotate(Long targetId, Entity key, Entity value) throws ComputeException {
		OntologyAnnotation annotation = new OntologyAnnotation(null, targetId, key.getId(), key.getName(), value.getId(), value.getName());
		annotationBean.createSilentOntologyAnnotation(maaUsername, annotation);
		numAnnotationsCreated++;
    }
    
    protected Entity newTerm(Entity parent, String name, OntologyElementType type) throws ComputeException {
    	EntityData ed = annotationBean.createOntologyTerm(user.getUserLogin(), parent.getId(), name, type, parent.getMaxOrderIndex()+1);
    	parent.getEntityData().add(ed);
    	return ed.getChildEntity();
    }

    protected Entity newTerm(Entity parent, String name, String type) throws ComputeException {
    	return newTerm(parent, name, OntologyElementType.createTypeByName(type));
    }
    
    private Map<String,Score> readScoresFile(File scoresFile) throws Exception {
    	
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

    private class Score {
    	Long compartmentId;
    	Integer distribution;
    	Integer intensity;    	
    }

    private String getCol(String[] cols, int index) {
    	if (index > cols.length-1) return null;
    	return cols[index];
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
                        && entity.getEntityType().getName().equals(folderType.getName())
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
            topLevelFolder.setEntityType(folderType);
            topLevelFolder.addAttributeAsTag(EntityConstants.ATTRIBUTE_COMMON_ROOT);
            topLevelFolder = entityBean.saveOrUpdateEntity(topLevelFolder);
            logger.info("Saved top level folder as " + topLevelFolder.getId());
        }

        return topLevelFolder;
    }

    protected Entity verifyOrCreateChildFolder(Entity parent, String childName) throws Exception {

        logger.info("Looking for child entity "+childName+" in parent entity "+parent.getId());
        for (EntityData ed : parent.getEntityData()) {
            Entity child = ed.getChildEntity();
            if (child != null && child.getEntityType().getName().equals(folderType.getName()) && child.getName().equals(childName)) {
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
        child.setEntityType(folderType);
        child = entityBean.saveOrUpdateEntity(child);
        logger.info("Saved child as "+child.getId());
        addToParent(parent, child, parent.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
    
        return child;
    }

    protected void addToParent(Entity parent, Entity entity, Integer index, String attrName) throws Exception {
        EntityData ed = parent.addChildEntity(entity, attrName);
        ed.setOrderIndex(index);
        entityBean.saveOrUpdateEntityData(ed);
        logger.info("Added "+entity.getEntityType().getName()+"#"+entity.getId()+
        		" as child of "+parent.getEntityType().getName()+"#"+parent.getId());
    }
}
