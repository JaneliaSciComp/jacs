package org.janelia.it.jacs.shared.utils;

import java.util.*;

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

    public static boolean isInitialized(Object obj) {
    	return Hibernate.isInitialized(obj);
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
    	return getDefaultImageFilePath(entity, EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
    }
    
    public static String getDefaultImageFilePath(Entity entity, String imageRole) {

    	String type = entity.getEntityType().getName();
    	String path = null;
    	
    	// If the entity is a 2D image, just return its path
		if (type.equals(EntityConstants.TYPE_IMAGE_2D)) {
			path = getFilePath(entity);
		}
    	
		if (path == null) {
			// If the entity has a default 2D image, then just return the cached path
	    	path = entity.getValueByAttributeName(imageRole);
		}
		
		if (path == null) {
	    	// This should never be non-null if the previous attempt was null, but just in case...
	    	path = getFilePath(entity.getChildByAttributeName(imageRole));
		}

		if (path == null && EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE.equals(imageRole)) {
	    	// TODO: This is for backwards compatibility with old data. Remove this in the future.
	    	path = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH);
	    	if (path!=null) {
//	    		System.out.println("Warning: old data detected. Using deprecated attribute '"+EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH+"'");
	    	}
	    	
		}
		
		return path;
    }
    
    /**
     * TODO: refactor this
     */
    public EntityData setValueByAttributeName(Entity entity, String attributeName, String value) {
        Set<EntityData> matchingData=new HashSet<EntityData>();
        for (EntityData ed : entity.getEntityData()) {
            if (ed.getEntityAttribute().getName().matches(attributeName)) {
                matchingData.add(ed);
            }
        }
        if (matchingData.size()==0) {
            // Ok, we will add this
            EntityAttribute attribute=entity.getAttributeByName(attributeName);
            if (attribute==null) {
                throw new IllegalArgumentException("Entity "+entity.getId()+" with type "+entity.getEntityType().getName()+" does not have attribute: "+attributeName);
            }
            EntityData ed=new EntityData();
            ed.setParentEntity(entity);
            ed.setEntityAttribute(attribute);
            ed.setValue(value);
            ed.setUser(entity.getUser());
            entity.getEntityData().add(ed);
            return ed;
        } else if (matchingData.size()==1) {
            // Update the value of the existing entry
            EntityData ed=matchingData.iterator().next();
            ed.setValue(value);
            return ed;
        }
        // More than one EntityData matching the attribute - do nothing
        return null;
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

    public static EntityData findChildEntityDataWithName(Entity entity, String childName) {
		return findChildEntityDataWithNameAndType(entity, childName, null);
    }

    public static EntityData findChildEntityDataWithNameAndType(Entity entity, String childName, String type) {
		for (EntityData ed : entity.getEntityData()) {
			Entity child = ed.getChildEntity();
			if (child!=null) {
				if (child.getName().equals(childName) && (type==null||type.equals(child.getEntityType().getName()))) {
					return ed;
				}
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

    public static List<EntityData> getOrderedEntityDataForAttribute(Entity entity, String attrName) {
        List<EntityData> items = new ArrayList<EntityData>();
        for (EntityData entityData : entity.getOrderedEntityData()) {
            if (attrName==null || attrName.equals(entityData.getEntityAttribute().getName())) {
                items.add(entityData);
            }
        }
        return items;
    }

    public static List<EntityData> getOrderedEntityDataWithChildren(Entity entity) {
        List<EntityData> items = new ArrayList<EntityData>();
        for (EntityData entityData : entity.getOrderedEntityData()) {
            if (entityData.getChildEntity()!=null) {
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

	public static void replaceEntityData(Entity entity, EntityData ed, EntityData savedEd) {
		if (!entity.getEntityData().remove(ed)) {
			throw new IllegalStateException("Entity does not contain EntityData: "+ed.getId());
		}
		entity.getEntityData().add(savedEd);
	}

	/**
	 * Returns true if the given data should not be directly displayed in the GUI.
	 * @param entityData
	 * @return
	 */
	public static boolean isHidden(EntityData entityData) {
		String attrName = entityData.getEntityAttribute().getName();
		if (EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE.equals(attrName) 
				|| EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE.equals(attrName) 
				|| EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE.equals(attrName) 
				|| EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE.equals(attrName)
				|| EntityConstants.ATTRIBUTE_REPRESENTATIVE_SAMPLE.equals(attrName)) {
			return true;
		}
		return false;
	}

	public static void replaceChildNodes(Entity entity, Set<Entity> childEntitySet) {

        Map<Long, Entity> childEntityMap = new HashMap<Long, Entity>();
        for (Entity childEntity : childEntitySet) {
            childEntityMap.put(childEntity.getId(), childEntity);
        }

        // Replace the entity data with real objects
        for (EntityData ed : entity.getEntityData()) {
            if (ed.getChildEntity() != null) {
                ed.setChildEntity(childEntityMap.get(ed.getChildEntity().getId()));
            }
        }
	}

	public static List<String> getPathFromUniqueId(String uniqueId) {
		List<String> path = new ArrayList<String>();
		String[] pathParts = uniqueId.split("/");
		String part = "";
		for(int i=0; i<pathParts.length; i+=2) {
			part += "/";
			if (!StringUtils.isEmpty(pathParts[i])) {
				part += pathParts[i]+"/";
			}
			if (pathParts.length>i+1) {
				part += pathParts[i+1];
				path.add(part);
			}
		}
		
		return path;
	}
	
	public static Long getEntityIdFromUniqueId(String uniqueId) {
		String[] pathParts = uniqueId.split("/");
		String lastPart = pathParts[pathParts.length-1];
		if (!lastPart.startsWith("e_")) return null;
		return new Long(lastPart.substring(2));
	}
}
