package org.janelia.it.jacs.compute.service.fileDiscovery;

import java.io.File;
import java.util.*;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * File discovery service for samples defined by a slide_group_info.txt metadata file.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleDiscoveryService extends FileDiscoveryService {
    
	private Map<TilingPattern,Integer> patterns = new EnumMap<TilingPattern,Integer>(TilingPattern.class);

    public void execute(IProcessData processData) throws ServiceException {
    	super.execute(processData);

    	logger.info("Tiling pattern statistics:");
        for(TilingPattern pattern : TilingPattern.values()) {
        	Integer count = patterns.get(pattern);
        	if (count==null) count = 0;
        	logger.info(pattern.getName()+": "+count);
        }
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
        
        // TODO: make this parser into a standalone class
        File slideGroupInfoFile = new File(dir,"slide_group_info.txt");
        if (!slideGroupInfoFile.exists()) {
        	// This is not a sample dir, keep looking.
        	processChildFolders(folder);
        	return;
        }
        
        HashMap<String, FilePair> filePairings = new HashMap<String, FilePair>();
        String sampleIdentifier = null;
        Scanner scanner = null;
        
        try {
            scanner = new Scanner(slideGroupInfoFile);
            while (scanner.hasNextLine()){
                String[] pieces = scanner.nextLine().split("\t");
                String tag = pieces[1];
                if (!filePairings.containsKey(tag)) {
                	
                	FilePair filePair = new FilePair(tag);
                	filePair.setFilename1(pieces[0]);

                	File lsmFile1 = new File(dir,filePair.getFilename1());
            		if (!lsmFile1.exists()) {
            			logger.warn("File referenced by slide_group_info.txt does not exist: "+filePair.getFilename1());
            			return;
            		}
            		
            		filePair.setFile1(lsmFile1);
            		filePairings.put(filePair.getPairTag(), filePair);
                }
                else {
                	FilePair filePair = filePairings.get(tag);
                	filePair.setFilename2(pieces[0]);
                    
                	File lsmFile2 = new File(dir,filePair.getFilename2());
            		if (!lsmFile2.exists()) {
            			logger.warn("File referenced by slide_group_info.txt does not exist: "+filePair.getFilename2());
            			return;
            		}

            		filePair.setFile2(lsmFile2);
                }
                if (null==sampleIdentifier) {
                    sampleIdentifier = pieces[2]+ "-" + folder.getName();
                }
            }
        }
        finally {
        	scanner.close();
        }
        
        // Get a list of complete pairs
    	List<FilePair> filePairs = new ArrayList<FilePair>();
        for (FilePair filePair : filePairings.values()) {
        	if (filePair.isPairingComplete()) {
        		filePairs.add(filePair);
        	}
        }

        // Make sure we actually have a sample here    
        if (filePairs.size()<1) return;
    	
        // Sort the pairs by their tag name
        Collections.sort(filePairs, new Comparator<FilePair>() {
			@Override
			public int compare(FilePair o1, FilePair o2) {
				return o1.getPairTag().compareTo(o2.getPairTag());
			}
		});
        
        List<String> tags = new ArrayList<String>();
        for(FilePair filePair : filePairs) {
        	tags.add(filePair.getPairTag());
        }
        
        TilingPattern tiling = TilingPattern.getTilingPattern(tags);
        logger.info("Sample "+sampleIdentifier+" has tiling pattern: "+(tiling==null?"Unknown":tiling.getName()));
        
        if (tiling != null) {
        	Integer count = patterns.get(tiling);
        	if (count == null) {
        		count = 1;
        	}
        	else {
        		count++;
        	}
        	patterns.put(tiling, count);
        }
        
        if (tiling != null && tiling.isStitchable()) {
        	// This is a stitchable case
        	logger.info("Sample "+sampleIdentifier+" is stitchable");
            Entity sample = createOrVerifySample(sampleIdentifier, folder, tiling);
        	// Add the LSM pairs to the Sample's Supporting Files folder
            for (FilePair filePair : filePairs) {
            	addLsmPairToSample(sample, filePair);
            }
        }
        else {
        	// In non-stitchable cases we just create a Sample for each LSM pair
        	logger.info("Sample "+sampleIdentifier+" is not stitchable");
            for (FilePair filePair : filePairs) {
            	String sampleName = sampleIdentifier+"-"+filePair.getPairTag().replaceAll(" ", "_");
                Entity sample = createOrVerifySample(sampleName, folder, tiling);
            	addLsmPairToSample(sample, filePair);
            }
        }
    }
    
    private Entity createOrVerifySample(String name, Entity folder, TilingPattern tiling) throws Exception {
        Entity sample = findExistingSample(folder, name);
        if (sample == null) {
	        sample = createSample(name, tiling);
	        helper.addToParent(folder, sample, null, EntityConstants.ATTRIBUTE_ENTITY);
        }
        return sample;
    }

    private void addLsmPairToSample(Entity sample, FilePair filePair) throws Exception {

        // Get the existing Supporting Files, or create a new one
        Entity supportingFiles = EntityUtils.getSupportingData(sample);
    	if (supportingFiles == null) {
    		supportingFiles = createSupportingFilesFolder();
    		helper.addToParent(sample, supportingFiles, 0, EntityConstants.ATTRIBUTE_SUPPORTING_FILES);
    	}
    	
		Entity lsmStackPair = EntityUtils.findChildWithName(supportingFiles, filePair.getPairTag());
		if (lsmStackPair == null) {
			lsmStackPair = new Entity();
            lsmStackPair.setUser(user);
            lsmStackPair.setEntityType(entityBean.getEntityTypeByName(EntityConstants.TYPE_LSM_STACK_PAIR));
            lsmStackPair.setCreationDate(createDate);
            lsmStackPair.setUpdatedDate(createDate);
            lsmStackPair.setName(filePair.getPairTag());
            lsmStackPair = entityBean.saveOrUpdateEntity(lsmStackPair);
            logger.info("Saved LSM stack pair for '"+filePair.getPairTag()+"' as "+lsmStackPair.getId());
            helper.addToParent(supportingFiles, lsmStackPair, null, EntityConstants.ATTRIBUTE_ENTITY);
		}

		Entity lsmEntity1 = EntityUtils.findChildWithName(lsmStackPair, filePair.getFilename1());
		if (lsmEntity1 == null) {
            lsmEntity1 = createLsmStackFromFile(filePair.getFile1());
            helper.addToParent(lsmStackPair, lsmEntity1, 0, EntityConstants.ATTRIBUTE_LSM_STACK_1);
            logger.info("Adding lsm file to sample parent entity="+filePair.getFile1().getAbsolutePath());
		}

		Entity lsmEntity2 = EntityUtils.findChildWithName(lsmStackPair, filePair.getFilename2());
		if (lsmEntity2 == null) {
            lsmEntity2 = createLsmStackFromFile(filePair.getFile2());
            helper.addToParent(lsmStackPair, lsmEntity2, 1, EntityConstants.ATTRIBUTE_LSM_STACK_2);
            logger.info("Adding lsm file to sample parent entity="+filePair.getFile2().getAbsolutePath());
		}
    }
    
    /**
     * Find and return the child Sample entity
     */
    private Entity findExistingSample(Entity folder, String sampleIdentifier) {

    	for(EntityData ed : folder.getEntityData()) {
			Entity sample = ed.getChildEntity();
    		if (sample == null) continue;
    		if (!EntityConstants.TYPE_SAMPLE.equals(sample.getEntityType().getName())) continue;
    		if (!sample.getName().equals(sampleIdentifier)) continue;
    	    return sample;
    	}

        // Could not find sample child entity
    	return null;
    }

    protected Entity createSample(String name, TilingPattern tiling) throws Exception {
        Entity sample = new Entity();
        sample.setUser(user);
        sample.setEntityType(entityBean.getEntityTypeByName(EntityConstants.TYPE_SAMPLE));
        sample.setCreationDate(createDate);
        sample.setUpdatedDate(createDate);
        sample.setName(name);
        sample.setValueByAttributeName(EntityConstants.ATTRIBUTE_TILING_PATTERN, tiling.toString());
        sample = entityBean.saveOrUpdateEntity(sample);
        logger.info("Saved sample as "+sample.getId());
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
    
    private class FilePair {
        private String pairTag;
        private String filename1;
        private String filename2;
        private File file1;
        private File file2;

        public FilePair(String pairTag) {
            this.pairTag = pairTag;
        }

        public String getPairTag() {
			return pairTag;
		}

		public String getFilename1() {
			return filename1;
		}

		public void setFilename1(String filename1) {
			this.filename1 = filename1;
		}

		public String getFilename2() {
			return filename2;
		}

		public void setFilename2(String filename2) {
			this.filename2 = filename2;
		}

		public File getFile1() {
			return file1;
		}

		public void setFile1(File file1) {
			this.file1 = file1;
		}

		public File getFile2() {
			return file2;
		}

		public void setFile2(File file2) {
			this.file2 = file2;
		}

		public boolean isPairingComplete() {
            return (null!=filename1&&!"".equals(filename1)) &&
                   (null!=filename2&&!"".equals(filename2));
        }
    }
}
