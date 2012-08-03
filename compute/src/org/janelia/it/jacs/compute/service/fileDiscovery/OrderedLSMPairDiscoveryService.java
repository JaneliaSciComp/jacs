package org.janelia.it.jacs.compute.service.fileDiscovery;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * File discovery service for LSM pairs ordered by suffix.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OrderedLSMPairDiscoveryService extends FileDiscoveryService {

    protected String defaultChannelSpec;

    public void execute(IProcessData processData) throws ServiceException {
        try {
			defaultChannelSpec = (String) processData.getItem("DEFAULT CHANNEL SPECIFICATION");
			if (defaultChannelSpec == null) {
				throw new IllegalArgumentException("DEFAULT CHANNEL SPECIFICATION may not be null");
			}
        } 
        catch (Exception e) {
            throw new ServiceException(e);
        }
        super.execute(processData);
    }
			
    protected void processFolderForData(Entity folder) throws Exception {
    	
        File dir = new File(folder.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
        logger.info("Processing folder="+dir.getAbsolutePath()+" id="+folder.getId());
        
        if (!dir.canRead()) {
        	logger.info("Cannot read from folder "+dir.getAbsolutePath());
        	return;
        }

        processSampleFolder(folder);
    }
    
    protected void processSampleFolder(Entity folder) throws Exception {

        File dir = new File(folder.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
        logger.info("Processing folder as unstitched data="+dir.getAbsolutePath());
        List<File> lsmFileList = new ArrayList<File>();
        for (File file : getOrderedFilesInDir(dir)) {
            if (file.isDirectory()) {
                Entity subfolder = verifyOrCreateChildFolderFromDir(folder, file, null /*index*/);
                processFolderForData(subfolder);
            } 
            else if (file.getName().toUpperCase().endsWith(".LSM")) {
            	lsmFileList.add(file);
            }
            else {
            	// Ignore other files
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
            
        	try {
        		if (sample == null) {
    	        	// Create the sample
    	            sample = createSample(lsmPair);
    	            addToParent(folder, sample, i++, EntityConstants.ATTRIBUTE_ENTITY);
        		}
        	}
        	catch (ComputeException e) {
        		logger.warn("Could not delete existing sample for regeneration, id="+sample.getId(),e);
        	}
        	
        }
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

            Entity supportingFiles = EntityUtils.getSupportingData(sampleFolder);
        	
	    	for(EntityData sed : supportingFiles.getEntityData()) {
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

    protected Entity createSample(LsmPair lsmPair) throws Exception {

    	lsmPair.lsmEntity1 = createLsmStackFromFile(lsmPair.lsmFile1);
    	lsmPair.lsmEntity2 = createLsmStackFromFile(lsmPair.lsmFile2);
		
        Entity sample = new Entity();
        sample.setUser(user);
        sample.setEntityType(entityBean.getEntityTypeByName(EntityConstants.TYPE_SAMPLE));
        sample.setCreationDate(createDate);
        sample.setUpdatedDate(createDate);
        sample.setName(lsmPair.name);
        sample.setValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION, defaultChannelSpec);
        sample = entityBean.saveOrUpdateEntity(sample);
        logger.info("Saved sample as "+sample.getId());

        Entity supportingFiles = EntityUtils.getSupportingData(sample);
    	if (supportingFiles == null) {
    		supportingFiles = createSupportingFilesFolder();
    		addToParent(sample, supportingFiles, 0, EntityConstants.ATTRIBUTE_SUPPORTING_FILES);
    	}
    	
    	Entity lsmStackPair = new Entity();
        lsmStackPair.setUser(user);
        lsmStackPair.setEntityType(entityBean.getEntityTypeByName(EntityConstants.TYPE_LSM_STACK_PAIR));
        lsmStackPair.setCreationDate(createDate);
        lsmStackPair.setUpdatedDate(createDate);
        lsmStackPair.setName("Scans");
        lsmStackPair = entityBean.saveOrUpdateEntity(lsmStackPair);
        logger.info("Saved LSM stack pair as "+lsmStackPair.getId());
        addToParent(supportingFiles, lsmStackPair, 0, EntityConstants.ATTRIBUTE_ENTITY);
        addToParent(lsmStackPair, lsmPair.lsmEntity1, 0, EntityConstants.ATTRIBUTE_ENTITY);
        addToParent(lsmStackPair, lsmPair.lsmEntity2, 1, EntityConstants.ATTRIBUTE_ENTITY);
        
        return sample;
    }

    protected Entity createSupportingFilesFolder() throws Exception {
        Entity filesFolder = new Entity();
        filesFolder.setUser(user);
        filesFolder.setEntityType(entityBean.getEntityTypeByName(EntityConstants.TYPE_SUPPORTING_DATA));
        filesFolder.setCreationDate(createDate);
        filesFolder.setUpdatedDate(createDate);
        filesFolder.setName("Supporting Files");
        filesFolder = entityBean.saveOrUpdateEntity(filesFolder);
        logger.info("Saved supporting files folder as "+filesFolder.getId());
        return filesFolder;
    }
    
    private Entity createLsmStackFromFile(File file) throws Exception {
        Entity lsmStack = new Entity();
        lsmStack.setUser(user);
        lsmStack.setEntityType(entityBean.getEntityTypeByName(EntityConstants.TYPE_LSM_STACK));
        lsmStack.setCreationDate(createDate);
        lsmStack.setUpdatedDate(createDate);
        lsmStack.setName(file.getName());
        lsmStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, file.getAbsolutePath());
        lsmStack = entityBean.saveOrUpdateEntity(lsmStack);
        logger.info("Saved LSM stack as "+lsmStack.getId());
        return lsmStack;
    }

    private class LsmPair {
        public LsmPair() {}
        public String name;
        public Integer lowIndex;
        public File lsmFile1;
        public File lsmFile2;
        public Entity lsmEntity1;
        public Entity lsmEntity2;
    }
}
