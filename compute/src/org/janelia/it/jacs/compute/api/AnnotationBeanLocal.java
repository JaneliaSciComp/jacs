package org.janelia.it.jacs.compute.api;

import org.janelia.it.jacs.model.annotation.Annotation;
import org.janelia.it.jacs.model.entity.Entity;

import javax.ejb.Local;
import java.util.ArrayList;
import java.util.List;

@Local
public interface AnnotationBeanLocal {

    public String addAnnotation(String owner, String namespace, String term, String value, String comment, String conditional);
    public void deleteAnnotation(String owner, String uniqueIdentifier);
    public ArrayList<Annotation> getAnnotationsForUser(String owner);
    public void editAnnotation(String owner, String uniqueIdentifier, String namespace, String term, String value,
                               String comment, String conditional);
    public List<Entity> getEntitiesWithFilePath(String filePath);


}
