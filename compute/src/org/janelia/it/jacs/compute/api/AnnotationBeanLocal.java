package org.janelia.it.jacs.compute.api;

import org.janelia.it.jacs.model.annotation.Annotation;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityAttribute;
import org.janelia.it.jacs.model.entity.EntityType;

import javax.ejb.Local;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Local
public interface AnnotationBeanLocal {

    public Entity saveOrUpdateEntity(Entity entity);
    public EntityAttribute getEntityAttributeByName(String name);
    public EntityType getEntityTypeByName(String name);
    public Set<EntityAttribute> getEntityAttributesByEntityType(EntityType entityType);

    public String addAnnotation(String owner, String namespace, String term, String value, String comment, String conditional);
    public void deleteAnnotation(String owner, String uniqueIdentifier);
    public ArrayList<Annotation> getAnnotationsForUser(String owner);
    public void editAnnotation(String owner, String uniqueIdentifier, String namespace, String term, String value,
                               String comment, String conditional);
    public List<Entity> getEntitiesWithFilePath(String filePath);

    public void createOntologyTerm(String userLogin, String ontologyTermParentId, String termName);
    public void createOntologyRoot(String userLogin, String rootName);
    public boolean removeOntologyTerm(String userLogin, String ontologyTermId);

    public void setupEntityTypes();

}
