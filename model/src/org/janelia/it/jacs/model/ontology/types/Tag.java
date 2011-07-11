/*
 * Created by IntelliJ IDEA.
 * User: rokickik
 * Date: 6/22/11
 * Time: 6:09 PM
 */
package org.janelia.it.jacs.model.ontology.types;

import org.janelia.it.jacs.model.entity.Entity;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Tag extends OntologyElementType {

    public void init(Entity entity) {

    }
    
    public boolean allowsChildren() {
        return false;
    }

    public String getName() {
    	return "Tag";
    }
}
