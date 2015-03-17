package org.janelia.it.jacs.shared.utils;

import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Hibernate;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityActorPermission;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.user_data.Group;
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
        if (!Hibernate.isInitialized(obj)) {
            return false;
        }
        if (obj instanceof Entity) {
            Entity entity = (Entity)obj;
            try {
                if (entity.getName()==null) {
                    return false;
                }   
            }
            catch (AccessControlException e) {
                // This is a forbidden entity
                return true;
            }
        }
    	return true;
    }
    
    public static boolean areLoaded(Collection<EntityData> eds) {
        for (EntityData entityData : eds) {
            Entity child = entityData.getChildEntity();
            if (child!=null) {
                if (!isInitialized(entityData.getChildEntity())) {
                    return false;
                }
            }
        }
        return true;
    }
    
    /**
     * Returns true if the given entity is a common root.
     * @param entity
     * @return
     */
    public static boolean isCommonRoot(Entity entity) {
        return entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_COMMON_ROOT) != null;
    }
    /**
     * Returns true if the given entity is a common root.
     * @param entity
     * @return
     */
    public static boolean isOntologyRoot(Entity entity) {
        return entity.getEntityTypeName().equals(EntityConstants.TYPE_ONTOLOGY_ROOT);
    }
    
    /**
     * Returns true if the given entity is protected.
     * @param entity
     * @return
     */
    public static boolean isProtected(Entity entity) {
        return entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_IS_PROTECTED) != null;
    }
    
    /**
     * Returns true if the user owns the given entity. 
     * @param entity entity to test
     * @param subjectKey subject key of the user to test
     * @return
     */
    public static boolean isOwner(Entity entity, String subjectKeys) {
        if (entity==null) throw new IllegalArgumentException("Entity is null");
        if (entity.getOwnerKey()==null) throw new IllegalArgumentException("Entity's owner is null");
        return entity.getOwnerKey().equals(subjectKeys);
    }
    
    /**
     * Returns true if the user owns the given entity. 
     * @param entity entity to test
     * @param subjectKeys the keys of the user. The user's key must be the first key.
     * @return
     */
    public static boolean isOwner(Entity entity, List<String> subjectKeys) {
        if (subjectKeys.isEmpty()) throw new IllegalArgumentException("subjectKeys list is empty");
        if (entity==null) throw new IllegalArgumentException("Entity is null");
        if (entity.getOwnerKey()==null) throw new IllegalArgumentException("Entity's owner is null");
        return entity.getOwnerKey().equals(subjectKeys.get(0));
    }

    /**
     * Returns true if the user with the given keys is authorized to read the given entity, either because they are 
     * the owner, or because they or one of their groups has read permission.
     * @param entity entity to test
     * @param subjectKeys the keys of the user. The user's key must be the first key.
     * @return
     */
    public static boolean hasReadAccess(Entity entity, List<String> subjectKeys) {
        String ownerKey = entity.getOwnerKey();
        
        // Special case for fake entities which do not exist in the database
        if (ownerKey==null) return true;
        
        // User or any of their groups grant read access
        if  (subjectKeys.contains(ownerKey)) return true;
                
        // Check explicit permission grants
        for(EntityActorPermission eap : entity.getEntityActorPermissions()) {
            if (subjectKeys.contains(eap.getSubjectKey())) {
                if (eap.getPermissions().contains("r")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns true if the user is authorized to write the given entity, either because they are the owner, or 
     * because they or one of their groups has write permission.
     * @param entity entity to test
     * @param subjectKeys the keys of the user. The user's key must be the first key.
     * @return
     */
    public static boolean hasWriteAccess(Entity entity, List<String> subjectKeys) {
        String ownerKey = entity.getOwnerKey();
        
        // Special case for fake entities which do not exist in the database. 
        if (ownerKey==null) return false;
        
        // Only being the owner grants write access
        if (isOwner(entity, subjectKeys)) return true;

        // Check explicit permission grants
        for(EntityActorPermission eap : entity.getEntityActorPermissions()) {
            if (subjectKeys.contains(eap.getSubjectKey())) {
                if (eap.getPermissions().contains("w")) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Returns true if the user is part of the admin group.
     * @param subjectKeys the keys of the user. The user's key must be the first key.
     * @return
     */
    public static boolean isAdmin(List<String> subjectKeys) {
        return subjectKeys.contains(Group.ADMIN_GROUP_KEY);
        
    }

    /**
     * Returns the subject name part of a given subject key. For example, for "group:flylight", this will return "flylight".
     *
     * @param subjectKey
     * @return
     */
    public static String getNameFromSubjectKey(String subjectKey) {
        if (subjectKey == null) {
            return null;
        }
        return subjectKey.substring(subjectKey.indexOf(':') + 1);
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
	        	.compare(entity1.getName(), entity2.getName(), Ordering.natural().nullsFirst())
	        	.compare(entity1.getEntityTypeName(), entity2.getEntityTypeName(), Ordering.natural().nullsFirst());
    	
    	if (chain.result()!=0) {
    		log.debug("Entity areEqual? false");
	    	debug(entity1);
	    	debug(entity2);
    		return false;
    	}

		Map<Long,EntityData> edMap1 = new HashMap<Long,EntityData>();
		for(EntityData ed : entity1.getEntityData()) {
			edMap1.put(ed.getId(),ed);
		}

		Map<Long,EntityData> edMap2 = new HashMap<Long,EntityData>();
		for(EntityData ed : entity2.getEntityData()) {
			edMap2.put(ed.getId(),ed);
		}
        
        if (edMap1.size()!=edMap2.size()) {
            log.debug("Entity areEqual? attribute sizes differ");
            return false;
        }

        if (!edMap1.keySet().equals(edMap2.keySet())) {
            log.debug("Entity areEqual? attribute id sets differ");
            return false;
        }
		
		for(EntityData ed1 : edMap1.values()) {
		    EntityData ed2 = edMap2.get(ed1.getId());
		    if (!areEqual(ed1,ed2)) {
		        log.debug("Entity 2 does not have the same "+ed1.getEntityAttrName());
		        return false;
		    }
		}
		
        for(EntityData ed2 : edMap2.values()) {
            EntityData ed1 = edMap1.get(ed2.getId());
            if (!areEqual(ed1,ed2)) {
                log.debug("Entity 1 does not have the same "+ed2.getEntityAttrName());
                return false;
            }
        }
        
		if (EntityUtils.isInitialized(entity1.getEntityActorPermissions()) && EntityUtils.isInitialized(entity2.getEntityActorPermissions())) {
			
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
		}
		else {
			log.error("Uninitialized EntityActorPermission collection detected!");
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
    	
        if (ed1==null || ed2==null) return false;
        
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
	            .compare(ed1.getEntityAttrName(), ed2.getEntityAttrName(), Ordering.natural().nullsFirst())
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
    	log.debug("    entityAttribute: {}",ed.getEntityAttrName());
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
    	log.debug("    entityType: {}",entity.getEntityTypeName());
    	log.debug("    name: {}",entity.getName());
    	log.debug("    ownerKey: {}",entity.getOwnerKey()==null?null:entity.getOwnerKey());
    	log.debug("    creationDate: {}",entity.getCreationDate());
    	log.debug("    updatedDate: {}",entity.getUpdatedDate());
    }
    
    public static String identify(Entity entity) {
        return "("+(entity==null?"null entity":entity.getName())+", @"+System.identityHashCode(entity)+")";
    }
    
    public static String identify(EntityData entityData) {
        return "("+(entityData==null?"null entityData":entityData.getId())+", @"+System.identityHashCode(entityData)+")";
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
			entity.setEntityTypeName(newEntity.getEntityTypeName());
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

     	String type = entity.getEntityTypeName();
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
                        type.equals(EntityConstants.TYPE_V3D_ANO_FILE)) {
                    path = getFilePath(entity);
                    log.debug("at 'type-test' attempt, found " + path);
                }
            }
            else if (imageRole.equals(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE)) {
                // If the entity is a 2D image, just return its path
                if (type.equals(EntityConstants.TYPE_IMAGE_2D)) {
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
    
    public static Entity findChildWithEntityId(Entity entity, Long entityId) {
        for (EntityData ed : entity.getEntityData()) {
            Entity child = ed.getChildEntity();
            if (child!=null && child.getId().equals(entityId)) {
                return child;
            }
        }
        return null;
    }
    
    public static Entity findChildWithName(Entity entity, String childName) {
        return findChildWithNameAndTypeAndOwner(entity, childName, null, null);
    }

    public static Entity findChildWithType(Entity entity, String type) {
        return findChildWithNameAndTypeAndOwner(entity, null, type, null);
    }
    
    public static Entity findChildWithNameAndType(Entity entity, String childName, String type) {
    	return findChildWithNameAndTypeAndOwner(entity, childName, type, null);
    }

    public static Entity findChildWithNameAndOwner(Entity entity, String childName, String owner) {
        return findChildWithNameAndTypeAndOwner(entity, childName, null, owner);
    }
    
    public static Entity findChildWithNameAndTypeAndOwner(Entity entity, String childName, String type, String owner) {
    	EntityData ed = findChildEntityDataWithNameAndTypeAndOwner(entity, childName, type, owner);
    	return ed==null?null:ed.getChildEntity();
    }
    
    public static EntityData findChildEntityDataWithName(Entity entity, String childName) {
		return findChildEntityDataWithNameAndTypeAndOwner(entity, childName, null, null);
    }

    public static EntityData findChildEntityDataWithType(Entity entity, String type) {
		return findChildEntityDataWithNameAndTypeAndOwner(entity, null, type, null);
    }

    public static EntityData findChildEntityDataWithNameAndType(Entity entity, String childName, String type) {
		return findChildEntityDataWithNameAndTypeAndOwner(entity, childName, type, null);
    }

    public static EntityData findChildEntityDataWithNameAndOwner(Entity entity, String childName, String owner) {
		return findChildEntityDataWithNameAndTypeAndOwner(entity, childName, null, owner);
    }
    
    public static EntityData findChildEntityDataWithNameAndTypeAndOwner(Entity entity, String childName, String type, String owner) {
    	if (entity==null) return null;
		for (EntityData ed : entity.getOrderedEntityData()) {
			Entity child = ed.getChildEntity();
			if (child!=null) {
				if ((childName==null||childName.equals(child.getName())) && (type==null||type.equals(child.getEntityTypeName())) && (owner==null||owner.equals(child.getOwnerKey()))) {
					return ed;
				}
			}
		}
		return null;
    }

    public static EntityData findChildEntityDataWithChildId(Entity entity, long childId) {
        for (EntityData ed : entity.getEntityData()) {
            Entity child = ed.getChildEntity();
            if (child!=null && child.getId().equals(childId)) {
                return ed;
            }
        }
        return null;
    }
    
    public static Entity getSupportingData(Entity entity) {
    	EntityData ed = findChildEntityDataWithType(entity, EntityConstants.TYPE_SUPPORTING_DATA);
    	if (ed==null) return null;
    	return ed.getChildEntity();
    }

    public static List<EntityData> getOrderedEntityDataForAttribute(Entity entity, String attrName) {
        List<EntityData> items = new ArrayList<EntityData>();
        for (EntityData entityData : entity.getOrderedEntityData()) {
            if (attrName==null || attrName.equals(entityData.getEntityAttrName())) {
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
        if (typeName==null || typeName.equals(entity.getEntityTypeName())) {
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
                if (typeName==null || typeName.equals(child.getEntityTypeName())) {
                    items.add(child);
                }
            }
        }
        return items;
    }

    public static List<Entity> getChildrenForAttribute(Entity entity, String attrName) {
        List<Entity> items = new ArrayList<Entity>();
        for (EntityData entityData : entity.getOrderedEntityData()) {
            if (attrName==null || attrName.equals(entityData.getEntityAttrName())) {
                Entity child = entityData.getChildEntity();
                if (child != null) {
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
    		if (!child.getEntityTypeName().equals(entityTypeName)) continue;
	    	return child;
    	}
    	return null;
    }
    
    public static boolean addAttributeAsTag(Entity entity, String attributeName) {
        Set<EntityData> data=entity.getEntityData();
        
        if (attributeName.equals(entity.getValueByAttributeName(attributeName))) {
            return true;
        } 

        EntityData tag = new EntityData();
        tag.setParentEntity(entity);
        tag.setEntityAttrName(attributeName);
        tag.setOwnerKey(entity.getOwnerKey());
        Date createDate = new Date();
        tag.setCreationDate(createDate);
        tag.setUpdatedDate(createDate);
        tag.setValue(attributeName);
        data.add(tag);

        return true;
    }

    
    public static void replaceAllAttributeTypesInEntityTree(Entity topEntity, String previousEa, String newEa, SaveUnit su) throws Exception {
        log.debug("replaceAllAttributeTypesInEntityTree id="+topEntity.getId());
        Set<EntityData> edSet=topEntity.getEntityData();
        log.debug("Found "+edSet.size()+" entity-data");
        for (EntityData ed : edSet) {
            if (ed.getEntityAttrName().equals(previousEa)) {
                log.debug("Changing value to "+newEa);
                ed.setEntityAttrName(newEa);
                su.saveUnit(ed);
            } else {
                log.debug("Skipping attribute="+ed.getEntityAttrName());
            }
            Entity child=ed.getChildEntity();
            if (child!=null) {
                replaceAllAttributeTypesInEntityTree(child, previousEa, newEa, su);
            }
        }
    }

	/**
	 * Returns true if the given data should not be directly displayed in the GUI.
	 * @param entityData
	 * @return
	 */
	public static boolean isHidden(EntityData entityData) {
		if (entityData==null || entityData.getEntityAttrName()==null) return true;
		String attrName = entityData.getEntityAttrName();
		if (EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE.equals(attrName) 
				|| EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE.equals(attrName) 
				|| EntityConstants.ATTRIBUTE_ALIGNMENT_SPACE.equals(attrName) 
				|| EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE.equals(attrName) 
				|| EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE.equals(attrName)
				|| EntityConstants.ATTRIBUTE_INPUT_IMAGE.equals(attrName)
				|| EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE_ENUMTEXT_ENUMID.equals(attrName)
				|| EntityConstants.ATTRIBUTE_MASK_IMAGE.equals(attrName)
				|| EntityConstants.ATTRIBUTE_CHAN_IMAGE.equals(attrName)
				|| EntityConstants.ATTRIBUTE_SOURCE_SEPARATION.equals(attrName)
				|| EntityConstants.ATTRIBUTE_REPRESENTATIVE_SAMPLE.equals(attrName)
				) {
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
		String attrName = entityData.getEntityAttrName();
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
        	if (entity!=null) {
        		entityMap.put(entity.getId(), entity);
        	}
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
		if (pathParts.length<1) {
		    try {
		        return new Long(uniqueId.trim());
		    }
		    catch (NumberFormatException e) {
		        return null;
		    }
		}
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
	
	/**
	 * Create a standardized, denormalized identifier for the given name which is unique to a user. 
	 * This prepends the user's name and then appends the name with all non-word characters replaced with underscores.
	 * @param username
	 * @param dataSetName
	 * @return
	 */
	public static String createDenormIdentifierFromName(String username, String name) {
	    if (username.contains(":")) username = username.split(":")[1];
    	return username+"_"+name.toLowerCase().replaceAll("\\W+", "_");
	}
	
	/**
	 * Returns true if the given Entity is a virtual non-persistent entity for client-side use only. 
	 * @param entity
	 * @return
	 */
	public static boolean isVirtual(Entity entity) {
        if (EntityConstants.IN_MEMORY_TYPE_VIRTUAL_ENTITY.equals(entity.getEntityTypeName()) || EntityConstants.IN_MEMORY_TYPE_PLACEHOLDER_ENTITY.equals(entity.getEntityTypeName())) {
            return true;
        }
        return false;
	}
}
