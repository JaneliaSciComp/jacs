package org.janelia.it.jacs.compute.service.fileDiscovery;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.neuronSeparator.NeuronSeparatorHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.fileDiscovery.MultiColorFlipOutFileDiscoveryTask;
import org.janelia.it.jacs.model.tasks.neuronSeparator.NeuronSeparatorPipelineTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 5/24/11
 * Time: 12:23 PM
 */
public class MultiColorFlipOutFileDiscoveryService implements IService {

    private static final String TOP_LEVEL_FOLDER_NAME_PARAM = "TOP_LEVEL_FOLDER_NAME";
    private static final String DIRECTORY_PARAM_PREFIX = "DIRECTORY_";
    private static final String LINKING_FOLDER = "LINKING_FOLDER";
    
    private enum NeuronSepMode {
    	LOCAL,
    	REMOTE,
    	GRID
    }
    
    private NeuronSepMode mode = NeuronSepMode.REMOTE;
    
    private String linkingDir;
    private boolean refresh = true;
    private boolean runNeuronSeperator = true;
    
    private Logger logger;
    private MultiColorFlipOutFileDiscoveryTask task;
    private AnnotationBeanLocal annotationBean;
    private ComputeBeanLocal computeBean;
    private User user;
    private Date createDate;
    
    private List<String> directoryPathList = new ArrayList<String>();

    private class LsmPair {
        public LsmPair() {}
        public String name;
        public Integer lowIndex;
        public File lsmFile1;
        public File lsmFile2;
        public Entity lsmEntity1;
        public Entity lsmEntity2;
    }
    
    public void execute(IProcessData processData) {
        try {
        	
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            task = (MultiColorFlipOutFileDiscoveryTask) ProcessDataHelper.getTask(processData);
            annotationBean = EJBFactory.getLocalAnnotationBean();
            computeBean = EJBFactory.getLocalComputeBean();
            user = computeBean.getUserByName(task.getOwner());
            createDate = new Date();
            refresh = "true".equals(task.getParameter(MultiColorFlipOutFileDiscoveryTask.PARAM_refresh));
            
            String topLevelFolderName = null;
            
            Set<Map.Entry<String, Object>> entrySet = processData.entrySet();
            for (Map.Entry<String, Object> entry : entrySet) {
                String paramName = entry.getKey();
                if (paramName.startsWith(DIRECTORY_PARAM_PREFIX)) {
                    directoryPathList.add((String) entry.getValue());
                } 
                else if (paramName.equals(TOP_LEVEL_FOLDER_NAME_PARAM)) {
                    topLevelFolderName = (String)entry.getValue();
                }
                else if (paramName.equals(LINKING_FOLDER)) {
                	linkingDir = (String)entry.getValue();
                	linkingDir = linkingDir.replaceAll("%USER%", user.getUserLogin());
                }
            }

            if (topLevelFolderName == null) {
            	throw new Exception("No TOP_LEVEL_FOLDER_NAME parameter provided");
            }
            
            String taskInputDirectoryList = task.getParameter(MultiColorFlipOutFileDiscoveryTask.PARAM_inputDirectoryList);
            if (taskInputDirectoryList != null) {
                String[] directoryArray = taskInputDirectoryList.split(",");
                for (String d : directoryArray) {
                    String trimmedPath=d.trim();
                    if (trimmedPath.length()>0) {
                        directoryPathList.add(trimmedPath);
                    }
                }
            }

            if (directoryPathList.isEmpty()) {
            	throw new Exception("No input directories provided");
            }
            
            for (String directoryPath : directoryPathList) {
                logger.info("MultiColorFlipOutFileDiscoveryService including directory = "+directoryPath);
            }
            
            Entity topLevelFolder = createOrVerifyRootEntity(topLevelFolderName);
            processDirectoriesForLsm(topLevelFolder);
            
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }
    }

    protected Entity createOrVerifyRootEntity(String topLevelFolderName) throws Exception {
    	
        Set<Entity> topLevelFolders = annotationBean.getEntitiesByName(topLevelFolderName);

    	Entity topLevelFolder = null;
        if (topLevelFolders!=null) {
        	// Only accept the current user's top level folder
	        for(Entity entity : topLevelFolders) {
	        	if (entity.getUser().getUserLogin().equals(user.getUserLogin())) {
	                // This is the folder we want, now load the entire folder hierarchy
	                topLevelFolder = annotationBean.getFolderTree(entity.getId());
	                logger.info("Found existing topLevelFolder, name=" + topLevelFolder.getName());
	        		break;
	        	}
	        }
        }
         
        if (topLevelFolder == null) {
            logger.info("Creating new topLevelFolder with name="+topLevelFolderName);
            topLevelFolder = new Entity();
            topLevelFolder.setCreationDate(createDate);
            topLevelFolder.setUpdatedDate(createDate);
            topLevelFolder.setUser(user);
            topLevelFolder.setName(topLevelFolderName);
            topLevelFolder.setEntityType(annotationBean.getEntityTypeByName(EntityConstants.TYPE_FOLDER));
            topLevelFolder.addAttributeAsTag(EntityConstants.ATTRIBUTE_COMMON_ROOT);
            topLevelFolder = annotationBean.saveOrUpdateEntity(topLevelFolder);
            logger.info("Saved top level folder as "+topLevelFolder.getId());
        }
        
        logger.info("Using topLevelFolder with id="+topLevelFolder.getId());
        return topLevelFolder;
    }

    protected void processDirectoriesForLsm(Entity topLevelFolder) throws Exception {
        for (String directoryPath : directoryPathList) {
            logger.info("Processing dir="+directoryPath);
            File dir = new File(directoryPath);
            if (!dir.exists()) {
                logger.error("Directory "+dir.getAbsolutePath()+" does not exist - skipping");
            }
            else if (!dir.isDirectory()) {
                logger.error(("File " + dir.getAbsolutePath()+ " is not a directory - skipping"));
            } 
            else {
                Entity folder = verifyOrCreateChildFolderFromDir(topLevelFolder, dir);
                processFolderForLsm(folder);
            }
        }
    }

    protected Entity verifyOrCreateChildFolderFromDir(Entity parentFolder, File dir) throws Exception {

        logger.info("Looking for folder entity with path "+dir.getAbsolutePath()+" in parent folder "+parentFolder.getId());
        Entity folder = null;
        
        for (EntityData ed : parentFolder.getEntityData()) {
            Entity child = ed.getChildEntity();
        	
            if (child != null && child.getEntityType().getName().equals(EntityConstants.TYPE_FOLDER)) {
                String folderPath = child.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
                if (folderPath == null) {
                    logger.warn("Unexpectedly could not find attribute '"+EntityConstants.ATTRIBUTE_FILE_PATH+"' for entity id="+child.getId());
                }
                else if (folderPath.equals(dir.getAbsolutePath())) {
                    if (folder != null) {
                    	logger.warn("Unexpectedly found multiple child folders with path=" + dir.getAbsolutePath()+" for parent folder id="+parentFolder.getId());
                    }
                    else {
                    	folder = ed.getChildEntity();	
                    }
                }
            }
        }
        
        if (folder == null) {
            // We need to create a new folder
            folder = new Entity();
            folder.setCreationDate(createDate);
            folder.setUpdatedDate(createDate);
            folder.setUser(user);
            folder.setName(dir.getName());
            folder.setEntityType(annotationBean.getEntityTypeByName(EntityConstants.TYPE_FOLDER));
            folder.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, dir.getAbsolutePath());
            folder = annotationBean.saveOrUpdateEntity(folder);
            logger.info("Saved folder as "+folder.getId());
            NeuronSeparatorHelper.addToParent(parentFolder, folder, null, EntityConstants.ATTRIBUTE_ENTITY);
        }
        else {
            logger.info("Found folder with id="+folder.getId());
        }
        
        return folder;
    }

    protected void processFolderForLsm(Entity folder) throws Exception {
    	
        File dir = new File(folder.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
        logger.info("Processing folder="+dir.getAbsolutePath()+" id="+folder.getId());
        
        if (!dir.canRead()) {
        	logger.info("Cannot read from folder "+dir.getAbsolutePath());
        	return;
        }
        
        List<File> lsmFileList = new ArrayList<File>();
        
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                Entity subfolder = verifyOrCreateChildFolderFromDir(folder, file);
                processFolderForLsm(subfolder);
            } 
            else {
                if (file.getName().toUpperCase().endsWith(".LSM")) {
                	lsmFileList.add(file);
                }
            }
        }
        
        processLsmStacksIntoSamples(folder, lsmFileList);
    }

    private void processLsmStacksIntoSamples(Entity folder, List<File> lsmStackList) throws Exception {

        logger.info("Checking for LSM pairs in folder "+folder.getName());
        List<LsmPair> lsmPairs = findLsmPairs(lsmStackList);
        logger.info("Found " + lsmPairs.size() + " pairs");
        
        int i = 0;
        for (LsmPair lsmPair : lsmPairs) {

        	logger.info("Processing LSM Pair: "+lsmPair.name);
        	
        	Entity sample = findExistingSample(folder, lsmPair);
            String linkName = getSymbolicLinkName(lsmPair.lsmFile1);
        	File symbolicLink = new File(linkingDir, linkName);
            
        	try {
        		if (sample == null) {
    	        	// Create the sample
    	            sample = createSample(lsmPair, symbolicLink);
    	            NeuronSeparatorHelper.addToParent(folder, sample, i++, EntityConstants.ATTRIBUTE_ENTITY);
        		}
        		else if (!refresh) {
                   	// We're only doing an incremental update, so skip this pair since it already has results
                	logger.info("  Existing sample exists (id="+sample.getId()+"), skipping.");
                	continue;
	        	}
        		
	        	launchColorSeparationPipeline(sample, lsmPair, symbolicLink);
        	}
        	catch (ComputeException e) {
        		logger.warn("Could not delete existing sample for regeneration, id="+sample.getId(),e);
        	}
        	
        }
    }

    private String getSymbolicLinkName(File lsmFile1) throws Exception {
    	String location = lsmFile1.getAbsolutePath();
        for (String directoryPath : directoryPathList) {
        	location = location.replaceFirst(directoryPath, "");
        }
        return location.replaceFirst("^\\/", "").replaceAll("\\/", "_D_").replaceAll(" ", "_");
    }
    
    /**
     * Find and return the child Sample entity which contains the given LSM Pair. Also populates the LSM entities in the 
     * given lsmPair.
     */
    private Entity findExistingSample(Entity folder, LsmPair lsmPair) {

    	for(EntityData ed : folder.getEntityData()) {
			Entity sampleFolder = ed.getChildEntity();
    		if (sampleFolder == null) continue;
    		if (!EntityConstants.TYPE_SAMPLE.equals(sampleFolder.getEntityType().getName())) continue;

	    	for(EntityData sed : sampleFolder.getEntityData()) {
    			Entity lsmStackPair = sed.getChildEntity();
	    		if (lsmStackPair == null) continue;
	    		if (!EntityConstants.TYPE_LSM_STACK_PAIR.equals(lsmStackPair.getEntityType().getName())) continue;

				boolean found1 = false;
				boolean found2 = false;
				
    	    	for(EntityData led : lsmStackPair.getEntityData()) {
    				Entity lsmStack = led.getChildEntity();
    	    		if (lsmStack == null) continue;
    	    		if (!EntityConstants.TYPE_LSM_STACK.equals(lsmStack.getEntityType().getName())) continue;
    	    		
	    			if (lsmPair.lsmFile1.getName().equals(lsmStack.getName())) {
	    				lsmPair.lsmEntity1 = lsmStack;
	    				found1 = true;
	    			}
	    			else if (lsmPair.lsmFile2.getName().equals(lsmStack.getName())) {
	    				lsmPair.lsmEntity2 = lsmStack;
	    				found2 = true;
	    			}
    	    	}

    	    	if (found1 && found2) {
    	    		return sampleFolder;
    	    	}
    		}
    	}
    	
    	return null;
    }
    
    private List<LsmPair> findLsmPairs(List<File> lsmStackList) throws Exception {
    	
        List<LsmPair> pairs = new ArrayList<LsmPair>();
        Pattern lsmPattern = Pattern.compile("(.+)\\_L(\\d+)((.*)\\.lsm)");
        Set<File> alreadyPaired = new HashSet<File>();
        
        for (File lsm1 : lsmStackList) {
        	
            if (alreadyPaired.contains(lsm1)) continue;
            	
            String lsm1Filename = lsm1.getAbsolutePath();

            Matcher lsm1Matcher = lsmPattern.matcher(lsm1Filename);
            if (lsm1Matcher.matches() && lsm1Matcher.groupCount()==4) {
                String lsm1Prefix = lsm1Matcher.group(1);
                String lsm1Index = lsm1Matcher.group(2);
                String lsm1Suffix = lsm1Matcher.group(3);
                String lsm1SuffixNoExt = lsm1Matcher.group(4);
//                logger.info("  findLsmPairs: prefix="+lsm1Prefix+" index="+lsm1Index+" suffix="+lsm1Suffix+" lsuffixNoExt="+lsm1SuffixNoExt);
                
                String combinedName = lsm1Prefix.substring(lsm1Prefix.lastIndexOf("/")+1) + "_L" + lsm1Index + "-L";

                Integer lsm1IndexInt = null;
                try {
                    lsm1IndexInt = new Integer(lsm1Index.trim());
                } 
                catch (NumberFormatException ex) {
                	logger.warn("File index ("+lsm1Index+") was not an integer for file: "+lsm1Filename);
                	continue;
                }

                Set<File> possibleMatches = new HashSet<File>();
                for (File lsm2 : lsmStackList) {
                	
                	if (alreadyPaired.contains(lsm2)) continue;
                    
                    String lsm2Filename = lsm2.getAbsolutePath();
                    
                    // Obviously we do not want to pair something to itself
                    if (lsm1Filename.equals(lsm2Filename)) continue;
                    
                    Matcher lsm2Matcher = lsmPattern.matcher(lsm2Filename);
                    if (lsm2Matcher.matches() && lsm2Matcher.groupCount()==4) {
                        String lsm2Prefix=lsm2Matcher.group(1);
                        String lsm2Index=lsm2Matcher.group(2);
                        String lsm2Suffix=lsm2Matcher.group(3);
//                        logger.info("    findLsmPairs: prefix="+lsm2Prefix+" index="+lsm2Index+" suffix="+lsm2Suffix);
                        
                        Integer lsm2IndexInt=null;

                        try {
                            lsm2IndexInt = new Integer(lsm2Index.trim());
                        } 
                        catch (Exception ex) {
                        	logger.warn("File index ("+lsm2Index+") was not an integer for file: "+lsm2Filename);
                        	continue;
                        }

                        boolean indexMatch=false;
                        if (lsm2IndexInt%2==0 && lsm1IndexInt==lsm2IndexInt-1) {
                            indexMatch = true;
                        }
                        
                        if (indexMatch && lsm1Prefix.equals(lsm2Prefix) && lsm1Suffix.equals(lsm2Suffix)) {
                            possibleMatches.add(lsm2);
                            combinedName += lsm2Index;
                        } 
                    }
                }
                
                if (possibleMatches.size() == 1) {
                    // We have a unique match
                	File lsm2 = possibleMatches.iterator().next();
                    alreadyPaired.add(lsm1);
                    alreadyPaired.add(lsm2);
                    LsmPair pair = new LsmPair();
                    pair.lsmFile1 = lsm1;
                    pair.lsmFile2 = lsm2;
                    pair.name = combinedName+lsm1SuffixNoExt;
                    pair.lowIndex = lsm1IndexInt;
                    pairs.add(pair);
                    logger.info("Adding lsm pair: " + combinedName);
                }

            }
        }
        
        // Assume that a folder contains one slide's worth of LSM pairs, and sort by the LSM index
        Collections.sort(pairs, new Comparator<LsmPair>() {
			public int compare(LsmPair o1, LsmPair o2) {
				return o1.lowIndex.compareTo(o2.lowIndex);
			}
		});
        
        return pairs;
    }

    protected Entity createSample(LsmPair lsmPair, File link) throws Exception {

    	lsmPair.lsmEntity1 = createLsmStackFromFile(lsmPair.lsmFile1);
    	lsmPair.lsmEntity2 = createLsmStackFromFile(lsmPair.lsmFile2);
		
        Entity sample = new Entity();
        sample.setUser(user);
        sample.setEntityType(annotationBean.getEntityTypeByName(EntityConstants.TYPE_SAMPLE));
        sample.setCreationDate(createDate);
        sample.setUpdatedDate(createDate);
        sample.setName(lsmPair.name);
        sample = annotationBean.saveOrUpdateEntity(sample);
        logger.info("Saved sample as "+sample.getId());
        
    	Entity lsmStackPair = new Entity();
        lsmStackPair.setUser(user);
        lsmStackPair.setEntityType(annotationBean.getEntityTypeByName(EntityConstants.TYPE_LSM_STACK_PAIR));
        lsmStackPair.setCreationDate(createDate);
        lsmStackPair.setUpdatedDate(createDate);
        lsmStackPair.setName("Scans");
        lsmStackPair = annotationBean.saveOrUpdateEntity(lsmStackPair);
        logger.info("Saved LSM stack pair as "+lsmStackPair.getId());
        NeuronSeparatorHelper.addToParent(sample, lsmStackPair, 0, EntityConstants.ATTRIBUTE_ENTITY);
        NeuronSeparatorHelper.addToParent(lsmStackPair, lsmPair.lsmEntity1, 0, EntityConstants.ATTRIBUTE_ENTITY);
        NeuronSeparatorHelper.addToParent(lsmStackPair, lsmPair.lsmEntity2, 1, EntityConstants.ATTRIBUTE_ENTITY);
        
        return sample;
    }

    private Entity createLsmStackFromFile(File file) throws Exception {
        Entity lsmStack = new Entity();
        lsmStack.setUser(user);
        lsmStack.setEntityType(annotationBean.getEntityTypeByName(EntityConstants.TYPE_LSM_STACK));
        lsmStack.setCreationDate(createDate);
        lsmStack.setUpdatedDate(createDate);
        lsmStack.setName(file.getName());
        lsmStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, file.getAbsolutePath());
        lsmStack = annotationBean.saveOrUpdateEntity(lsmStack);
        logger.info("Saved LSM stack as "+lsmStack.getId());
        return lsmStack;
    }
    
    private void launchColorSeparationPipeline(Entity sample, LsmPair lsmPair, File symbolicLink) throws Exception {
        if (runNeuronSeperator) {

        	NeuronSeparatorPipelineTask neuTask = new NeuronSeparatorPipelineTask(new HashSet<Node>(), 
            		user.getUserLogin(), new ArrayList<Event>(), new HashSet<TaskParameter>());
            String lsmEntityIds = lsmPair.lsmEntity1.getId()+" , "+lsmPair.lsmEntity2.getId();
            neuTask.setParameter(NeuronSeparatorPipelineTask.PARAM_inputLsmEntityIdList, lsmEntityIds);
            neuTask.setParameter(NeuronSeparatorPipelineTask.PARAM_symbolLinkName, symbolicLink.getAbsolutePath());
            neuTask.setParameter(NeuronSeparatorPipelineTask.PARAM_outputSampleEntityId, sample.getId().toString());
            computeBean.saveOrUpdateTask(neuTask);
            
            switch(mode) {
            case GRID:
	            neuTask.setJobName("Grid Neuron Separator for MultiColorFlipOutFileDiscovery, task id="+neuTask.getObjectId());
	            computeBean.submitJob("NeuronSeparationPipeline", neuTask.getObjectId());
            	break;
            	
            case LOCAL:
	            neuTask.setJobName("Local Neuron Separator for MultiColorFlipOutFileDiscovery, task id="+neuTask.getObjectId());
	            computeBean.submitJob("NeuronSeparationPipelineLocal", neuTask.getObjectId());
	            break;
	            
            case REMOTE:
	            neuTask.setJobName("Remote Neuron Separator for MultiColorFlipOutFileDiscovery, task id="+neuTask.getObjectId());
	            computeBean.submitJob("NeuronSeparationPipelineRemote", neuTask.getObjectId());
            	break;
            }
        }
        
    }
}
