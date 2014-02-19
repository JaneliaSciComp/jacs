
package org.janelia.it.jacs.compute.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Remote;

import org.janelia.it.jacs.compute.api.support.Access;
import org.janelia.it.jacs.compute.api.support.MappedId;
import org.janelia.it.jacs.model.graph.entity.EntityNode;
import org.janelia.it.jacs.model.graph.entity.EntityPermission;
import org.janelia.it.jacs.model.graph.entity.EntityRelationship;

/**
 * A remote CRUD interface for manipulating graph.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@Remote
public interface GraphBeanRemote {
    
    // CREATE
    
	public EntityNode createEntityNode(Access access, EntityNode entityNode) throws ComputeException;
    public EntityRelationship createRelationship(Access access, Long sourceObjGuid, Long targetObjGuid, Integer index, String relType) throws ComputeException;
    public void createRelationships(Access access, Long sourceObjGuid, List<Long> targetObjGuids, String relType) throws ComputeException;
    
    // READ
    
    public EntityNode getEntityNode(Access access, Long objGuid) throws ComputeException;
    public EntityNode getEntityNodeTree(Access access, Long objGuid) throws ComputeException;
    public EntityNode getEntityNodeAndChildren(Access access, Long objGuid) throws ComputeException;
    public Collection<EntityNode> getIncomingRelatedObjects(Access access, Long objGuid) throws ComputeException;
    public Collection<EntityNode> getOutgoingRelatedObjects(Access access, Long objGuid) throws ComputeException;
   
    public Collection<EntityNode> getEntityNodes(Access access, List<Long> objGuid) throws ComputeException;
    public Collection<EntityNode> getEntityNodesByName(Access access, String name) throws ComputeException;
    public Collection<EntityNode> getEntityNodesByTypeName(Access access, String typeName) throws ComputeException;
    public Collection<EntityNode> getEntityNodesByNameAndTypeName(Access access, String name, String typeName) throws ComputeException;
    public Collection<EntityNode> getEntityNodesWithAttributeValue(Access access, String attrName, String attrValue) throws ComputeException;
    public Collection<EntityNode> getEntityNodesWithAttributeValueAndTypeName(Access access, String attrName, String attrValue, String typeName) throws ComputeException;
    public Collection<EntityNode> getEntityNodesWithTag(Access access, String attrTag) throws ComputeException;
    
    public Collection<EntityRelationship> getIncomingRelationships(Access access, Long targetObjGuid) throws ComputeException;
    public Collection<Long> getEntityNodeGuidsWithIncomingRelationshipsTo(Access access, Long targetObjGuid, String attrName) throws ComputeException;

    public long getCountEntityNodesWithAttributeValue(Access access, String attrName, String attrValue) throws ComputeException;
    public EntityNode getAncestorWithType(Access access, Long objGuid, String typeName) throws ComputeException;
    public List<List<EntityRelationship>> getPathsToRoots(Access access, Long objGuid) throws ComputeException;
    public List<MappedId> getProjectedResults(Access access, List<Long> objGuids, List<String> upMapping, List<String> downMapping) throws ComputeException;
    public Map<Long,String> getChildEntityNodeNames(Access access, Long objGuid) throws ComputeException;
    
    // UPDATE
    
    public EntityNode updateEntityNode(Access access, EntityNode entityNode) throws ComputeException;
    public EntityRelationship updateRelationship(Access access, EntityRelationship relationship) throws ComputeException;
    public EntityPermission updatePermission(Access access, EntityPermission permission) throws ComputeException;
    
    public EntityNode setOrUpdateObjectAttrValue(Access access, Long objGuid, String attributeName, String value) throws ComputeException;
    public EntityRelationship setOrUpdateChildIndex(Access access, Long relGuid, Integer orderIndex) throws ComputeException;
    
    public Set<EntityPermission> getFullPermissions(Access access, Long objGuid) throws ComputeException;
    public EntityPermission grantPermissions(Access access, Long objGuid, String granteeKey, String permissions, boolean recursive) throws ComputeException;
    public void revokePermissions(Access access, Long objGuid, String revokeeKey, boolean recursive) throws ComputeException;
    
    // DELETE
    
    public boolean deleteEntityNodeTree(Access access, Long objGuid) throws ComputeException;
    public boolean deleteEntityNodeTree(Access access, Long objGuid, boolean unlinkMultipleIncomingRelations) throws ComputeException;
    public void deleteRelationship(Access access, Long relGuid) throws ComputeException;
}
