/*
 * Created by IntelliJ IDEA.
 * User: rokickik
 * Date: 6/27/11
 * Time: 10:03 AM
 */
package org.janelia.it.jacs.model.ontology;

import org.janelia.it.jacs.model.entity.Entity;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class OntologyTermType implements java.io.Serializable {
    
    private static String ONTOLOGY_TERM_TYPES_PACKAGE = OntologyTermType.class.getPackage().getName();

    public static OntologyTermType createTypeByName(String className) {

        try {
            return (OntologyTermType)Class.forName(ONTOLOGY_TERM_TYPES_PACKAGE+"."+className).newInstance();
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
