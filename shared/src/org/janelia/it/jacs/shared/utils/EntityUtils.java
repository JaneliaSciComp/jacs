package org.janelia.it.jacs.shared.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Hibernate;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityAttribute;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;

/**
 * Utilities for dealing with Entities.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */


public class EntityUtils {

    private static final Logger logger = Logger.getLogger(EntityUtils.class);


    public interface SaveUnit {
        public void saveUnit(Object o) throws Exception;
    }

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
    
    public static EntityData removeChild(Entity entity, Entity child) {
		EntityData toDelete = null;
		for(EntityData ed : entity.getEntityData()) {
			if (ed.getChildEntity() != null && ed.getChildEntity().getId().equals(child.getId())) {
				toDelete = ed;
			}
		}
		if (toDelete == null) {
			System.out.println("Could not find EntityData to delete for "+child.getName());
			return null;
		}
		else {
			entity.getEntityData().remove(toDelete);
			return toDelete;
		}
    }

    public static List<EntityData> getOrderedEntityDataOfType(Entity entity, String attrName) {
        List<EntityData> items = new ArrayList<EntityData>();
        for (EntityData entityData : entity.getOrderedEntityData()) {
            if (attrName==null || attrName.equals(entityData.getEntityAttribute().getName())) {
                items.add(entityData);
            }
        }
        return items;
    }

    public static void replaceAllAttributeTypesInEntityTree(Entity topEntity, EntityAttribute previousEa, EntityAttribute newEa, SaveUnit su) throws Exception {
        logger.info("replaceAllAttributeTypesInEntityTree id="+topEntity.getId());
        Set<EntityData> edSet=topEntity.getEntityData();
        logger.info("Found "+edSet.size()+" entity-data");
        for (EntityData ed : edSet) {
            if (ed.getEntityAttribute().getName().equals(previousEa.getName())) {
                logger.info("Changing value to "+newEa.getName());
                ed.setEntityAttribute(newEa);
                su.saveUnit(ed);
            } else {
                logger.info("Skipping attribute="+ed.getEntityAttribute().getName());
            }
            Entity child=ed.getChildEntity();
            if (child!=null) {
                replaceAllAttributeTypesInEntityTree(child, previousEa, newEa, su);
            }
        }
    }

}
