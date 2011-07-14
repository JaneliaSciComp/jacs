package org.janelia.it.jacs.compute.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.ejb.Remote;

import org.janelia.it.jacs.model.annotation.Annotation;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.EntityType;
import org.janelia.it.jacs.model.ontology.types.OntologyElementType;

@Remote
public interface AnnotationBeanRemote {

    public Entity saveOrUpdateEntity(Entity entity);
    public EntityType getEntityTypeByName(String name);
    public List<Entity> getUserEntitiesByType(String userLogin, long entityTypeId);
    public Set<Entity> getEntitiesByName(String name);
    public Entity getEntityById(String targetId);
    public Entity getEntityTree(Long id);
    public Entity getUserEntityById(String userLogin, long entityId);
    public boolean deleteEntityById(Long entityId);
    public Set<Entity> getParentEntities(long entityId);
    public Set<EntityData> getParentEntityDatas(long entityId);
    
    public List<EntityType> getEntityTypes();
    public List<Entity> getEntitiesByType(long entityTypeId);
    public List<Entity> getCommonRootEntitiesByType(long entityTypeId);

    public String addAnnotation(String owner, String namespace, String term, String value, String comment, String conditional);
    public void deleteAnnotation(String owner, String uniqueIdentifier);
    public ArrayList<Annotation> getAnnotationsForUser(String owner);
    public void editAnnotation(String owner, String uniqueIdentifier, String namespace, String term, String value,
                               String comment, String conditional);
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

	public Entity createOntologyAnnotation(String userLogin, String sessionId, String targetEntityId,
			String keyEntityId, String keyString, String valueEntityId, String valueString, String tag)
			throws ComputeException;

    public void setupEntityTypes();
}
