package org.janelia.it.jacs.model.ontology;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * The root of an ontology tree. May be public or private.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OntologyRoot extends OntologyElement {

	private boolean isPublic;

	public OntologyRoot(Entity entity) {
		super(entity, null);
		
		// Derive additional properties
	  
        String publicStr = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_IS_PUBLIC);
    	this.isPublic = (publicStr != null && !"".equals(publicStr) && !"false".equals(publicStr));
	}

	public boolean isPublic() {
		return isPublic;
	}

	public void setPublic(boolean isPublic) {
		this.isPublic = isPublic;
	}
}
