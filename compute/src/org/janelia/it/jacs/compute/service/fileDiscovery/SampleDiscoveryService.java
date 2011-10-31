package org.janelia.it.jacs.compute.service.fileDiscovery;

import java.io.File;
import java.util.HashMap;
import java.util.Scanner;

import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;

/**
 * File discovery service for samples defined by a slide_group_info.txt metadata file.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleDiscoveryService extends FileDiscoveryService {
    
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
                if (null==filePairings.get(pieces[1])) {
                    filePairings.put(pieces[1], new FilePair(pieces[1], pieces[0]));
                }
                else {
                    filePairings.get(pieces[1]).setFilename2(pieces[0]);
                }
                if (null==sampleIdentifier) {
                    sampleIdentifier = pieces[2];
                }
            }
        }
        finally {
        	scanner.close();
        }
        
        sampleIdentifier +=  "-" + folder.getName();
        Entity sample = findExistingSample(folder, sampleIdentifier);
        
        if (sample != null) {
			logger.info("Sample already exists: "+sample.getName());
        	return;
        }

        sample = createSample(sampleIdentifier);
        addToParent(folder, sample, null, EntityConstants.ATTRIBUTE_ENTITY);
        
        for (FilePair filePair : filePairings.values()) {
        	if (filePair.isPairingComplete()) {

        		File lsmFile1 = new File(dir,filePair.getFilename1());
        		File lsmFile2 = new File(dir,filePair.getFilename2());
        		
        		if (!lsmFile1.exists()) {
        			logger.warn("File referenced by slide_group_info.txt does not exist: "+lsmFile1.getAbsolutePath());
        			return;
        		}
        		
        		if (!lsmFile2.exists()) {
        			logger.warn("File referenced by slide_group_info.txt does not exist: "+lsmFile2.getAbsolutePath());
        			return;
        		}

            	Entity lsmStackPair = new Entity();
                lsmStackPair.setUser(user);
                lsmStackPair.setEntityType(annotationBean.getEntityTypeByName(EntityConstants.TYPE_LSM_STACK_PAIR));
                lsmStackPair.setCreationDate(createDate);
                lsmStackPair.setUpdatedDate(createDate);
                lsmStackPair.setName(filePair.getPairTag());
                lsmStackPair = annotationBean.saveOrUpdateEntity(lsmStackPair);
                logger.info("Saved LSM stack pair for '"+filePair.getPairTag()+"' as "+lsmStackPair.getId());
                addSampleSupportingEntity(sample, lsmStackPair);
                
                Entity lsmEntity1 = createLsmStackFromFile(lsmFile1);
                Entity lsmEntity2 = createLsmStackFromFile(lsmFile2);
                addToParent(lsmStackPair, lsmEntity1, 0, EntityConstants.ATTRIBUTE_LSM_STACK_1);
                addToParent(lsmStackPair, lsmEntity2, 1, EntityConstants.ATTRIBUTE_LSM_STACK_2);
        		
                logger.info("Adding lsm file to sample parent entity="+lsmFile1.getAbsolutePath());
                logger.info("Adding lsm file to sample parent entity="+lsmFile2.getAbsolutePath());
        		
        	}
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

    protected Entity createSample(String name) throws Exception {
        Entity sample = new Entity();
        sample.setUser(user);
        sample.setEntityType(annotationBean.getEntityTypeByName(EntityConstants.TYPE_SAMPLE));
        sample.setCreationDate(createDate);
        sample.setUpdatedDate(createDate);
        sample.setName(name);
        sample = annotationBean.saveOrUpdateEntity(sample);
        logger.info("Saved sample as "+sample.getId());
        return sample;
    }
    
    private void addSampleSupportingEntity(Entity sample, Entity entity) throws Exception {

    	Entity supportingFiles = getSampleSupportingFiles(sample);
    	
    	if (supportingFiles == null) {
    		supportingFiles = createSupportingFilesFolder();
    		addToParent(sample, supportingFiles, 0, EntityConstants.ATTRIBUTE_SUPPORTING_FILES);
    	}
    	
    	addToParent(supportingFiles, entity, null, EntityConstants.ATTRIBUTE_ENTITY);
    }
    
    protected Entity createSupportingFilesFolder() throws Exception {
        Entity filesFolder = new Entity();
        filesFolder.setUser(user);
        filesFolder.setEntityType(annotationBean.getEntityTypeByName(EntityConstants.TYPE_SUPPORTING_DATA));
        filesFolder.setCreationDate(createDate);
        filesFolder.setUpdatedDate(createDate);
        filesFolder.setName("Supporting Files");
        filesFolder = annotationBean.saveOrUpdateEntity(filesFolder);
        logger.info("Saved supporting files folder as "+filesFolder.getId());
        return filesFolder;
    }
    
    private Entity getSampleSupportingFiles(Entity sample) {
    	Entity supportingFiles = null;
    	for(EntityData ed : sample.getEntityData()) {
    		Entity child = ed.getChildEntity();
    		if (child != null && child.getEntityType().getName().equals(EntityConstants.TYPE_SUPPORTING_DATA)) {
    			supportingFiles = child;
    		}	
    	}
    	return supportingFiles;
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

    public void addToParent(Entity parent, Entity entity, Integer index, String attrName) throws Exception {
        EntityData ed = parent.addChildEntity(entity, attrName);
        ed.setOrderIndex(index);
        EJBFactory.getLocalAnnotationBean().saveOrUpdateEntityData(ed);
        logger.info("Added "+entity.getEntityType().getName()+"#"+entity.getId()+
        		" as child of "+parent.getEntityType().getName()+"#"+parent.getId());
    }
    
    private class FilePair {
        private String pairTag;
        private String filename1;
        private String filename2;

        public FilePair(String pairTag, String filename1) {
            this.pairTag = pairTag;
            this.filename1 = filename1;
        }

        public String getFilename1() {
            return filename1;
        }

        public String getFilename2() {
            return filename2;
        }

        public void setFilename2(String filename2) {
            this.filename2 = filename2;
        }

        public String getPairTag() {
            return pairTag;
        }

        public boolean isPairingComplete() {
            return (null!=filename1&&!"".equals(filename1)) &&
                   (null!=filename2&&!"".equals(filename2));
        }
    }
}
