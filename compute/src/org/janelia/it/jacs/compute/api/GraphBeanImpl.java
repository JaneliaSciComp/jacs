
package org.janelia.it.jacs.compute.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.AnnotationDAO;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.api.support.Access;
import org.janelia.it.jacs.compute.api.support.Access.AccessPattern;
import org.janelia.it.jacs.compute.api.support.MappedId;
import org.janelia.it.jacs.compute.launcher.indexing.IndexingHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityActorPermission;
import org.janelia.it.jacs.model.entity.EntityAttribute;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.EntityType;
import org.janelia.it.jacs.model.graph.entity.EntityNode;
import org.janelia.it.jacs.model.graph.entity.EntityPermission;
import org.janelia.it.jacs.model.graph.entity.EntityRelationship;
import org.janelia.it.jacs.model.graph.entity.support.EntityGraphObjectFactory;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.jboss.annotation.ejb.PoolClass;
import org.jboss.annotation.ejb.TransactionTimeout;

/**
 * Implementation of local and remote graph beans.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@Stateless(name = "GraphEJB")
@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
@TransactionTimeout(432000)
@PoolClass(value = org.jboss.ejb3.StrictMaxPool.class, maxSize = 500, timeout = 10000)
public class GraphBeanImpl implements GraphBeanLocal, GraphBeanRemote {
	
    private static final Logger _logger = Logger.getLogger(GraphBeanImpl.class);
    private final AnnotationDAO _annotationDAO = new AnnotationDAO(_logger);
    private final EntityGraphObjectFactory factory = new EntityGraphObjectFactory();
    
    private void updateIndex(Entity entity) {
    	IndexingHelper.updateIndex(entity.getId());
    }
   
    @Override
    public EntityNode createEntityNode(Access access, EntityNode entityNode) throws ComputeException {
        try {
            String subjectKey = access.getSubjectKey();
            
            if (entityNode.getOwnerKey()!=null && !entityNode.getOwnerKey().equals(subjectKey)) {
                throw new IllegalArgumentException(subjectKey+" cannot create an object for another user ("+entityNode.getOwnerKey()+")");
            }
            
            Date creationDate = entityNode.getCreationDate();
            Date updatedDate = entityNode.getCreationDate();
            Date now = new Date();
            
            if (creationDate == null) {
                creationDate = now;
            }
            
            if (updatedDate == null) {
                updatedDate = now;
            }
            
            String entityType = factory.getEntityType(entityNode);
            Entity entity = new Entity(null, entityNode.getName(), subjectKey, entityType, creationDate, updatedDate, new HashSet<EntityData>());
            Map<String,String> attrMap = factory.getAttributes(entityNode);
            for(String key : attrMap.keySet()) {
                entity.setValueByAttributeName(key, attrMap.get(key));
            }
            
            _annotationDAO.saveOrUpdate(entity);
            
            _logger.info(subjectKey+" created entity "+entity.getId());
            updateIndex(entity);
            return (EntityNode)factory.getNodeInstance(entity);
        } 
        catch (Exception e) {
            _logger.error("Error creating domain object",e);
            throw new ComputeException("Error creating domain object",e);
        }
    }

    @Override
    public EntityRelationship createRelationship(Access access, Long sourceObjGuid, Long targetObjGuid, Integer index, String relType) throws ComputeException {
        return createRelationship(access, sourceObjGuid, targetObjGuid, index, relType, null);
    }

    public EntityRelationship createRelationship(Access access, Long sourceObjGuid, Long targetObjGuid, Integer index, String relType, String value) throws ComputeException {
        try {
            String subjectKey = access.getSubjectKey();
            Entity source = _annotationDAO.getEntityById(subjectKey, sourceObjGuid);
            if (source==null) {
                throw new DaoException("Source domain object does not exist: "+sourceObjGuid);
            }
            if (subjectKey!=null && !EntityUtils.hasWriteAccess(source, _annotationDAO.getSubjectKeys(subjectKey))) {
                throw new ComputeException("Subject "+subjectKey+" cannot add children to "+sourceObjGuid);
            }
            Entity target = _annotationDAO.getEntityById(subjectKey, targetObjGuid);
            if (target==null) {
                throw new DaoException("Entity does not exist "+targetObjGuid);
            }
            
            checkEntityTypeSupportsAttribute(source.getEntityTypeName(), relType);
            
            EntityData ed = _annotationDAO.addEntityToParent(source, target, index, relType, value);
            
            _logger.info(subjectKey+" added entity data "+ed.getId()+" (parent="+source.getId()+",child="+target.getId()+")");
            
            IndexingHelper.updateIndexAddAncestor(targetObjGuid, sourceObjGuid);
            
            return (EntityRelationship)factory.getRelationshipInstance(ed);
        } 
        catch (Exception e) {
            _logger.error("Error creating relationship from "+sourceObjGuid+" to "+targetObjGuid, e);
            throw new ComputeException("Error creating relationship from "+sourceObjGuid+" to "+targetObjGuid,e);
        }
    }

    @Override
    public void createRelationships(Access access, Long sourceObjGuid, List<Long> targetObjGuids, String relType) throws ComputeException {
        try {
            String subjectKey = access.getSubjectKey();
            Entity parent = _annotationDAO.getEntityById(sourceObjGuid);
            if (parent==null) {
                throw new Exception("Entity not found: "+sourceObjGuid);
            }
            if (!EntityUtils.hasWriteAccess(parent, _annotationDAO.getSubjectKeys(subjectKey))) {
                throw new ComputeException("Subject "+subjectKey+" cannot add children to "+parent.getId());
            }
            
            checkEntityTypeSupportsAttribute(parent.getEntityTypeName(), relType);
            
            _annotationDAO.addChildren(subjectKey, sourceObjGuid, targetObjGuids, relType);
            _logger.info("Subject "+subjectKey+" added "+targetObjGuids.size()+" children to parent "+sourceObjGuid);
            
            for(Long childId : targetObjGuids) {
                IndexingHelper.updateIndexAddAncestor(childId, sourceObjGuid);
            }
        } 
        catch (Exception e) {
            _logger.error("Error creating relationships to parent "+sourceObjGuid, e);
            throw new ComputeException("Error creating relationships to parent "+sourceObjGuid, e);
        }
    }

    @Override
    public EntityNode getEntityNode(Access access, Long objGuid) throws ComputeException {
        try {
            String subjectKey = access.getSubjectKey();
            switch (access.getAccessPattern()) {
            case ALL_ACCESSIBLE_OBJECTS: return convert(_annotationDAO.getEntityById(subjectKey, objGuid));
            case OWNED_OBJECTS_ONLY: throw new IllegalArgumentException("This method does not support the OWNED_OBJECTS_ONLY access pattern");
            }
            return null;
        }
        catch (Exception e) {
            _logger.error("Error getting domain object "+objGuid,e);
            throw new ComputeException("Error getting domain object "+objGuid,e);
        }
    }

    @Override
    public EntityNode getEntityNodeTree(Access access, Long objGuid) throws ComputeException {
        try {
            String subjectKey = access.getSubjectKey();
            Entity entity = _annotationDAO.getEntityById(subjectKey, objGuid);
            _annotationDAO.loadLazyEntity(subjectKey, entity, true);    
            switch (access.getAccessPattern()) {
            case ALL_ACCESSIBLE_OBJECTS: return convert(entity);
            case OWNED_OBJECTS_ONLY: throw new IllegalArgumentException("This method does not support the OWNED_OBJECTS_ONLY access pattern");
            }
            return null;
        }
        catch (Exception e) {
            _logger.error("Error getting domain object "+objGuid,e);
            throw new ComputeException("Error getting domain object "+objGuid,e);
        }
    }

    @Override
    public EntityNode getEntityNodeAndChildren(Access access, Long objGuid) throws ComputeException {
        try {
            String subjectKey = access.getSubjectKey();    
            switch (access.getAccessPattern()) {
            case ALL_ACCESSIBLE_OBJECTS: return convert(_annotationDAO.getEntityAndChildren(subjectKey, objGuid));
            case OWNED_OBJECTS_ONLY: throw new IllegalArgumentException("This method does not support the OWNED_OBJECTS_ONLY access pattern");
            }
            return null;
        }
        catch (Exception e) {
            _logger.error("Error getting domain object "+objGuid,e);
            throw new ComputeException("Error getting domain object "+objGuid,e);
        }
    }

    @Override
    public Collection<EntityNode> getIncomingRelatedObjects(Access access, Long objGuid) throws ComputeException {
        try {
            String subjectKey = access.getSubjectKey();     
            switch (access.getAccessPattern()) {
            case ALL_ACCESSIBLE_OBJECTS: return convertEntities(_annotationDAO.getParentEntities(subjectKey, objGuid));
            case OWNED_OBJECTS_ONLY: throw new IllegalArgumentException("This method does not support the OWNED_OBJECTS_ONLY access pattern");
            }
            return null;
        } 
        catch (Exception e) {
            _logger.error("Error getting incoming related objects for domain object "+objGuid, e);
            throw new ComputeException("Error getting incoming related objects for domain object "+objGuid,e);
        }
    }

    @Override
    public Collection<EntityNode> getOutgoingRelatedObjects(Access access, Long objGuid) throws ComputeException {
        try {
            String subjectKey = access.getSubjectKey();     
            switch (access.getAccessPattern()) {
            case ALL_ACCESSIBLE_OBJECTS: return convertEntities(_annotationDAO.getChildEntities(subjectKey, objGuid));
            case OWNED_OBJECTS_ONLY: throw new IllegalArgumentException("This method does not support the OWNED_OBJECTS_ONLY access pattern");
            }
            return null;
        } 
        catch (Exception e) {
            _logger.error("Error getting outgoing related objects for domain object "+objGuid, e);
            throw new ComputeException("Error getting outgoing related objects for domain object "+objGuid,e);
        }
    }

    @Override
    public Collection<EntityNode> getEntityNodes(Access access, List<Long> objGuids) throws ComputeException {
        try {
            String subjectKey = access.getSubjectKey();
            switch (access.getAccessPattern()) {
            case ALL_ACCESSIBLE_OBJECTS: return convertEntities(_annotationDAO.getEntitiesInList(subjectKey, objGuids));
            case OWNED_OBJECTS_ONLY: throw new IllegalArgumentException("This method does not support the OWNED_OBJECTS_ONLY access pattern");
            }
            return null;
        }
        catch (Exception e) {
            _logger.error("Error getting domain objects in list", e);
            throw new ComputeException("Error getting domain objects in list",e);
        }
    }

    @Override
    public Collection<EntityNode> getEntityNodesByName(Access access, String name) throws ComputeException {
        String subjectKey = access.getSubjectKey();
        try {
            switch (access.getAccessPattern()) {
            case ALL_ACCESSIBLE_OBJECTS: return convertEntities(_annotationDAO.getEntitiesByName(subjectKey, name));
            case OWNED_OBJECTS_ONLY: return convertEntities(_annotationDAO.getUserEntitiesByName(subjectKey, name));
            }
            return null;
        }
        catch (Exception e) {
            _logger.error("Error trying to get the domain objects with name "+name+" for user "+subjectKey, e);
            throw new ComputeException("Error trying to get the domain objects with name "+name+" for user "+subjectKey,e);
        }
    }

    @Override
    public Collection<EntityNode> getEntityNodesByTypeName(Access access, String typeName) throws ComputeException {
        String subjectKey = access.getSubjectKey();
        try {
            switch (access.getAccessPattern()) {
            case ALL_ACCESSIBLE_OBJECTS: return convertEntities(_annotationDAO.getEntitiesByTypeName(subjectKey, typeName));
            case OWNED_OBJECTS_ONLY: return convertEntities(_annotationDAO.getUserEntitiesByTypeName(subjectKey, typeName));
            }
            return null;
        }
        catch (Exception e) {
            _logger.error("Error trying to get the domain objects with type "+typeName+" for user "+subjectKey, e);
            throw new ComputeException("Error trying to get the domain objects with type "+typeName+" for user "+subjectKey,e);
        }
    }

    @Override
    public Collection<EntityNode> getEntityNodesByNameAndTypeName(Access access, String name, String typeName) throws ComputeException {
        String subjectKey = access.getSubjectKey();
        try {
            switch (access.getAccessPattern()) {
            case ALL_ACCESSIBLE_OBJECTS: return convertEntities(_annotationDAO.getEntitiesByNameAndTypeName(subjectKey, name, typeName));
            case OWNED_OBJECTS_ONLY: return convertEntities(_annotationDAO.getUserEntitiesByNameAndTypeName(subjectKey, name, typeName));
            }
            return null;
        }
        catch (Exception e) {
            _logger.error("Error trying to get the domain objects with name "+name+" and type "+typeName+" for user "+subjectKey, e);
            throw new ComputeException("Error trying to get the domain objects with name "+name+" and type "+typeName+" for user "+subjectKey,e);
        }
    }

    @Override
    public Collection<EntityNode> getEntityNodesWithAttributeValue(Access access, String attrName, String attrValue) throws ComputeException {
        String subjectKey = access.getSubjectKey();
        try {
            switch (access.getAccessPattern()) {
            case ALL_ACCESSIBLE_OBJECTS: return convertEntities(_annotationDAO.getEntitiesWithAttributeValue(subjectKey, attrName, attrValue));
            case OWNED_OBJECTS_ONLY: return convertEntities(_annotationDAO.getUserEntitiesWithAttributeValue(subjectKey, attrName, attrValue));
            }
            return null;
        }
        catch (Exception e) {
            _logger.error("Error getting domain objects with "+attrName+" like "+attrValue+" for "+subjectKey, e);
            throw new ComputeException("Error getting domain objects with "+attrName+" like "+attrValue+" for "+subjectKey,e);
        }
    }

    @Override
    public Collection<EntityNode> getEntityNodesWithAttributeValueAndTypeName(Access access, String attrName, String attrValue, String typeName) throws ComputeException {
        String subjectKey = access.getSubjectKey();
        try {
            switch (access.getAccessPattern()) {
            case ALL_ACCESSIBLE_OBJECTS: return convertEntities(_annotationDAO.getEntitiesWithAttributeValue(subjectKey, typeName, attrName, attrValue));
            case OWNED_OBJECTS_ONLY: return convertEntities(_annotationDAO.getUserEntitiesWithAttributeValue(subjectKey, typeName, attrName, attrValue));
            }
            return null;
        }
        catch (Exception e) {
            _logger.error("Error getting domain objects with type "+typeName+" and "+attrName+" like "+attrValue+" for "+subjectKey, e);
            throw new ComputeException("Error getting domain objects with type "+typeName+" and "+attrName+" like "+attrValue+" for "+subjectKey,e);
        }
    }

    @Override
    public Collection<EntityNode> getEntityNodesWithTag(Access access, String attrTag) throws ComputeException {
        String subjectKey = access.getSubjectKey();
        try {
            switch (access.getAccessPattern()) {
            case ALL_ACCESSIBLE_OBJECTS: return convertEntities(_annotationDAO.getEntitiesWithTag(subjectKey, attrTag));
            case OWNED_OBJECTS_ONLY: throw new IllegalArgumentException("This method does not support the OWNED_OBJECTS_ONLY access pattern");
            }
            return null;
        }
        catch (Exception e) {
            _logger.error("Error getting domain objects with tag "+attrTag+" for "+subjectKey, e);
            throw new ComputeException("Error getting domain objects with tag "+attrTag+" for "+subjectKey,e);
        }
    }

    @Override
    public Collection<EntityRelationship> getIncomingRelationships(Access access, Long targetObjGuid) throws ComputeException {
        String subjectKey = access.getSubjectKey();
        try {
            switch (access.getAccessPattern()) {
            case ALL_ACCESSIBLE_OBJECTS: return convertEntityDatas(_annotationDAO.getParentEntityDatas(subjectKey, targetObjGuid));
            case OWNED_OBJECTS_ONLY: throw new IllegalArgumentException("This method does not support the OWNED_OBJECTS_ONLY access pattern");
            }
            return null;
        }
        catch (Exception e) {
            _logger.error("Error getting incoming relationships for domain object "+targetObjGuid+" for "+subjectKey, e);
            throw new ComputeException("Error getting incoming relationships for domain object "+targetObjGuid+" for "+subjectKey,e);
        }
    }

    @Override
    public Collection<Long> getEntityNodeGuidsWithIncomingRelationshipsTo(Access access, Long targetObjGuid, String attrName) throws ComputeException {
        String subjectKey = access.getSubjectKey();
        try {
            switch (access.getAccessPattern()) {
            case ALL_ACCESSIBLE_OBJECTS: return _annotationDAO.getParentIdsForAttribute(subjectKey, targetObjGuid, attrName);
            case OWNED_OBJECTS_ONLY: throw new IllegalArgumentException("This method does not support the OWNED_OBJECTS_ONLY access pattern");
            }
            return null;
        }
        catch (Exception e) {
            _logger.error("Error getting incoming relationships for domain object "+targetObjGuid+" for "+subjectKey, e);
            throw new ComputeException("Error getting incoming relationships for domain object "+targetObjGuid+" for "+subjectKey,e);
        }
    }

    @Override
    public long getCountEntityNodesWithAttributeValue(Access access, String attrName, String attrValue) throws ComputeException {
        String subjectKey = access.getSubjectKey();
        try {
            switch (access.getAccessPattern()) {
            case ALL_ACCESSIBLE_OBJECTS: _logger.warn("getCountEntityNodesWithAttributeValue does not support ALL_ACCESSIBLE_OBJECTS access pattern. Falling back to OWNED_OBJECTS_ONLY."); 
            case OWNED_OBJECTS_ONLY: 
            }
            return _annotationDAO.getCountUserEntitiesWithAttributeValue(subjectKey, attrName, attrValue);
        }
        catch (Exception e) {
            _logger.error("Error counting domain objects with attribute "+attrName+" value "+attrValue+" for "+subjectKey, e);
            throw new ComputeException("Error counting domain objects with attribute "+attrName+" value "+attrValue+" for "+subjectKey,e);
        }
    }

    @Override
    public EntityNode getAncestorWithType(Access access, Long objGuid, String typeName) throws ComputeException {
        String subjectKey = access.getSubjectKey();
        try {
            switch (access.getAccessPattern()) {
            case ALL_ACCESSIBLE_OBJECTS: return convert(_annotationDAO.getAncestorWithType(subjectKey, objGuid, typeName));
            case OWNED_OBJECTS_ONLY: throw new IllegalArgumentException("This method does not support the OWNED_OBJECTS_ONLY access pattern");
            }
            return null;
        }
        catch (Exception e) {
            _logger.error("Error getting ancestors of type "+typeName+" for domain object "+objGuid+" for "+subjectKey, e);
            throw new ComputeException("Error getting ancestors of type "+typeName+" for domain object "+objGuid+" for "+subjectKey,e);
        }
    }

    @Override
    public List<List<EntityRelationship>> getPathsToRoots(Access access, Long objGuid) throws ComputeException {
        String subjectKey = access.getSubjectKey();
        try {
            if (access.getAccessPattern().equals(AccessPattern.OWNED_OBJECTS_ONLY)) {
                throw new IllegalArgumentException("This method does not support the OWNED_OBJECTS_ONLY access pattern");
            }
        
            List<List<EntityRelationship>> paths = new ArrayList<List<EntityRelationship>>(); 
            EntityData fakeEd = new EntityData();
            fakeEd.setParentEntity(_annotationDAO.getEntityById(objGuid));
            for(List<EntityData> edList : _annotationDAO.getEntityDataPathsToRoots(subjectKey, fakeEd)) {
                paths.add((List<EntityRelationship>)convertEntityDatas(edList));
            }
            return paths;
        }
        catch (Exception e) {
            _logger.error("Error getting paths to root for domain object "+objGuid+" for "+subjectKey, e);
            throw new ComputeException("Error getting paths to root for domain object "+objGuid+" for "+subjectKey,e);
        }
    }

    @Override
    public List<MappedId> getProjectedResults(Access access, List<Long> objGuids, List<String> upMapping, List<String> downMapping) throws ComputeException {
        String subjectKey = access.getSubjectKey();
        try {
            if (access.getAccessPattern().equals(AccessPattern.OWNED_OBJECTS_ONLY)) {
                throw new IllegalArgumentException("This method does not support the OWNED_OBJECTS_ONLY access pattern");
            }
            return _annotationDAO.getProjectedResults(subjectKey, objGuids, upMapping, downMapping);
        } 
        catch (Exception e) {
            _logger.error("Error getting projected results for "+subjectKey, e);
            throw new ComputeException("Error getting projected results for "+subjectKey, e);
        }
    }

    @Override
    public Map<Long,String> getChildEntityNodeNames(Access access, Long objGuid) throws ComputeException {
        String subjectKey = access.getSubjectKey();
        try {
            if (access.getAccessPattern().equals(AccessPattern.OWNED_OBJECTS_ONLY)) {
                throw new IllegalArgumentException("This method does not support the OWNED_OBJECTS_ONLY access pattern");
            }
            // TODO: implement access control
            return _annotationDAO.getChildEntityNames(objGuid);
        } 
        catch (Exception e) {
            _logger.error("Error getting projected results for "+subjectKey, e);
            throw new ComputeException("Error getting projected results for "+subjectKey, e);
        }
    }

    @Override
    public EntityNode updateEntityNode(Access access, EntityNode entityNode) throws ComputeException {
        try {
            Entity entity = _annotationDAO.getEntityById(entityNode.getId());
            entity.setName(entityNode.getName());
            entity.setCreationDate(entityNode.getCreationDate());
            entity.setUpdatedDate(entityNode.getUpdatedDate());
            entity.setEntityTypeName(factory.getEntityType(entityNode));
            entity.setOwnerKey(entityNode.getOwnerKey());
            if (entityNode.isRelsInit()) {
                entity.setNumChildren(entityNode.getRelationships().size());
            }
            
            Map<String,String> attrMap = factory.getAttributes(entityNode);
            for(Entry<String,String> entry : attrMap.entrySet()) {
                entity.setValueByAttributeName(entry.getKey(), entry.getValue());
            }
            
            Set<EntityData> toRemove = new HashSet<EntityData>();
            for(EntityData ed : entity.getEntityData()) {
                if (ed.getChildEntity()==null && !attrMap.containsKey(ed.getEntityAttrName())) {
                    toRemove.add(ed);
                }
            }
            
            for(EntityData ed : toRemove) {
                entity.getEntityData().remove(ed);
            }
            
            checkAttributeTypes(entity);
            _annotationDAO.saveOrUpdateEntity(entity);
            
            updateIndex(entity);
            return convert(entity);
        } 
        catch (Exception e) {
            _logger.error("Error updating domain object");
            throw new ComputeException("Error updating domain object",e);
        }
    }

    @Override
    public EntityRelationship updateRelationship(Access access, EntityRelationship relationship) throws ComputeException {
        try {
            Entity entity = _annotationDAO.getEntityByEntityDataId(access.getSubjectKey(), relationship.getId());
            EntityData entityData = getEntityData(entity, relationship.getId());
            
            entityData.setCreationDate(relationship.getCreationDate());
            entityData.setUpdatedDate(relationship.getUpdatedDate());
            entityData.setEntityAttrName(relationship.getType());
            entityData.setOwnerKey(entity.getOwnerKey());
            entityData.setOrderIndex(relationship.getOrderIndex());
            
            checkAttributeTypes(entityData);
            _annotationDAO.saveOrUpdateEntityData(entityData);
            
            updateIndex(entity);
            return convert(entityData);
        } 
        catch (Exception e) {
            _logger.error("Error updating relationship");
            throw new ComputeException("Error updating relationship",e);
        }
    }

    @Override
    public EntityNode setOrUpdateObjectAttrValue(Access access, Long objGuid, String attributeName, String value) throws ComputeException {
        try {
            String subjectKey = access.getSubjectKey();
            Entity entity = _annotationDAO.getEntityById(subjectKey, objGuid);
            if (entity==null) {
                throw new Exception("Entity not found: "+objGuid);
            }
            if (subjectKey!=null && !EntityUtils.hasWriteAccess(entity, _annotationDAO.getSubjectKeys(subjectKey))) {
                throw new ComputeException("Subject "+subjectKey+" cannot change "+objGuid);
            }
            
            int c = 0;
            for(EntityData entityData : entity.getEntityData()) {
                if (entityData.getEntityAttrName().equals(attributeName)) {
                    entityData.setValue(value);
                    c++;
                }
            }
            
            if (c>1) {
                throw new ComputeException("More than one "+attributeName+" value was found on entity "+objGuid);
            }
            
            if (c==0) {
                entity.setValueByAttributeName(attributeName, value);
            }
            
            _annotationDAO.saveOrUpdateEntity(entity);
            return (EntityNode)factory.getNodeInstance(entity);

        } 
         catch (Exception e) {
            _logger.error("Error trying to get delete entity "+objGuid, e);
            throw new ComputeException("Error deleting entity "+objGuid,e);
        }    
    }

    @Override
    public EntityRelationship setOrUpdateChildIndex(Access access, Long relGuid, Integer orderIndex) throws ComputeException {
        try {
            String subjectKey = access.getSubjectKey();
            Entity parent = _annotationDAO.getEntityByEntityDataId(subjectKey, relGuid);
            if (subjectKey!=null && !EntityUtils.hasWriteAccess(parent, _annotationDAO.getSubjectKeys(subjectKey))) {
                throw new ComputeException("Subject "+subjectKey+" cannot change "+parent.getId());
            }

            EntityData entityData = getEntityData(parent, relGuid);
            
            if (entityData==null) {
                throw new Exception("Could not retrieve parent of entity data "+relGuid);
            }
            
            entityData.setOrderIndex(orderIndex);
            _annotationDAO.saveOrUpdateEntityData(entityData);

            if (entityData.getParentEntity()!=null) {
                updateIndex(entityData.getParentEntity());
            }
            return (EntityRelationship)factory.getRelationshipInstance(entityData);
         } 
         catch (Exception e) {
            _logger.error("Error trying to update order index for "+orderIndex, e);
            throw new ComputeException("Error trying to update order index for "+orderIndex,e);
         }
    }

    @Override
    public EntityPermission updatePermission(Access access, EntityPermission permission) throws ComputeException {
        try {
            EntityActorPermission eap = _annotationDAO.getEntityActorPermission(permission.getId());
            eap.setPermissions((permission.canRead()?"r":"")+(permission.canWrite()?"w":""));
            String subjectKey = access.getSubjectKey();
            if (subjectKey!=null) {
                if (!subjectKey.equals(eap.getEntity().getOwnerKey())) {
                    throw new ComputeException("User "+subjectKey+" does not have the right to grant access to "+eap.getEntity().getId());
                }   
            }
            _annotationDAO.saveOrUpdate(eap);
            return (EntityPermission)factory.getPermissionInstance(eap);
        }
        catch (Exception e) {
            _logger.error("Error saving permission", e);
            throw new ComputeException("Error saving permission",e);
        }
    }

    @Override
    public Set<EntityPermission> getFullPermissions(Access access, Long objGuid) throws ComputeException {
        try {
            Set<EntityPermission> permissions = new HashSet<EntityPermission>();
            String subjectKey = access.getSubjectKey();
            for(EntityActorPermission eap : _annotationDAO.getFullPermissions(subjectKey, objGuid)) {
                permissions.add((EntityPermission)factory.getPermissionInstance(eap));
            }
            return permissions;
        } 
        catch (Exception e) {
            _logger.error("Error getting full permissions for "+objGuid, e);
            throw new ComputeException("Error getting full permissions for "+objGuid, e);
        }
    }

    @Override
    public EntityPermission grantPermissions(Access access, Long objGuid, String granteeKey, String permissions, boolean recursive) throws ComputeException {
        try {
            String subjectKey = access.getSubjectKey();
            return (EntityPermission)factory.getPermissionInstance(_annotationDAO.grantPermissions(subjectKey, objGuid, granteeKey, permissions, recursive));
        }
        catch (Exception e) {
            _logger.error("Error granting "+permissions+" permission for "+objGuid+" to "+granteeKey, e);
            throw new ComputeException("Error granting permission",e);
        }
    }

    @Override
    public void revokePermissions(Access access, Long objGuid, String revokeeKey, boolean recursive) throws ComputeException {
        try {
            String subjectKey = access.getSubjectKey();
            _annotationDAO.revokePermissions(subjectKey, objGuid, revokeeKey, recursive);
        }
        catch (Exception e) {
            _logger.error("Error revoking permission for "+objGuid+" to "+revokeeKey, e);
            throw new ComputeException("Error revoking permission",e);
        }
    }

    @Override
    public boolean deleteEntityNodeTree(Access access, Long objGuid) throws ComputeException {
        return deleteEntityNodeTree(access, objGuid, false);
    }

    @Override
    public boolean deleteEntityNodeTree(Access access, Long objGuid, boolean unlinkMultipleIncomingRelations) throws ComputeException {
        try {
            String subjectKey = access.getSubjectKey();
            Entity currEntity = _annotationDAO.getEntityById(subjectKey, objGuid);
            if (currEntity==null) {
                throw new Exception("Entity not found: "+objGuid);
            }
            if (subjectKey!=null && !EntityUtils.hasWriteAccess(currEntity, _annotationDAO.getSubjectKeys(subjectKey))) {
                throw new ComputeException("Subject "+subjectKey+" cannot change "+objGuid);
            }
            _annotationDAO.deleteEntityTree(subjectKey, currEntity, unlinkMultipleIncomingRelations);
            _logger.info(subjectKey+" deleted entity tree "+objGuid);
            return true;
        }
        catch (Exception e) {
            _logger.error("Error deleting domain object tree "+objGuid,e);
            throw new ComputeException("Error deleting domain object tree "+objGuid,e);
        }
    }

    @Override
    public void deleteRelationship(Access access, Long relGuid) throws ComputeException {
        try {
            String subjectKey = access.getSubjectKey();
            Entity currEntity = _annotationDAO.getEntityByEntityDataId(subjectKey, relGuid);
            if (currEntity==null) {
                throw new Exception("Parent entity not found for entity data with id="+relGuid);
            }
            if (subjectKey!=null && !EntityUtils.hasWriteAccess(currEntity, _annotationDAO.getSubjectKeys(subjectKey))) {
                throw new ComputeException("Subject "+subjectKey+" cannot delete entity data "+relGuid);
            }
            EntityData toDelete = getEntityData(currEntity, relGuid);
            _annotationDAO.deleteEntityData(toDelete);
            _logger.info(subjectKey+" deleted entity data "+relGuid);
        }
        catch (Exception e) {
            _logger.error("Unexpected error while trying to delete relationship "+relGuid, e);
            throw new ComputeException("Unexpected error while trying to delete relationship "+relGuid,e);
        }
    }

    // LOCAL interface

    @Override
    public void loadRelationships(EntityNode entityNode, boolean recurse) throws ComputeException {
        try {
            loadRelationships(entityNode, recurse, new HashSet<Long>());
        } 
        catch (Exception e) {
            _logger.error("Error loading relationships for "+entityNode.getId(), e);
            throw new ComputeException("Error loading relationships for "+entityNode.getId(), e);
        }
    }

    public void loadRelationships(EntityNode entityNode, boolean recurse, Set<Long> visited) throws ComputeException {

        try {
            if (entityNode==null) return;
            if (visited.contains(entityNode.getId())) return;
            visited.add(entityNode.getId());
            
            if (!entityNode.isRelsInit()) {
                
                // Here we take advantage of the fact that even though the relationships are not initialized, the 
                // Relationship objects were loaded already with EntityEntityNodeStubs
                
                Map<Long,EntityRelationship> relByChildId = new HashMap<Long,EntityRelationship>();
                for(EntityRelationship rel : entityNode.getRelationships()) {
                    relByChildId.put(rel.getTargetNode().getId(), rel);
                }
                
                for(Entity child : _annotationDAO.getChildEntities((String)null, entityNode.getId())) {
                    EntityRelationship rel = relByChildId.get(child.getId());
                    if (rel==null) {
                        throw new DaoException("Cannot find relationship from "+entityNode.getId()+" to "+child.getId());
                    }
                    rel.setTargetNode(convert(child));
                }   
            }
    
            if (recurse) {
                for(EntityRelationship rel : entityNode.getRelationships()) {
                    loadRelationships(rel.getTargetNode(), true, visited);
                }
            }
        }
        catch (Exception e) {
            _logger.error("Error loading relationships for "+entityNode.getId(), e);
            throw new ComputeException("Error loading relationships for "+entityNode.getId(), e);
        }
    }

    @Override
    public int bulkUpdateAttributeValue(String oldValue, String newValue) throws ComputeException {
        try {
            return _annotationDAO.bulkUpdateEntityDataValue(oldValue, newValue);
        }
        catch (Exception e) {
            _logger.error("Error bulk updating attribute values", e);
            throw new ComputeException("Error bulk updating attribute values",e);
        }
    }

    @Override
    public int bulkUpdateAttributePrefix(String oldPrefix, String newPrefix) throws ComputeException {
        try {
            return _annotationDAO.bulkUpdateEntityDataPrefix(oldPrefix, newPrefix);
        }
        catch (Exception e) {
            _logger.error("Error bulk updating attribute values", e);
            throw new ComputeException("Error bulk updating attribute values",e);
        }
    }

    @Override
    public EntityNode annexEntityNodeTree(Access access, Long objGuid) throws ComputeException {
        String subjectKey = access.getSubjectKey();
        try {
            return convert(_annotationDAO.annexEntityTree(subjectKey, objGuid));
        } 
        catch (Exception e) {
            _logger.error("Error annexing domain object "+objGuid+" for "+subjectKey, e);
            throw new ComputeException("Error annexing domain object "+objGuid+" for "+subjectKey, e);
        }
    }
    
    // Private utility methods
    
    private void checkAttributeTypes(Entity... entities) {
        for(Entity entity : entities) {
            for(EntityData ed : entity.getEntityData()) {
                checkEntityTypeSupportsAttribute(entity.getEntityTypeName(), ed.getEntityAttrName());
            }
        }
    }

    private void checkAttributeTypes(EntityData... entityDatas) {
        for(EntityData ed : entityDatas) {
            checkEntityTypeSupportsAttribute(ed.getParentEntity().getEntityTypeName(), ed.getEntityAttrName());
        }
    }
    
    private void checkEntityTypeSupportsAttribute(String entityTypeName, String attrName) {
        EntityType entityType = _annotationDAO.getEntityTypeByName(entityTypeName);
        for(EntityAttribute attr : entityType.getAttributes()) {
            if (attr.getName().equals(attrName)) {
                return;
            }
        }
        
        throw new IllegalStateException("Entity type "+entityTypeName+" does not support attribute "+attrName);
    }

    private EntityData getEntityData(Entity entity, Long entityDataId) {
        for(EntityData ed : entity.getEntityData()) {
            if (ed.getId().equals(entityDataId)) {
                return ed;
            }
        }
        return null;
    }
    
    private List<EntityNode> convertEntities(Collection<Entity> entities) throws Exception {
        List<EntityNode> domainObjs = new ArrayList<EntityNode>();
        for(Entity entity : entities) {
            domainObjs.add(convert(entity));
        }
        return domainObjs;
    }

    private List<EntityRelationship> convertEntityDatas(Collection<EntityData> eds) throws Exception {
        List<EntityRelationship> domainObjs = new ArrayList<EntityRelationship>();
        for(EntityData ed : eds) {
            domainObjs.add(convert(ed));
        }
        return domainObjs;
    }
    
    private EntityNode convert(Entity entity) throws Exception {
        return (EntityNode)factory.getNodeInstance(entity);
    }

    private EntityRelationship convert(EntityData entityData) throws Exception  {
        return (EntityRelationship)factory.getRelationshipInstance(entityData);
    }
}
