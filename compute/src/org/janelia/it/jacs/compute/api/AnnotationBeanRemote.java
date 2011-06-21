package org.janelia.it.jacs.compute.api;

import org.janelia.it.jacs.model.annotation.Annotation;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityType;

import javax.ejb.Remote;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Remote
public interface AnnotationBeanRemote {

    public Entity saveOrUpdateEntity(Entity entity);
    public EntityType getEntityTypeByName(String name);
    public java.util.List<Entity> getUserEntitiesByType(String userLogin, long entityTypeId);
    public Set<Entity> getEntitiesByName(String name);
    public Entity getUserEntityById(String userLogin, long entityId);
    public boolean deleteEntityById(Long entityId);
    public Set<Entity> getParentEntities(long entityId);

    public String addAnnotation(String owner, String namespace, String term, String value, String comment, String conditional);
    public void deleteAnnotation(String owner, String uniqueIdentifier);
    public ArrayList<Annotation> getAnnotationsForUser(String owner);
    public void editAnnotation(String owner, String uniqueIdentifier, String namespace, String term, String value,
                               String comment, String conditional);
    public List<Entity> getEntitiesWithFilePath(String filePath);

    public void createOntologyTerm(String userLogin, String ontologyTermParentId, String termName);
    public Entity createOntologyRoot(String userLogin, String rootName);
    public boolean removeOntologyTerm(String userLogin, String ontologyTermId);

    public void setupEntityTypes();
}
