package org.janelia.it.jacs.compute.api;

import org.janelia.it.jacs.model.annotation.Annotation;

import javax.ejb.Local;
import java.util.ArrayList;

@Local
public interface AnnotationBeanLocal {

    public String addAnnotation(String owner, String namespace, String term, String value, String comment, String conditional);
    public void deleteAnnotation(String owner, String uniqueIdentifier);
    public ArrayList<Annotation> getAnnotationsForUser(String owner);
    public void editAnnotation(String owner, String uniqueIdentifier, String namespace, String term, String value,
                               String comment, String conditional);

}
