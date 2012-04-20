package org.janelia.it.jacs.compute.api;

import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.api.support.MappedId;
import org.janelia.it.jacs.compute.api.support.EntityMapStep;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityAttribute;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.EntityType;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.jacs.model.ontology.types.OntologyElementType;
import org.janelia.it.jacs.model.tasks.Task;

import javax.ejb.Local;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Local
public interface AnnotationBeanLocal {

    public Entity saveOrUpdateEntity(Entity entity) throws ComputeException;
    public EntityData saveOrUpdateEntityData(EntityData newData) throws ComputeException;
    public Entity createEntity(String userLogin, String entityTypeName, String entityName) throws ComputeException;
    public EntityData addEntityToParent(Entity parent, Entity entity, Integer index, String attrName) throws ComputeException;
    
    public EntityType getEntityTypeByName(String name);
	public EntityAttribute getEntityAttributeByName(String name);
    public Set<Entity> getEntitiesByName(String name);
    public Entity getEntityById(String targetId);
    public Entity getEntityTree(Long id);
    public Entity getEntityTreeQuery(Long id);
    public List<Entity> getEntitiesById(List<Long> ids) throws ComputeException;
    public Entity getCachedEntityTree(Long id);
    public Entity getUserEntityById(String userLogin, long entityId);
    public boolean deleteEntityById(Long entityId);
    public boolean deleteEntityTree(String userLogin, long entityId) throws ComputeException;
    public boolean deleteSmallEntityTree(String userLogin, long entityId) throws ComputeException;
    public void removeEntityFromFolder(EntityData folderEntityData) throws ComputeException;
    public void deleteEntityData(EntityData ed) throws ComputeException;
    public Set<Entity> getParentEntities(long entityId);
    public Set<Entity> getChildEntities(long entityId);
    public Set<EntityData> getParentEntityDatas(long childEntityId);
    
    public List<EntityType> getEntityTypes();
    public List<EntityAttribute> getEntityAttributes();
    public List<Entity> getEntitiesByTypeName(String entityTypeName);
    public List<Entity> getCommonRootEntitiesByTypeName(String entityTypeName);
    public List<Entity> getCommonRootEntitiesByTypeName(String userLogin, String entityTypeName);
    
    public List<Entity> getAnnotationsForEntities(String username, List<Long> entityIds) throws ComputeException;
    public List<Entity> getAnnotationsForEntity(String username, long entityId) throws ComputeException;
    
    public List<Task> getAnnotationSessionTasks(String username) throws ComputeException;
    public List<Entity> getAnnotationsForSession(String username, long sessionId) throws ComputeException;
    public List<Entity> getEntitiesForAnnotationSession(String username, long sessionId) throws ComputeException;
    public List<Entity> getCategoriesForAnnotationSession(String username, long sessionId) throws ComputeException;
    public Set<Long> getCompletedEntityIds(long sessionId) throws ComputeException;
    
    public List<Entity> getEntitiesWithFilePath(String filePath);
    public Entity getFolderTree(Long id) throws ComputeException;
    
    public Entity createOntologyRoot(String userLogin, String rootName) throws ComputeException;
    public EntityData createOntologyTerm(String userLogin, Long ontologyTermParentId, String termName, OntologyElementType type, Integer orderIndex) throws ComputeException;
    public void removeOntologyTerm(String userLogin, Long ontologyTermId) throws ComputeException;
    public Entity cloneEntityTree(Long sourceRootId, String targetUserLogin, String targetRootName) throws ComputeException;
    public Entity publishOntology(Long sourceRootId, String targetRootName) throws ComputeException;
    public Entity getOntologyTree(String userLogin, Long id) throws ComputeException;
    public List<Entity> getPublicOntologies() throws ComputeException;
    public List<Entity> getPrivateOntologies(String userLogin) throws ComputeException;

	public Entity createOntologyAnnotation(String userLogin, OntologyAnnotation annotation) throws ComputeException;
	public void removeOntologyAnnotation(String userLogin, long annotationId) throws ComputeException;
	public void removeAllOntologyAnnotationsForSession(String userLogin, long sessionId) throws ComputeException;

    public EntityType createNewEntityType(String entityTypeName) throws ComputeException;
    public EntityAttribute createNewEntityAttr(String entityTypeName, String attrName) throws ComputeException;
    public void setupEntityTypes();
    
    public Entity getAncestorWithType(Entity entity, String type) throws ComputeException;
    public List<List<Long>> searchTreeForNameStartingWith(Long rootId, String searchString) throws ComputeException;
    public List<Long> getPathToRoot(Long entityId, Long rootId) throws ComputeException;
    public List<Entity> getEntitiesWithAttributeValue(String attrName, String attrValue) throws ComputeException;
    public void addChildren(String userLogin, Long parentId, List<Long> childrenIds, String attributeName) throws ComputeException;
    public List<MappedId> getProjectedResults(List<Long> entityIds, List<EntityMapStep> upMapping, List<EntityMapStep> downMapping) throws ComputeException;
    
    public Map<Entity, Map<String, Double>> getPatternAnnotationQuantifiers() throws ComputeException;
    public Object[] getPatternAnnotationQuantifierMapsFromSummary() throws ComputeException;
    
    public void loadLazyEntity(Entity entity, boolean recurse) throws DaoException;
}
