package org.janelia.it.jacs.compute.api;

import java.util.Date;
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

/**
 * A remote interface to queries having to do with specific types in the entity model, mainly
 * ontologies and annotations. For generic queries against the entity model, see EntityBeanRemote.
 * 
 * @see EntityBeanRemote
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@Remote
public interface AnnotationBeanRemote {

    public Entity createOntologyRoot(String subjectKey, String rootName) throws ComputeException;
    public EntityData createOntologyTerm(String subjectKey, Long ontologyTermParentId, String termName, OntologyElementType type, Integer orderIndex) throws ComputeException;
    
	public Entity createOntologyAnnotation(String subjectKey, OntologyAnnotation annotation) throws ComputeException;
	public void removeOntologyAnnotation(String subjectKey, long annotationId) throws ComputeException;
	public void removeAllOntologyAnnotationsForSession(String subjectKey, long sessionId) throws ComputeException;

	public List<Entity> getOntologyRootEntities(String subjectKey) throws ComputeException;
    public Entity getOntologyTree(String subjectKey, Long id) throws ComputeException;
    public Entity getErrorOntology() throws ComputeException;
    
	public List<Task> getAnnotationSessionTasks(String subjectKey) throws ComputeException;
    public List<Entity> getAnnotationsForEntities(String subjectKey, List<Long> entityIds) throws ComputeException;
    public List<Entity> getAnnotationsForChildren(String subjectKey, long entityId) throws ComputeException;
    public List<Entity> getAnnotationsForEntity(String subjectKey, long entityId) throws ComputeException;
    public List<Entity> getAnnotationsForSession(String subjectKey, long sessionId) throws ComputeException;
    public List<Entity> getEntitiesAnnotatedWithTerm(String subjectKey, String annotationName) throws ComputeException;
    public List<Entity> getEntitiesForAnnotationSession(String subjectKey, long sessionId) throws ComputeException;
    public List<Entity> getCategoriesForAnnotationSession(String subjectKey, long sessionId) throws ComputeException;
    public Set<Long> getCompletedEntityIds(long sessionId) throws ComputeException;
    public long getNumDescendantsAnnotated(Long entityId) throws ComputeException;
    public List<Long> getEntityIdsInAlignmentSpace(String opticalRes, String pixelRes, List<Long> guids) throws ComputeException;
    public List<Long> getAllEntityIdsByType(String entityTypeName) throws ComputeException;


    /**
     * @deprecated use getWorkspaces to get the real roots
     */
    public List<Entity> getCommonRootEntities(String subjectKey) throws ComputeException;
	
    
    public List<Entity> getAlignmentSpaces(String subjectKey) throws ComputeException;
    public Entity getChildFolderByName(String subjectKey, Long parentId, String folderName, boolean createIfNecessary) throws ComputeException;
    public List<Entity> getEntitiesWithFilePath(String filePath);
    
    public Map<Entity, Map<String, Double>> getPatternAnnotationQuantifiers() throws ComputeException;
    public Object[] getPatternAnnotationQuantifierMapsFromSummary() throws ComputeException;
    public Map<Entity, Map<String, Double>> getMaskQuantifiers(String maskFolderName) throws ComputeException;
    public Object[] getMaskQuantifierMapsFromSummary(String maskFolderName) throws ComputeException;

    public List<DataDescriptor> patternSearchGetDataDescriptors(String type) throws ComputeException;
    public int patternSearchGetState() throws ComputeException;
    public List<String> patternSearchGetCompartmentList(String type) throws ComputeException;
    public FilterResult patternSearchGetFilteredResults(String type, Map<String, Set<DataFilter>> filterMap) throws ComputeException;

    public Entity createDataSet(String subjectKey, String dataSetName) throws ComputeException;
    public List<Entity> getAllDataSets() throws ComputeException;
    public List<Entity> getDataSets(String subjectKey) throws ComputeException;
    public List<Entity> getUserDataSets(List<String> subjectKeyList) throws ComputeException;
    public Entity getUserDataSetByName(String subjectKey, String dataSetName) throws ComputeException;
    public Entity getUserDataSetByIdentifier(String dataSetIdentifier) throws ComputeException;
    
    public Entity createFlyLineRelease(String subjectKey, String releaseName, Date releaseDate, List<String> dataSetList) throws ComputeException;
    public List<Entity> getUserFlyLineReleases(List<String> subjectKeyList) throws ComputeException;
    
    public Entity createAlignmentBoard(String subjectKey, String alignmentBoardName, String alignmentSpace, String opticalRes, String pixelRes) throws ComputeException;
    public EntityData addAlignedItem(Entity parentEntity, Entity child, String alignedItemName, boolean visible) throws ComputeException;

    public void addGroupWorkspaceToUserWorkspace(String userKey, String groupKey) throws ComputeException;
    public void removeGroupWorkspaceFromUserWorkspace(String userKey, String groupKey) throws ComputeException;
    public void createWorkspace(String ownerKey) throws ComputeException;
    public void reorderWorkspace(String ownerKey) throws ComputeException;
}
