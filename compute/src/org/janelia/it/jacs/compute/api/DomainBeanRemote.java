
package org.janelia.it.jacs.compute.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Remote;

import org.janelia.it.jacs.compute.api.support.Access;
import org.janelia.it.jacs.compute.api.support.MappedId;
import org.janelia.it.jacs.model.domain.interfaces.metamodel.DomainObject;
import org.janelia.it.jacs.model.domain.interfaces.metamodel.Permission;
import org.janelia.it.jacs.model.domain.interfaces.metamodel.Relationship;

/**
 * A remote CRUD interface for manipulating Domain Objects.  
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@Remote
public interface DomainBeanRemote {
    
    // CREATE
    
	public DomainObject createDomainObject(Access access, DomainObject domainObject) throws ComputeException;
    public Relationship createRelationship(Access access, Long sourceObjGuid, Long targetObjGuid, Integer index, String relType) throws ComputeException;
    public void createRelationships(Access access, Long sourceObjGuid, List<Long> targetObjGuids, String relType) throws ComputeException;
    
    // READ
    
    public DomainObject getDomainObject(Access access, Long objGuid) throws ComputeException;
    public DomainObject getDomainObjectTree(Access access, Long objGuid) throws ComputeException;
    public DomainObject getDomainObjectAndChildren(Access access, Long objGuid) throws ComputeException;
    public Collection<DomainObject> getIncomingRelatedObjects(Access access, Long objGuid) throws ComputeException;
    public Collection<DomainObject> getOutgoingRelatedObjects(Access access, Long objGuid) throws ComputeException;
   
    public Collection<DomainObject> getDomainObjectsBy(Access access, List<Long> objGuid) throws ComputeException;
    public Collection<DomainObject> getDomainObjectsByName(Access access, String name) throws ComputeException;
    public Collection<DomainObject> getDomainObjectsByTypeName(Access access, String typeName) throws ComputeException;
    public Collection<DomainObject> getDomainObjectsByNameAndTypeName(Access access, String name, String typeName) throws ComputeException;
    public Collection<DomainObject> getDomainObjectsWithAttributeValue(Access access, String attrName, String attrValue) throws ComputeException;
    public Collection<DomainObject> getDomainObjectsWithAttributeValueAndTypeName(Access access, String attrName, String attrValue, String typeName) throws ComputeException;
    public Collection<DomainObject> getDomainObjectsWithTag(Access access, String attrTag) throws ComputeException;
    
    public Collection<Relationship> getIncomingRelationships(Access access, Long targetObjGuid) throws ComputeException;
    public Collection<Long> getDomainObjectGuidsWithIncomingRelationshipsTo(Access access, Long targetObjGuid, String attrName) throws ComputeException;

    public long getCountDomainObjectsWithAttributeValue(Access access, String attrName, String attrValue) throws ComputeException;
    public DomainObject getAncestorWithType(Access access, Long objGuid, String typeName) throws ComputeException;
    public List<List<Relationship>> getPathsToRoots(Access access, Long objGuid) throws ComputeException;
    public List<MappedId> getProjectedResults(Access access, List<Long> objGuids, List<String> upMapping, List<String> downMapping) throws ComputeException;
    public Map<Long,String> getChildDomainObjectNames(Access access, Long objGuid) throws ComputeException;
    
    // UPDATE
    
    public DomainObject updateDomainObject(Access access, DomainObject domainObject) throws ComputeException;
    public Relationship updateRelationship(Access access, Relationship relationship) throws ComputeException;
    public Permission updatePermission(Access access, Permission permission) throws ComputeException;
    
    public DomainObject setOrUpdateObjectAttrValue(Access access, Long objGuid, String attributeName, String value) throws ComputeException;
    public DomainObject setOrUpdateRelAttrValue(Access access, Long objGuid, String attributeName, String value) throws ComputeException;
    public Relationship setOrUpdateChildIndex(Access access, Long relGuid, Integer orderIndex) throws ComputeException;
    
    public Set<Permission> getFullPermissions(Access access, Long objGuid) throws ComputeException;
    public Permission grantPermissions(Access access, Long objGuid, String granteeKey, String permissions, boolean recursive) throws ComputeException;
    public void revokePermissions(Access access, Long objGuid, String granteeKey,  boolean recursive) throws ComputeException;
    
    // DELETE
    
    public boolean deleteDomainObjectTree(Access access, Long objGuid) throws ComputeException;
    public boolean deleteDomainObjectTree(Access access, Long objGuid, boolean unlinkMultipleIncomingRelations) throws ComputeException;
    public void deleteRelationship(Access access, Long relGuid) throws ComputeException;
}
