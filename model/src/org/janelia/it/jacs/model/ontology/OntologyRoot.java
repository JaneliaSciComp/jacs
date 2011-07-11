package org.janelia.it.jacs.model.ontology;

import java.util.HashMap;
import java.util.Map;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * The root of an ontology tree. May be public or private.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OntologyRoot extends OntologyElement {

	private final Map<Long,OntologyElement> map = new HashMap<Long,OntologyElement>();
	
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

	public OntologyElement getElementById(Long id) {
		if (map.isEmpty()) {
			populateIdMap(this);
		}
		return map.get(id);
	}
	
	private void populateIdMap(OntologyElement currNode) {
		map.put(currNode.getId(), currNode);
		for(OntologyElement child : currNode.getChildren()) {
			populateIdMap(child);
		}
	}
}
