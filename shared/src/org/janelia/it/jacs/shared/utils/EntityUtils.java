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
    	return getImageFilePath(entity, EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
    }
    
    public static String getDefault3dImageFilePath(Entity entity) {
    	return getImageFilePath(entity, EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
    }
    
    public static String getImageFilePath(Entity entity, String imageRole) {

    	String type = entity.getEntityType().getName();
    	String path = null;

		if (path == null) {
			// Always attempt to get the shortcut image path for the role requested
	    	path = entity.getValueByAttributeName(imageRole);
		}
    		
		if (imageRole.equals(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE)) {
			// If the entity is a 3D image, just return its path
	        if (type.equals(EntityConstants.TYPE_IMAGE_3D) ||
	        		type.equals(EntityConstants.TYPE_ALIGNED_BRAIN_STACK) ||
	        		type.equals(EntityConstants.TYPE_LSM_STACK) ||
	        		type.equals(EntityConstants.TYPE_SWC_FILE) ||
	        		type.equals(EntityConstants.TYPE_V3D_ANO_FILE) ||
	        		type.equals(EntityConstants.TYPE_STITCHED_V3D_RAW) ||
	        		type.equals(EntityConstants.TYPE_TIF_3D)) {
	        	path = getFilePath(entity);
	        }
		}
		else if (imageRole.equals(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE)) {
			// If the entity is a 2D image, just return its path
			if (type.equals(EntityConstants.TYPE_IMAGE_2D) || 
	        		type.equals(EntityConstants.TYPE_TIF_2D)) {
				path = getFilePath(entity);
			}
		}

    	if (path == null) {
    		EntityData ed = entity.getEntityDataByAttributeName(imageRole);
    		if (ed!=null && isInitialized(ed.getChildEntity())) {
    			path = getFilePath(ed.getChildEntity());
    		}
    	}
    	
		if (path == null && EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE.equals(imageRole)) {
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
				|| EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE.equals(attrName)) {
			return true;
		}
		return false;
	}

	/**
	 * Returns true if the given data plays an image thumbnail role. 
	 * @param entityData
	 * @return
	 */
	public static boolean hasImageRole(EntityData entityData) {
		String attrName = entityData.getEntityAttribute().getName();
		return (attrName.equals(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE) || 
				attrName.equals(EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE) || 
				attrName.equals(EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE));
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
	
	public static Map<Long,Entity> getEntityMap(Collection<Entity> entities) {
        Map<Long, Entity> entityMap = new HashMap<Long, Entity>();
        for (Entity entity : entities) {
            entityMap.put(entity.getId(), entity);
        }
		return entityMap;
	}

	public static List<Long> getEntityIdList(Collection<Entity> entities) {
        List<Long> ids = new ArrayList<Long>();
        for (Entity entity : entities) {
        	ids.add(entity.getId());
        }
		return ids;
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

	public static List<Long> getEntityIdsFromUniqueId(String uniqueId) {
		List<Long> entityIds = new ArrayList<Long>();
		for(String ancestorUniqueId : getPathFromUniqueId(uniqueId)) {
			entityIds.add(getEntityIdFromUniqueId(ancestorUniqueId));
		}
		return entityIds;
	}
	
	public static String getUniqueIdFromParentEntityPath(Collection<EntityData> path) {
		StringBuffer sb = new StringBuffer();
		for(EntityData ed : path) {
			if (sb.length()<=0) {
				sb.append("/e_"+ed.getParentEntity().getId());
			}
			if (ed.getChildEntity()!=null) {
				sb.append("/ed_"+ed.getId());
				sb.append("/e_"+ed.getChildEntity().getId());
			}
		}
		return sb.toString();
	}
}
