package org.janelia.it.jacs.shared.utils;

import java.util.Collection;

import org.hibernate.Hibernate;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;

/**
 * Utilities for dealing with Entities.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityUtils {

    public static boolean areLoaded(Collection<EntityData> eds) {
        for (EntityData entityData : eds) {
            if (!Hibernate.isInitialized(entityData.getChildEntity())) {
                return false;
            }
        }
        return true;
    }
    
    public static String getFilePath(Entity entity) {
    	if (entity == null) return null;
    	return entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
    }
    
    public static String getDefaultImageFilePath(Entity entity) {

    	String type = entity.getEntityType().getName();
    	String path = null;
    	
    	// If the entity is a 2D image, just return its path
		if (type.equals(EntityConstants.TYPE_IMAGE_2D)) {
			path = getFilePath(entity);
		}
    	
		if (path == null) {
	    	// If the entity has a default 2D image, just return that path
	    	path = getFilePath(entity.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE));
		}

		if (path == null) {
	    	// TODO: This is for backwards compatibility with old data. Remove this in the future.
	    	path = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH);
		}
    	
		return path;
    }

    public static String getAnyFilePath(Entity entity) {
    	String filePath = getFilePath(entity);
    	if (filePath != null) {
    		return filePath;
    	}
    	return getDefaultImageFilePath(entity);
    }

    public static Entity findChildWithName(Entity entity, String childName) {
		for (Entity child : entity.getChildren()) {
			if (child.getName().equals(childName)) {
				return child;
			}
		}
		return null;
    }
    
    public static Entity getSupportingData(Entity entity) {
    	for(EntityData ed : entity.getEntityData()) {
    		Entity child = ed.getChildEntity();
    		if (child != null && child.getEntityType().getName().equals(EntityConstants.TYPE_SUPPORTING_DATA)) {
    			return child;
    		}	
    	}
    	return null;
    }
}
