/*
 * Created by IntelliJ IDEA.
 * User: rokickik
 * Date: 6/27/11
 * Time: 10:03 AM
 */
package org.janelia.it.jacs.model.ontology.types;

import org.janelia.it.jacs.model.entity.Entity;

import java.io.Serializable;

/**
 * The type of an ontology element. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class OntologyElementType implements Serializable {
    
    private static String ONTOLOGY_TERM_TYPES_PACKAGE = OntologyElementType.class.getPackage().getName();

    public static OntologyElementType createTypeByName(String className) {

        try {
            return (OntologyElementType)Class.forName(ONTOLOGY_TERM_TYPES_PACKAGE+"."+className).newInstance();
        }
        catch (Exception ex) {
            System.err.println("Could not instantiate term type: "+className);
            ex.printStackTrace();
        }
        return null;
    }

    public abstract void init(Entity entity);

    public abstract boolean allowsChildren();
    
    public abstract String getName();
}
