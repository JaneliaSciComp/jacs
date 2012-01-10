package org.janelia.it.jacs.compute.service.fileDiscovery;

import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * File discovery service for samples defined by a single merged file.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MergedFileDiscoveryService extends FileDiscoveryService {

	private Entity topLevelFolder;
	
    protected void processPathList(List<String> directoryPathList, Entity topLevelFolder) throws Exception {

    	this.topLevelFolder = topLevelFolder;
    	
    	for(String directoryPath : directoryPathList) {
    		
    		File dir = new File(directoryPath);
            logger.info("Processing folder="+dir.getAbsolutePath());
            if (!dir.canRead()) {
            	logger.info("Cannot read from folder "+dir.getAbsolutePath());
            	continue;
            }

    		processMergedFolder(dir);
    	}
    }
    
    protected void processMergedFolder(File dir) throws Exception {

    	List<File> mergedFiles = Arrays.asList(dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
	            return name.endsWith("v3draw") || name.endsWith("v3draw.zip");
			}
		}));
        
        // Sort the pairs by their tag name
        Collections.sort(mergedFiles, new Comparator<File>() {
			@Override
			public int compare(File o1, File o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
        
        for(File mergedFile : mergedFiles) {

        	// Regex grouping example:
        	// 11 222222222222222 33333333333333333333333333 4 5 6666666666666 7777777777777777 888 999999999999 
        	// c4_GMR_57C10_AD_01-Two_recombinase_flipouts_A-m-A-20111006_1_A4-right_optic_lobe.tif_localaligned.v3draw.zip

        	Pattern p = Pattern.compile("^([^_]+?)_(\\w+?)-(\\w+?)-(\\w*?)-(\\w*?)-(\\w+?)-(\\w+?)\\.(\\w+?)_(\\w+?)\\.v3draw\\.zip.*");
        	Matcher matcher = p.matcher(mergedFile.getName());
        	if (!matcher.matches()) {
        		logger.info("Cannot parse filename: "+mergedFile.getName());
        		continue;
        	}
        	
        	int i = 0;
        	String unknownId = matcher.group(++i);
        	String imageLine = matcher.group(++i);
        	String effector = matcher.group(++i);
        	String gender = matcher.group(++i);
        	String age = matcher.group(++i);
        	String directory = matcher.group(++i);
        	String anatomicLocation = formatAnatomicLocation(matcher.group(++i));
        	String originalExtension = matcher.group(++i);
        	String fileProcess = matcher.group(++i);
        	
        	String sampleIdentifier = imageLine+"-"+directory;
            Entity sample = findExistingSample(topLevelFolder, sampleIdentifier);
            
            if (sample == null) {
            	// Sample might be named with the anatomic location
                sample = findExistingSample(topLevelFolder, sampleIdentifier+"-"+anatomicLocation.replaceAll(" ", "_"));
            }
            
            if (sample == null) {
            	logger.warn("No existing sample found. Ignoring merged file for sample " +sampleIdentifier);
            }
            else {
            	logger.info("Found sample for "+sampleIdentifier+", id="+sample.getId());
            	sample = annotationBean.getEntityTree(sample.getId());
            	addMergedFileToSample(sample, anatomicLocation, mergedFile);	
            }
            
        }
    }
    
    private String formatAnatomicLocation(String s) {
    	String[] words = s.split("_");
    	StringBuffer buf = new StringBuffer();
    	for(String w : words) {
    		if (buf.length()>0) buf.append(" ");
    		buf.append(w.substring(0,1).toUpperCase());
    		buf.append(w.substring(1).toLowerCase());
    	}
    	
    	return buf.toString();
    }

    private void addMergedFileToSample(Entity sample, String anatomicLocation, File mergedFile) throws Exception {

        // Get the existing Supporting Files, or create a new one
        Entity supportingFiles = EntityUtils.getSupportingData(sample);
    	if (supportingFiles == null) {
    		supportingFiles = createSupportingFilesFolder();
    		addToParent(sample, supportingFiles, 0, EntityConstants.ATTRIBUTE_SUPPORTING_FILES);
    	}
    	
    	// Get LSM pair based on the tile's anatomic location
    	Entity lsmPair = findChildWithName(supportingFiles, anatomicLocation);
    	if (lsmPair == null) {
    		logger.warn("Could not find LSM Pair for "+anatomicLocation+" in sample (id="+sample.getId()+") with name "+sample.getName());
    		return;
    	}
    	
		Entity fileEntity = findChildWithName(lsmPair, mergedFile.getName());
		if (fileEntity == null) {
			fileEntity = createImageFromFile(mergedFile);
            addToParent(lsmPair, fileEntity, lsmPair.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_MERGED_STACK);
            logger.info("Adding merged file to sample: "+mergedFile.getName());
		}
    }
    
    private Entity findChildWithName(Entity entity, String childName) {
		for (Entity child : entity.getChildren()) {
			if (child.getName().equals(childName)) {
				return child;
			}
		}
		return null;
    }
    
    /**
     * Find and return the child Sample entity
     */
    private Entity findExistingSample(Entity commonRoot, String sampleIdentifier) throws Exception {

    	Set<Entity> entities = annotationBean.getEntitiesByName(sampleIdentifier);

    	Entity sample = null;
    	for(Entity entity : entities) {
    		List<Long> path = annotationBean.getPathToRoot(entity.getId(), commonRoot.getId());
    		if (path != null) {
    			if (sample != null) {
    				logger.warn("Found multiple entities with name "+sampleIdentifier+" rooted in "+commonRoot.getName());
    				return sample; // Return the first one found
    			}
    			sample = entity;
    		}
    	}
    	
    	return sample;
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

    private Entity createImageFromFile(File file) throws Exception {
        Entity imageEntity = new Entity();
        imageEntity.setUser(user);
        imageEntity.setEntityType(annotationBean.getEntityTypeByName(EntityConstants.TYPE_IMAGE_3D));
        imageEntity.setCreationDate(createDate);
        imageEntity.setUpdatedDate(createDate);
        imageEntity.setName(file.getName());
        imageEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, file.getAbsolutePath());
        imageEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_IMAGE_FORMAT, "v3draw");
        imageEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_IS_ZIPPED, new Boolean(file.getName().endsWith(".zip")).toString());
        imageEntity = annotationBean.saveOrUpdateEntity(imageEntity);
        logger.info("Saved merged file as "+imageEntity.getId());
        return imageEntity;
    }

    public void addToParent(Entity parent, Entity entity, Integer index, String attrName) throws Exception {
        EntityData ed = parent.addChildEntity(entity, attrName);
        ed.setOrderIndex(index);
        EJBFactory.getLocalAnnotationBean().saveOrUpdateEntityData(ed);
        logger.info("Added "+entity.getEntityType().getName()+"#"+entity.getId()+
        		" as child of "+parent.getEntityType().getName()+"#"+parent.getId());
    }
}
