package org.janelia.it.jacs.compute.service.entity;

import org.janelia.it.jacs.compute.service.fileDiscovery.TilingPattern;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.EntityUtils;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Creates or updates a view of samples categorized by tiling pattern.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MCFOViewCreationService extends EntityViewCreationService {

    protected void addToEntity(Entity viewEntity, List<Entity> entities) throws Exception {

    	Map<TilingPattern,List<Entity>> patterns = new EnumMap<TilingPattern,List<Entity>>(TilingPattern.class);
    	
    	for(Entity sample : entities) {
    		if (!sample.getEntityType().getName().equals(EntityConstants.TYPE_SAMPLE)) {
    			logger.error("Ommiting entity which is not a sample: "+sample.getId());
    			continue;
    		}
    		
    		TilingPattern pattern = TilingPattern.getTilingPattern(getTags(sample));
    		List<Entity> patternSamples = patterns.get(pattern);
    		if (patternSamples == null) {
    			patternSamples = new ArrayList<Entity>();
    			patterns.put(pattern, patternSamples);
    		}
    		
    		patternSamples.add(sample);
    	}
    	
        for(TilingPattern pattern : TilingPattern.values()) {
    		List<Entity> patternSamples = patterns.get(pattern);
    		if (patternSamples != null) {
    			Entity patternFolder = findChildWithName(viewEntity, pattern.getName());
    			if (patternFolder == null) {
    				patternFolder = createTilingPatternFolder(pattern);
    				addToParent(viewEntity, patternFolder, null, EntityConstants.ATTRIBUTE_ENTITY);
    			}
    			
    			super.addToEntity(patternFolder, patternSamples);
    		}
        }
    }
    
    private List<String> getTags(Entity sample) {
    	List<String> tags = new ArrayList<String>();
    	Entity supportingFiles = EntityUtils.getSupportingData(sample);
    	if (supportingFiles == null) return tags;
    	for(Entity lsmPair : supportingFiles.getOrderedChildren()) {
    		if (lsmPair.getEntityType().getName().equals(EntityConstants.TYPE_IMAGE_TILE)) {
    			tags.add(lsmPair.getName());
    		}
    	}
    	return tags;
    }

    private Entity createTilingPatternFolder(TilingPattern pattern) throws Exception {
        Entity patternFolder = new Entity();
        patternFolder.setUser(user);
        patternFolder.setEntityType(entityBean.getEntityTypeByName(EntityConstants.TYPE_FOLDER));
        patternFolder.setCreationDate(createDate);
        patternFolder.setUpdatedDate(createDate);
        patternFolder.setName(pattern.getName());
        patternFolder = entityBean.saveOrUpdateEntity(patternFolder);
        logger.info("Saved Tiling Pattern Folder as "+patternFolder.getId());
        return patternFolder;
    }
    
    private Entity findChildWithName(Entity entity, String childName) {
		for (Entity child : entity.getChildren()) {
			if (child.getName().equals(childName)) {
				return child;
			}
		}
		return null;
    }
    
}
