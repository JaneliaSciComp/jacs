package org.janelia.it.jacs.compute.api;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Remote;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.jacs.model.ontology.types.OntologyElementType;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.shared.annotation.DataDescriptor;
import org.janelia.it.jacs.shared.annotation.DataFilter;
import org.janelia.it.jacs.shared.annotation.FilterResult;
import org.janelia.it.jacs.shared.annotation.PatternAnnotationDataManager;

/**
 * A remote interface to queries having to do with specific types in the entity model, mainly
 * ontologies and annotations. For generic queries against the entity model, see EntityBeanRemote.
 * 
 * @see EntityBeanRemote
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@Remote
public interface AnnotationBeanRemote {

    public Entity createOntologyRoot(String userLogin, String rootName) throws ComputeException;
    public EntityData createOntologyTerm(String userLogin, Long ontologyTermParentId, String termName, OntologyElementType type, Integer orderIndex) throws ComputeException;
    public void removeOntologyTerm(String userLogin, Long ontologyTermId) throws ComputeException;
    public Entity cloneEntityTree(String userLogin, Long sourceRootId, String targetRootName) throws ComputeException;
    public Entity publishOntology(String userLogin, Long sourceRootId, String targetRootName) throws ComputeException;

	public Entity createOntologyAnnotation(String userLogin, OntologyAnnotation annotation) throws ComputeException;
	public void removeOntologyAnnotation(String userLogin, long annotationId) throws ComputeException;
	public void removeAllOntologyAnnotationsForSession(String userLogin, long sessionId) throws ComputeException;

    public Entity getOntologyTree(String userLogin, Long id) throws ComputeException;
    public List<Entity> getPublicOntologies() throws ComputeException;
    public Entity getErrorOntology() throws ComputeException;
    public List<Entity> getPrivateOntologies(String userLogin) throws ComputeException;
    
	public List<Task> getAnnotationSessionTasks(String username) throws ComputeException;
    public List<Entity> getAnnotationsForEntities(String username, List<Long> entityIds) throws ComputeException;
    public List<Entity> getAnnotationsForChildren(String username, long entityId) throws ComputeException;
    public List<Entity> getAnnotationsForEntity(String username, long entityId) throws ComputeException;
    public List<Entity> getAnnotationsForSession(String username, long sessionId) throws ComputeException;
    public List<Entity> getEntitiesForAnnotationSession(String username, long sessionId) throws ComputeException;
    public List<Entity> getCategoriesForAnnotationSession(String username, long sessionId) throws ComputeException;
    public Set<Long> getCompletedEntityIds(long sessionId) throws ComputeException;

    public List<Entity> getCommonRootEntitiesByTypeName(String entityTypeName);
    public List<Entity> getCommonRootEntitiesByTypeName(String userLogin, String entityTypeName);
    public List<Entity> getEntitiesWithFilePath(String filePath);
    
    public Map<Entity, Map<String, Double>> getPatternAnnotationQuantifiers() throws ComputeException;
    public Object[] getPatternAnnotationQuantifierMapsFromSummary() throws ComputeException;
    public Map<Entity, Map<String, Double>> getMaskQuantifiers(String maskFolderName) throws ComputeException;
    public Object[] getMaskQuantifierMapsFromSummary(String maskFolderName) throws ComputeException;

    public List<DataDescriptor> patternSearchGetDataDescriptors(String type) throws ComputeException;
    public int patternSearchGetState() throws ComputeException;
    public List<String> patternSearchGetCompartmentList(String type) throws ComputeException;
    public FilterResult patternSearchGetFilteredResults(String type, Map<String, Set<DataFilter>> filterMap) throws ComputeException;

}
