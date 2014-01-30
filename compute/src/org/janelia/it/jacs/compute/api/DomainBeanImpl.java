
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
import org.janelia.it.jacs.model.domain.impl.entity.EntityDomainObjectFactory;
import org.janelia.it.jacs.model.domain.interfaces.metamodel.DomainObject;
import org.janelia.it.jacs.model.domain.interfaces.metamodel.Permission;
import org.janelia.it.jacs.model.domain.interfaces.metamodel.Relationship;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityActorPermission;
import org.janelia.it.jacs.model.entity.EntityAttribute;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.EntityType;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.jboss.annotation.ejb.PoolClass;
import org.jboss.annotation.ejb.TransactionTimeout;

/**
 * Implementation of local and remote domain beans.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@Stateless(name = "DomainEJB")
@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
@TransactionTimeout(432000)
@PoolClass(value = org.jboss.ejb3.StrictMaxPool.class, maxSize = 500, timeout = 10000)
public class DomainBeanImpl implements DomainBeanLocal, DomainBeanRemote {
	
    private static final Logger _logger = Logger.getLogger(DomainBeanImpl.class);
    private final AnnotationDAO _annotationDAO = new AnnotationDAO(_logger);
    private final EntityDomainObjectFactory factory = new EntityDomainObjectFactory();
    
    private void updateIndex(Entity entity) {
    	IndexingHelper.updateIndex(entity.getId());
    }
   
    public DomainObject createDomainObject(Access access, DomainObject domainObject) throws ComputeException {
        try {
            String subjectKey = access.getSubjectKey();
            
            if (domainObject.getOwnerKey()!=null && !domainObject.getOwnerKey().equals(subjectKey)) {
                throw new IllegalArgumentException(subjectKey+" cannot create an object for another user ("+domainObject.getOwnerKey()+")");
            }
            
            Date creationDate = domainObject.getCreationDate();
            Date updatedDate = domainObject.getCreationDate();
            Date now = new Date();
            
            if (creationDate == null) {
                creationDate = now;
            }
            
            if (updatedDate == null) {
                updatedDate = now;
            }
            
            Entity entity = new Entity(null, domainObject.getName(), subjectKey, domainObject.getTypeName(), creationDate, updatedDate, new HashSet<EntityData>());
            for(String key : domainObject.getAttributes().keySet()) {
                entity.setValueByAttributeName(key, domainObject.getAttributeValue(key));
            }
            
            _annotationDAO.saveOrUpdate(entity);
            
            _logger.info(subjectKey+" created entity "+entity.getId());
            updateIndex(entity);
            return factory.getDomainObject(entity);
        } 
        catch (DaoException e) {
            _logger.error("Error creating domain object",e);
            throw new ComputeException("Error creating domain object",e);
        }
    }
    
    public Relationship createRelationship(Access access, Long sourceObjGuid, Long targetObjGuid, Integer index, String relType) throws ComputeException {
        return createRelationship(access, sourceObjGuid, targetObjGuid, index, relType, null);
    }
    
    public Relationship createRelationship(Access access, Long sourceObjGuid, Long targetObjGuid, Integer index, String relType, String value) throws ComputeException {
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
            
            return factory.getRelationship(ed);
        } 
        catch (DaoException e) {
            _logger.error("Error creating relationship from "+sourceObjGuid+" to "+targetObjGuid, e);
            throw new ComputeException("Error creating relationship from "+sourceObjGuid+" to "+targetObjGuid,e);
        }
    }
    
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
    
    public DomainObject getDomainObject(Access access, Long objGuid) throws ComputeException {
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
    
    public DomainObject getDomainObjectTree(Access access, Long objGuid) throws ComputeException {
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
    
    public DomainObject getDomainObjectAndChildren(Access access, Long objGuid) throws ComputeException {
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
    
    public Collection<DomainObject> getIncomingRelatedObjects(Access access, Long objGuid) throws ComputeException {
        try {
            String subjectKey = access.getSubjectKey();     
            switch (access.getAccessPattern()) {
            case ALL_ACCESSIBLE_OBJECTS: return convertEntities(_annotationDAO.getParentEntities(subjectKey, objGuid));
            case OWNED_OBJECTS_ONLY: throw new IllegalArgumentException("This method does not support the OWNED_OBJECTS_ONLY access pattern");
            }
            return null;
        } 
        catch (DaoException e) {
            _logger.error("Error getting incoming related objects for domain object "+objGuid, e);
            throw new ComputeException("Error getting incoming related objects for domain object "+objGuid,e);
        }
    }
    
    public Collection<DomainObject> getOutgoingRelatedObjects(Access access, Long objGuid) throws ComputeException {
        try {
            String subjectKey = access.getSubjectKey();     
            switch (access.getAccessPattern()) {
            case ALL_ACCESSIBLE_OBJECTS: return convertEntities(_annotationDAO.getChildEntities(subjectKey, objGuid));
            case OWNED_OBJECTS_ONLY: throw new IllegalArgumentException("This method does not support the OWNED_OBJECTS_ONLY access pattern");
            }
            return null;
        } 
        catch (DaoException e) {
            _logger.error("Error getting outgoing related objects for domain object "+objGuid, e);
            throw new ComputeException("Error getting outgoing related objects for domain object "+objGuid,e);
        }
    }
   
    public Collection<DomainObject> getDomainObjects(Access access, List<Long> objGuids) throws ComputeException {
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
    
    public Collection<DomainObject> getDomainObjectsByName(Access access, String name) throws ComputeException {
        String subjectKey = access.getSubjectKey();
        try {
            switch (access.getAccessPattern()) {
            case ALL_ACCESSIBLE_OBJECTS: return convertEntities(_annotationDAO.getEntitiesByName(subjectKey, name));
            case OWNED_OBJECTS_ONLY: return convertEntities(_annotationDAO.getUserEntitiesByName(subjectKey, name));
            }
            return null;
        }
        catch (DaoException e) {
            _logger.error("Error trying to get the domain objects with name "+name+" for user "+subjectKey, e);
            throw new ComputeException("Error trying to get the domain objects with name "+name+" for user "+subjectKey,e);
        }
    }
    
    public Collection<DomainObject> getDomainObjectsByTypeName(Access access, String typeName) throws ComputeException {
        String subjectKey = access.getSubjectKey();
        try {
            switch (access.getAccessPattern()) {
            case ALL_ACCESSIBLE_OBJECTS: return convertEntities(_annotationDAO.getEntitiesByTypeName(subjectKey, typeName));
            case OWNED_OBJECTS_ONLY: return convertEntities(_annotationDAO.getUserEntitiesByTypeName(subjectKey, typeName));
            }
            return null;
        }
        catch (DaoException e) {
            _logger.error("Error trying to get the domain objects with type "+typeName+" for user "+subjectKey, e);
            throw new ComputeException("Error trying to get the domain objects with type "+typeName+" for user "+subjectKey,e);
        }
    }
    
    public Collection<DomainObject> getDomainObjectsByNameAndTypeName(Access access, String name, String typeName) throws ComputeException {
        String subjectKey = access.getSubjectKey();
        try {
            switch (access.getAccessPattern()) {
            case ALL_ACCESSIBLE_OBJECTS: return convertEntities(_annotationDAO.getEntitiesByNameAndTypeName(subjectKey, name, typeName));
            case OWNED_OBJECTS_ONLY: return convertEntities(_annotationDAO.getUserEntitiesByNameAndTypeName(subjectKey, name, typeName));
            }
            return null;
        }
        catch (DaoException e) {
            _logger.error("Error trying to get the domain objects with name "+name+" and type "+typeName+" for user "+subjectKey, e);
            throw new ComputeException("Error trying to get the domain objects with name "+name+" and type "+typeName+" for user "+subjectKey,e);
        }
    }
    
    public Collection<DomainObject> getDomainObjectsWithAttributeValue(Access access, String attrName, String attrValue) throws ComputeException {
        String subjectKey = access.getSubjectKey();
        try {
            switch (access.getAccessPattern()) {
            case ALL_ACCESSIBLE_OBJECTS: return convertEntities(_annotationDAO.getEntitiesWithAttributeValue(subjectKey, attrName, attrValue));
            case OWNED_OBJECTS_ONLY: return convertEntities(_annotationDAO.getUserEntitiesWithAttributeValue(subjectKey, attrName, attrValue));
            }
            return null;
        }
        catch (DaoException e) {
            _logger.error("Error getting domain objects with "+attrName+" like "+attrValue+" for "+subjectKey, e);
            throw new ComputeException("Error getting domain objects with "+attrName+" like "+attrValue+" for "+subjectKey,e);
        }
    }
    
    public Collection<DomainObject> getDomainObjectsWithAttributeValueAndTypeName(Access access, String attrName, String attrValue, String typeName) throws ComputeException {
        String subjectKey = access.getSubjectKey();
        try {
            switch (access.getAccessPattern()) {
            case ALL_ACCESSIBLE_OBJECTS: return convertEntities(_annotationDAO.getEntitiesWithAttributeValue(subjectKey, typeName, attrName, attrValue));
            case OWNED_OBJECTS_ONLY: return convertEntities(_annotationDAO.getUserEntitiesWithAttributeValue(subjectKey, typeName, attrName, attrValue));
            }
            return null;
        }
        catch (DaoException e) {
            _logger.error("Error getting domain objects with type "+typeName+" and "+attrName+" like "+attrValue+" for "+subjectKey, e);
            throw new ComputeException("Error getting domain objects with type "+typeName+" and "+attrName+" like "+attrValue+" for "+subjectKey,e);
        }
    }
    
    public Collection<DomainObject> getDomainObjectsWithTag(Access access, String attrTag) throws ComputeException {
        String subjectKey = access.getSubjectKey();
        try {
            switch (access.getAccessPattern()) {
            case ALL_ACCESSIBLE_OBJECTS: return convertEntities(_annotationDAO.getEntitiesWithTag(subjectKey, attrTag));
            case OWNED_OBJECTS_ONLY: throw new IllegalArgumentException("This method does not support the OWNED_OBJECTS_ONLY access pattern");
            }
            return null;
        }
        catch (DaoException e) {
            _logger.error("Error getting domain objects with tag "+attrTag+" for "+subjectKey, e);
            throw new ComputeException("Error getting domain objects with tag "+attrTag+" for "+subjectKey,e);
        }
    }
    
    public Collection<Relationship> getIncomingRelationships(Access access, Long targetObjGuid) throws ComputeException {
        String subjectKey = access.getSubjectKey();
        try {
            switch (access.getAccessPattern()) {
            case ALL_ACCESSIBLE_OBJECTS: return convertEntityDatas(_annotationDAO.getParentEntityDatas(subjectKey, targetObjGuid));
            case OWNED_OBJECTS_ONLY: throw new IllegalArgumentException("This method does not support the OWNED_OBJECTS_ONLY access pattern");
            }
            return null;
        }
        catch (DaoException e) {
            _logger.error("Error getting incoming relationships for domain object "+targetObjGuid+" for "+subjectKey, e);
            throw new ComputeException("Error getting incoming relationships for domain object "+targetObjGuid+" for "+subjectKey,e);
        }
    }
    
    public Collection<Long> getDomainObjectGuidsWithIncomingRelationshipsTo(Access access, Long targetObjGuid, String attrName) throws ComputeException {
        String subjectKey = access.getSubjectKey();
        try {
            switch (access.getAccessPattern()) {
            case ALL_ACCESSIBLE_OBJECTS: return _annotationDAO.getParentIdsForAttribute(subjectKey, targetObjGuid, attrName);
            case OWNED_OBJECTS_ONLY: throw new IllegalArgumentException("This method does not support the OWNED_OBJECTS_ONLY access pattern");
            }
            return null;
        }
        catch (DaoException e) {
            _logger.error("Error getting incoming relationships for domain object "+targetObjGuid+" for "+subjectKey, e);
            throw new ComputeException("Error getting incoming relationships for domain object "+targetObjGuid+" for "+subjectKey,e);
        }
    }
    
    public long getCountDomainObjectsWithAttributeValue(Access access, String attrName, String attrValue) throws ComputeException {
        String subjectKey = access.getSubjectKey();
        try {
            switch (access.getAccessPattern()) {
            case ALL_ACCESSIBLE_OBJECTS: _logger.warn("getCountDomainObjectsWithAttributeValue does not support ALL_ACCESSIBLE_OBJECTS access pattern. Falling back to OWNED_OBJECTS_ONLY."); 
            case OWNED_OBJECTS_ONLY: 
            }
            return _annotationDAO.getCountUserEntitiesWithAttributeValue(subjectKey, attrName, attrValue);
        }
        catch (DaoException e) {
            _logger.error("Error counting domain objects with attribute "+attrName+" value "+attrValue+" for "+subjectKey, e);
            throw new ComputeException("Error counting domain objects with attribute "+attrName+" value "+attrValue+" for "+subjectKey,e);
        }
    }
    
    public DomainObject getAncestorWithType(Access access, Long objGuid, String typeName) throws ComputeException {
        String subjectKey = access.getSubjectKey();
        try {
            switch (access.getAccessPattern()) {
            case ALL_ACCESSIBLE_OBJECTS: return convert(_annotationDAO.getAncestorWithType(subjectKey, objGuid, typeName));
            case OWNED_OBJECTS_ONLY: throw new IllegalArgumentException("This method does not support the OWNED_OBJECTS_ONLY access pattern");
            }
            return null;
        }
        catch (DaoException e) {
            _logger.error("Error getting ancestors of type "+typeName+" for domain object "+objGuid+" for "+subjectKey, e);
            throw new ComputeException("Error getting ancestors of type "+typeName+" for domain object "+objGuid+" for "+subjectKey,e);
        }
    }
    
    public List<List<Relationship>> getPathsToRoots(Access access, Long objGuid) throws ComputeException {
        String subjectKey = access.getSubjectKey();
        try {
            if (access.getAccessPattern().equals(AccessPattern.OWNED_OBJECTS_ONLY)) {
                throw new IllegalArgumentException("This method does not support the OWNED_OBJECTS_ONLY access pattern");
            }
        
            List<List<Relationship>> paths = new ArrayList<List<Relationship>>(); 
            EntityData fakeEd = new EntityData();
            fakeEd.setParentEntity(_annotationDAO.getEntityById(objGuid));
            for(List<EntityData> edList : _annotationDAO.getEntityDataPathsToRoots(subjectKey, fakeEd)) {
                paths.add((List<Relationship>)convertEntityDatas(edList));
            }
            return paths;
        }
        catch (DaoException e) {
            _logger.error("Error getting paths to root for domain object "+objGuid+" for "+subjectKey, e);
            throw new ComputeException("Error getting paths to root for domain object "+objGuid+" for "+subjectKey,e);
        }
    }
    
    public List<MappedId> getProjectedResults(Access access, List<Long> objGuids, List<String> upMapping, List<String> downMapping) throws ComputeException {
        String subjectKey = access.getSubjectKey();
        try {
            if (access.getAccessPattern().equals(AccessPattern.OWNED_OBJECTS_ONLY)) {
                throw new IllegalArgumentException("This method does not support the OWNED_OBJECTS_ONLY access pattern");
            }
            return _annotationDAO.getProjectedResults(subjectKey, objGuids, upMapping, downMapping);
        } 
        catch (DaoException e) {
            _logger.error("Error getting projected results for "+subjectKey, e);
            throw new ComputeException("Error getting projected results for "+subjectKey, e);
        }
    }

    @Override
    public List<Long> getEntityIdsInAlignmentSpace(Access access, String opticalRes, String pixelRes, List<Long> guids) throws ComputeException {
        try {
            if ( access.getAccessPattern().equals(AccessPattern.OWNED_OBJECTS_ONLY)) {
                throw new IllegalArgumentException("This method does not support the OWNED_OBJECTS_ONLY access pattern");
            }
            return _annotationDAO.getEntityIdsInAlignmentSpace( opticalRes, pixelRes, guids);

        } catch ( DaoException daoe ) {
            _logger.error("Error getting applicable subset of entity ids for an alignment space.");
            throw new ComputeException("Error paring down entity ids for list", daoe);
        }
    }
    
    public Map<Long,String> getChildDomainObjectNames(Access access, Long objGuid) throws ComputeException {
        String subjectKey = access.getSubjectKey();
        try {
            if (access.getAccessPattern().equals(AccessPattern.OWNED_OBJECTS_ONLY)) {
                throw new IllegalArgumentException("This method does not support the OWNED_OBJECTS_ONLY access pattern");
            }
            // TODO: implement access control
            return _annotationDAO.getChildEntityNames(objGuid);
        } 
        catch (DaoException e) {
            _logger.error("Error getting projected results for "+subjectKey, e);
            throw new ComputeException("Error getting projected results for "+subjectKey, e);
        }
    }
    
    public DomainObject updateDomainObject(Access access, DomainObject domainObject) throws ComputeException {
        try {
            Entity entity = _annotationDAO.getEntityById(domainObject.getGuid());
            entity.setName(domainObject.getName());
            entity.setCreationDate(domainObject.getCreationDate());
            entity.setUpdatedDate(domainObject.getUpdatedDate());
            entity.setEntityTypeName(domainObject.getTypeName());
            entity.setOwnerKey(domainObject.getOwnerKey());
            entity.setNumChildren(domainObject.getNumRelationships());
            
            for(Entry<String,String> entry : domainObject.getAttributes().entries()) {
                entity.setValueByAttributeName(entry.getKey(), entry.getValue());
            }
            
            Set<EntityData> toRemove = new HashSet<EntityData>();
            for(EntityData ed : entity.getEntityData()) {
                if (ed.getChildEntity()==null && !domainObject.getAttributes().containsKey(ed.getEntityAttrName())) {
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
        catch (DaoException e) {
            _logger.error("Error updating domain object");
            throw new ComputeException("Error updating domain object",e);
        }
    }
    
    public Relationship updateRelationship(Access access, Relationship relationship) throws ComputeException {
        try {
            Entity entity = _annotationDAO.getEntityByEntityDataId(access.getSubjectKey(), relationship.getGuid());
            EntityData entityData = getEntityData(entity, relationship.getGuid());
            
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
        catch (DaoException e) {
            _logger.error("Error updating relationship");
            throw new ComputeException("Error updating relationship",e);
        }
    }
    
    public DomainObject setOrUpdateObjectAttrValue(Access access, Long objGuid, String attributeName, String value) throws ComputeException {
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
            return factory.getDomainObject(entity);

        } 
         catch (Exception e) {
            _logger.error("Error trying to get delete entity "+objGuid, e);
            throw new ComputeException("Error deleting entity "+objGuid,e);
        }    
    }
    
    public Relationship setOrUpdateChildIndex(Access access, Long relGuid, Integer orderIndex) throws ComputeException {
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
            return factory.getRelationship(entityData);
         } 
         catch (Exception e) {
            _logger.error("Error trying to update order index for "+orderIndex, e);
            throw new ComputeException("Error trying to update order index for "+orderIndex,e);
         }
    }
    
    public Permission updatePermission(Access access, Permission permission) throws ComputeException {
        try {
            EntityActorPermission eap = _annotationDAO.getEntityActorPermission(permission.getGuid());
            eap.setPermissions((permission.canRead()?"r":"")+(permission.canWrite()?"w":""));
            String subjectKey = access.getSubjectKey();
            if (subjectKey!=null) {
                if (!subjectKey.equals(eap.getEntity().getOwnerKey())) {
                    throw new ComputeException("User "+subjectKey+" does not have the right to grant access to "+eap.getEntity().getId());
                }   
            }
            _annotationDAO.saveOrUpdate(eap);
            return factory.getPermission(eap);
        }
        catch (DaoException e) {
            _logger.error("Error saving permission", e);
            throw new ComputeException("Error saving permission",e);
        }
    }
    
    public Set<Permission> getFullPermissions(Access access, Long objGuid) throws ComputeException {
        try {
            Set<Permission> permissions = new HashSet<Permission>();
            String subjectKey = access.getSubjectKey();
            for(EntityActorPermission eap : _annotationDAO.getFullPermissions(subjectKey, objGuid)) {
                permissions.add(factory.getPermission(eap));
            }
            return permissions;
        } 
        catch (DaoException e) {
            _logger.error("Error getting full permissions for "+objGuid, e);
            throw new ComputeException("Error getting full permissions for "+objGuid, e);
        }
    }
    
    public Permission grantPermissions(Access access, Long objGuid, String granteeKey, String permissions, boolean recursive) throws ComputeException {
        try {
            String subjectKey = access.getSubjectKey();
            return factory.getPermission(_annotationDAO.grantPermissions(subjectKey, objGuid, granteeKey, permissions, recursive));
        }
        catch (DaoException e) {
            _logger.error("Error granting "+permissions+" permission for "+objGuid+" to "+granteeKey, e);
            throw new ComputeException("Error granting permission",e);
        }
    }
    
    public void revokePermissions(Access access, Long objGuid, String revokeeKey, boolean recursive) throws ComputeException {
        try {
            String subjectKey = access.getSubjectKey();
            _annotationDAO.revokePermissions(subjectKey, objGuid, revokeeKey, recursive);
        }
        catch (DaoException e) {
            _logger.error("Error revoking permission for "+objGuid+" to "+revokeeKey, e);
            throw new ComputeException("Error revoking permission",e);
        }
    }
    
    public boolean deleteDomainObjectTree(Access access, Long objGuid) throws ComputeException {
        return deleteDomainObjectTree(access, objGuid, false);
    }
    
    public boolean deleteDomainObjectTree(Access access, Long objGuid, boolean unlinkMultipleIncomingRelations) throws ComputeException {
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
    
    public void loadRelationships(DomainObject domainObject, boolean recurse) throws ComputeException {
        try {
            loadRelationships(domainObject, recurse, new HashSet<Long>());
        } 
        catch (DaoException e) {
            _logger.error("Error loading relationships for "+domainObject.getGuid(), e);
            throw new ComputeException("Error loading relationships for "+domainObject.getGuid(), e);
        }
    }

    public void loadRelationships(DomainObject domainObject, boolean recurse, Set<Long> visited) throws DaoException {
        
        if (domainObject==null) return;
        if (visited.contains(domainObject.getGuid())) return;
        visited.add(domainObject.getGuid());
        
        if (!domainObject.relationshipsAreInitialized()) {
            
            // Here we take advantage of the fact that even though the relationships are not initialized, the 
            // Relationship objects were loaded already with EntityDomainObjectStubs
            
            Map<Long,Relationship> relByChildId = new HashMap<Long,Relationship>();
            for(Relationship rel : domainObject.getRelationships()) {
                relByChildId.put(rel.getTarget().getGuid(), rel);
            }
            
            for(Entity child : _annotationDAO.getChildEntities((String)null, domainObject.getGuid())) {
                Relationship rel = relByChildId.get(child.getId());
                if (rel==null) {
                    throw new DaoException("Cannot find relationship from "+domainObject.getGuid()+" to "+child.getId());
                }
                rel.setTarget(convert(child));
            }   
        }

        if (recurse) {
            for(Relationship rel : domainObject.getRelationships()) {
                loadRelationships(rel.getTarget(), true, visited);
            }
        }
    }
    
    public int bulkUpdateAttributeValue(String oldValue, String newValue) throws ComputeException {
        try {
            return _annotationDAO.bulkUpdateEntityDataValue(oldValue, newValue);
        }
        catch (Exception e) {
            _logger.error("Error bulk updating attribute values", e);
            throw new ComputeException("Error bulk updating attribute values",e);
        }
    }
    
    public int bulkUpdateAttributePrefix(String oldPrefix, String newPrefix) throws ComputeException {
        try {
            return _annotationDAO.bulkUpdateEntityDataPrefix(oldPrefix, newPrefix);
        }
        catch (Exception e) {
            _logger.error("Error bulk updating attribute values", e);
            throw new ComputeException("Error bulk updating attribute values",e);
        }
    }
    
    public DomainObject annexDomainObjectTree(Access access, Long objGuid) throws ComputeException {
        String subjectKey = access.getSubjectKey();
        try {
            return convert(_annotationDAO.annexEntityTree(subjectKey, objGuid));
        } 
        catch (DaoException e) {
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
    
    private List<DomainObject> convertEntities(Collection<Entity> entities) {
        List<DomainObject> domainObjs = new ArrayList<DomainObject>();
        for(Entity entity : entities) {
            domainObjs.add(convert(entity));
        }
        return domainObjs;
    }

    private List<Relationship> convertEntityDatas(Collection<EntityData> eds) {
        List<Relationship> domainObjs = new ArrayList<Relationship>();
        for(EntityData ed : eds) {
            domainObjs.add(convert(ed));
        }
        return domainObjs;
    }
    
    private DomainObject convert(Entity entity) {
        return factory.getDomainObject(entity);
    }

    private Relationship convert(EntityData entityData) {
        return factory.getRelationship(entityData);
    }
}
