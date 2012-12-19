package org.janelia.it.jacs.shared.utils;

import java.util.*;

import org.hibernate.Hibernate;
import org.janelia.it.jacs.model.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

/**
 * Utilities for dealing with Entities.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityUtils {
	
	private static final Logger log = LoggerFactory.getLogger(EntityUtils.class);
	
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

    /**
     * Returns true if the given entities are identical in terms of their own properties and their EntityData properties.
     * @param entity1
     * @param entity2
     * @return
     */
    public static boolean areEqual(Entity entity1, Entity entity2) {

    	ComparisonChain chain = ComparisonChain.start()
	        	.compare(entity1.getId(), entity2.getId(), Ordering.natural().nullsFirst())
	            // date comparison is disabled because sometimes the milliseconds are truncated for unknown reasons
//		        .compare(entity1.getCreationDate(), entity2.getCreationDate(), Ordering.natural().nullsFirst())
//		        .compare(entity1.getUpdatedDate(), entity2.getUpdatedDate(), Ordering.natural().nullsFirst())
	        	.compare(entity1.getOwnerKey(), entity2.getOwnerKey(), Ordering.natural().nullsFirst())
	        	.compare(entity1.getName(), entity2.getName(), Ordering.natural().nullsFirst());
        
    	if (entity1.getEntityType()!=null || entity2.getEntityType()!=null) {
        	if (entity1.getEntityType()==null||entity2.getEntityType()==null) {
        		log.debug("Entity areEqual? entity types differ");
        		return false;
        	}
    		chain = chain.compare(entity1.getEntityType().getName(), entity2.getEntityType().getName(), Ordering.natural().nullsFirst());
    	}
    
    	if (chain.result()!=0) {
    		log.debug("Entity areEqual? false");
	    	debug(entity1);
	    	debug(entity2);
    		return false;
    	}

		Set<Long> edIds1 = new HashSet<Long>();
		for(EntityData ed : entity1.getEntityData()) {
			edIds1.add(ed.getId());
		}

		Set<Long> edIds2 = new HashSet<Long>();
		for(EntityData ed : entity2.getEntityData()) {
			edIds2.add(ed.getId());
		}
		
		if (!edIds1.equals(edIds2)) {
			log.debug("Entity areEqual? attributes differ");
			return false;
		}

		Set<Long> eapIds1 = new HashSet<Long>();
		for(EntityActorPermission eap : entity1.getEntityActorPermissions()) {
			eapIds1.add(eap.getId());
		}

		Set<Long> eapIds2 = new HashSet<Long>();
		for(EntityActorPermission eap : entity2.getEntityActorPermissions()) {
			eapIds2.add(eap.getId());
		}
		
		if (!eapIds1.equals(eapIds2)) {
			log.debug("Entity areEqual? permissions differ");
			return false;
		}
		
    	return true;
    }

    /**
     * Returns true if the given EntityDatas are identical in terms of their properties.
     * @param ed1
     * @param ed2
     * @return
     */
    public static boolean areEqual(EntityData ed1, EntityData ed2) {
    	
    	ComparisonChain chain = ComparisonChain.start()
	        	.compare(ed1.getId(), ed2.getId(), Ordering.natural().nullsFirst())
	            .compare(ed1.getOrderIndex(), ed2.getOrderIndex(), Ordering.natural().nullsFirst())
	            .compare(ed1.getValue(), ed2.getValue(), Ordering.natural().nullsFirst());
    	
        if (ed1.getParentEntity()!=null||ed2.getParentEntity()!=null) {
        	if (ed1.getParentEntity()==null||ed2.getParentEntity()==null) {
        		log.debug("EntityData areEqual? ed parent entities differ");
        		return false;
        	}
        	chain = chain.compare(ed1.getParentEntity().getId(), ed2.getParentEntity().getId(), Ordering.natural().nullsFirst());
        }
        
        if (ed1.getChildEntity()!=null||ed2.getChildEntity()!=null) {
        	if (ed1.getChildEntity()==null||ed2.getChildEntity()==null) {
        		log.debug("EntityData areEqual? ed child entities differ");
        		return false;
        	}
        	chain = chain.compare(ed1.getChildEntity().getId(), ed2.getChildEntity().getId(), Ordering.natural().nullsFirst());
        }
        
	    int c = chain.compare(ed1.getParentEntity().getId(), ed2.getParentEntity().getId(), Ordering.natural().nullsFirst())
	            .compare(ed1.getEntityAttribute().getName(), ed2.getEntityAttribute().getName(), Ordering.natural().nullsFirst())
	            // date comparison is disabled because sometimes the milliseconds are truncated for unknown reasons
//	            .compare(ed1.getCreationDate(), ed2.getCreationDate(), Ordering.natural().nullsFirst())
//	            .compare(ed1.getUpdatedDate(), ed2.getUpdatedDate(), Ordering.natural().nullsFirst())
	            .compare(ed1.getOwnerKey(), ed2.getOwnerKey(), Ordering.natural().nullsFirst())
	            .result();
	    
	    if (c!=0) {
	    	log.debug("EntityData areEqual? false:");
	    	debug(ed1);
	    	debug(ed2);
	    	return false;
	    }
	    return true;
    }

    public static void debug(EntityData ed) {
    	if (!log.isDebugEnabled()) return;
    	log.debug("EntityData(id={})",ed.getId());
    	log.debug("    entityAttribute: {}",ed.getEntityAttribute()==null?null:ed.getEntityAttribute().getName());
    	log.debug("    value: {}",ed.getValue());
    	log.debug("    ownerKey: {}",ed.getOwnerKey());
    	log.debug("    orderIndex: {}",ed.getOrderIndex());
    	log.debug("    creationDate: {}",ed.getCreationDate());
    	log.debug("    updatedDate: {}",ed.getUpdatedDate());
    	log.debug("    childEntity: {}",ed.getChildEntity());
    }
    
    public static void debug(Entity entity) {    		
    	if (!log.isDebugEnabled()) return;
    	log.debug("Entity(id={})",entity.getId());
    	log.debug("    entityType: {}",entity.getEntityType()==null?null:entity.getEntityType().getName());
    	log.debug("    name: {}",entity.getName());
    	log.debug("    ownerKey: {}",entity.getOwnerKey()==null?null:entity.getOwnerKey());
    	log.debug("    creationDate: {}",entity.getCreationDate());
    	log.debug("    updatedDate: {}",entity.getUpdatedDate());
    }
    
    /**
     * Update the entity and its attributes.
     * @param entity
     * @param newEntity
     */
    public static void updateEntity(Entity entity, Entity newEntity) {
    	synchronized(entity) {
			// Map old children onto new EDs, since the old children are initialized and the ones may not be
			Map<Long,Entity> childMap = new HashMap<Long,Entity>();
			for(EntityData ed : entity.getEntityData()) {
				if (ed.getChildEntity()!=null) {
					childMap.put(ed.getChildEntity().getId(), ed.getChildEntity());
				}
			}
			entity.setEntityData(newEntity.getEntityData());
			for(EntityData ed : entity.getEntityData()) {
				if (ed.getChildEntity()!=null && !EntityUtils.isInitialized(ed.getChildEntity()) && ed.getChildEntity().getId()!=null) {
					Entity child = childMap.get(ed.getChildEntity().getId());
					if (child!=null) {
						ed.setChildEntity(child);
					}
				}
			}
			
			// Update permissions
			Map<Long,EntityActorPermission> newEapMap = new HashMap<Long,EntityActorPermission>();
			for(EntityActorPermission eap : newEntity.getEntityActorPermissions()) {
				newEapMap.put(eap.getId(), eap);
			}
			
			// Remove any permissions that are no longer present
			for(Iterator<EntityActorPermission> i = entity.getEntityActorPermissions().iterator(); i.hasNext();) {
				EntityActorPermission eap = i.next();
				if (!newEapMap.containsKey(eap.getId())) {
					i.remove();
				}
				else {
					newEapMap.remove(eap.getId());
				}
			}
			
			// Add any remaining permissions
			entity.getEntityActorPermissions().addAll(newEapMap.values());
			
			
			// Update entity properties
			entity.setName(newEntity.getName());
	    	entity.setUpdatedDate(newEntity.getUpdatedDate());
			entity.setCreationDate(newEntity.getCreationDate());
			entity.setEntityStatus(newEntity.getEntityStatus());
			entity.setEntityType(newEntity.getEntityType());
			entity.setOwnerKey(newEntity.getOwnerKey());
			entity.setEntityData(newEntity.getEntityData());
    	}
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

        if (path == null) {
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
                    log.debug("at 'type-test' attempt, found " + path);
                }
            }
            else if (imageRole.equals(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE)) {
                // If the entity is a 2D image, just return its path
                if (type.equals(EntityConstants.TYPE_IMAGE_2D) ||
                        type.equals(EntityConstants.TYPE_TIF_2D)) {
                    path = getFilePath(entity);
                }
            }
            else if (imageRole.equals(EntityConstants.ATTRIBUTE_DEFAULT_FAST_3D_IMAGE)) {
                Set<EntityData> data = entity.getEntityData();

                for ( EntityData datum: data ) {
                    Entity childEntity = datum.getChildEntity();
                    if ( childEntity != null ) {
                        path = childEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_FAST_3D_IMAGE );
                        if ( path != null ) {
                            log.debug("Found path " + path + " for entity " + entity.getName() + " via drill-and-attrib");
                            break;
                        }
                    }
                }
            }
        }

    	if (path == null) {
    		EntityData ed = entity.getEntityDataByAttributeName(imageRole);
    		if (ed!=null && isInitialized(ed.getChildEntity())) {
    			path = getFilePath(ed.getChildEntity());
                log.debug("at 'entitydata-for-att-name' attempt, found " + path);
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

    public static EntityData findChildEntityDataWithType(Entity entity, String type) {
		return findChildEntityDataWithNameAndType(entity, null, type);
    }

    public static EntityData findChildEntityDataWithNameAndType(Entity entity, String childName, String type) {
		for (EntityData ed : entity.getEntityData()) {
			Entity child = ed.getChildEntity();
			if (child!=null) {
				if ((childName==null||child.getName().equals(childName)) && (type==null||type.equals(child.getEntityType().getName()))) {
					return ed;
				}
			}
		}
		return null;
    }
    
    public static Entity getSupportingData(Entity entity) {
    	EntityData ed = findChildEntityDataWithType(entity, EntityConstants.TYPE_SUPPORTING_DATA);
    	if (ed==null) return null;
    	return ed.getChildEntity();
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
    

    public static List<Entity> getDescendantsOfType(Entity entity, String typeName) {
    	return getDescendantsOfType(entity, typeName, false);
    }
    
    /**
     * Get all the descendants (including self) which are of a certain type. Depends on the subtree of entities 
     * "below" this one being loaded.
     *
     * @param typeName
     * @param ignoreNested short circuit on searching a subtree once an entity of the given type is found
     * @return
     */
    public static List<Entity> getDescendantsOfType(Entity entity, String typeName, boolean ignoreNested) {

    	boolean found = false;
        List<Entity> items = new ArrayList<Entity>();
        if (typeName==null || typeName.equals(entity.getEntityType().getName())) {
            items.add(entity);
            found = true;
        }
        
        if (!found || !ignoreNested) {
            for (EntityData entityData : entity.getOrderedEntityData()) {
                Entity child = entityData.getChildEntity();
                if (child != null) {
                    items.addAll(getDescendantsOfType(child, typeName));
                }
            }	
        }

        return items;
    }

    public static List<Entity> getChildrenOfType(Entity entity, String typeName) {

        List<Entity> items = new ArrayList<Entity>();
        for (EntityData entityData : entity.getOrderedEntityData()) {
            Entity child = entityData.getChildEntity();
            if (child != null) {
                if (typeName==null || typeName.equals(child.getEntityType().getName())) {
                    items.add(child);
                }
            }
        }

        return items;
    }

    /**
     * Order the children and return the last child with the given type.
     * @param entityTypeName
     * @return
     */
    public static Entity getLatestChildOfType(Entity entity, String entityTypeName) {
    	List<Entity> children = entity.getOrderedChildren();
    	Collections.reverse(children);
    	for(Entity child : children) {
    		if (!child.getEntityType().getName().equals(entityTypeName)) continue;
	    	return child;
    	}
    	return null;
    }
    
    public static boolean addAttributeAsTag(Entity entity, String attributeName) {
        Set<EntityData> data=entity.getEntityData();
        EntityAttribute attribute = entity.getAttributeByName(attributeName);
        if (attribute==null) {
            return false;
        } else {
            EntityData tag = new EntityData();
            tag.setParentEntity(entity);
            tag.setEntityAttribute(attribute);
            tag.setOwnerKey(entity.getOwnerKey());
            Date createDate = new Date();
            tag.setCreationDate(createDate);
            tag.setUpdatedDate(createDate);
            tag.setValue(attribute.getName());
            data.add(tag);
        }
        return true;
    }

    
    public static void replaceAllAttributeTypesInEntityTree(Entity topEntity, EntityAttribute previousEa, EntityAttribute newEa, SaveUnit su) throws Exception {
        log.debug("replaceAllAttributeTypesInEntityTree id="+topEntity.getId());
        Set<EntityData> edSet=topEntity.getEntityData();
        log.debug("Found "+edSet.size()+" entity-data");
        for (EntityData ed : edSet) {
            if (ed.getEntityAttribute().getName().equals(previousEa.getName())) {
                log.debug("Changing value to "+newEa.getName());
                ed.setEntityAttribute(newEa);
                su.saveUnit(ed);
            } else {
                log.debug("Skipping attribute="+ed.getEntityAttribute().getName());
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
		if (entityData==null || entityData.getEntityAttribute()==null) return true;
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
	
	public static void replaceChildNodes(Entity entity, Collection<Entity> childEntitySet) {

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
	
	public static String createDataSetIdentifierFromName(String username,String dataSetName) {
    	return username+"_"+dataSetName.toLowerCase().replaceAll("\\W+", "_");
	}
}
