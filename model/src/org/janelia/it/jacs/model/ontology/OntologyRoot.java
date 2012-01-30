package org.janelia.it.jacs.model.ontology;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.Hibernate;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.ontology.types.EnumText;

/**
 * The root of an ontology tree. May be public or private.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OntologyRoot extends OntologyElement {

	private boolean isPublic;

	private Map<Long,OntologyElement> elementMap = new HashMap<Long,OntologyElement>();
	
	public OntologyRoot(Entity entity) {
		super(entity, null);
		
		// Derive additional properties
	  
        String publicStr = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_IS_PUBLIC);
    	this.isPublic = (publicStr != null && !"".equals(publicStr) && !"false".equals(publicStr));

        for (EntityData entityData : entity.getEntityData()) {
            if (!Hibernate.isInitialized(entityData.getChildEntity())) {
                return;
            }
        }
        
    	populateElementMap(this);
    	
    	// Populate secondary properties
    	
    	populateInternalReferences(this, true);
	}

	public void populateElementMap(OntologyElement element) {

		elementMap.put(element.getId(), element);
		
		if (hasChildren()) {
			for(OntologyElement child : element.getChildren()) {
				populateElementMap(child);
			}
		}	
	}

	public void populateInternalReferences(OntologyElement element, boolean recurse) {

		if (element.getType() instanceof EnumText) {
			EnumText enumText = (EnumText)element.getType();
			OntologyElement valueEnum = elementMap.get(enumText.getValueEnumId());
			if (valueEnum.getType() instanceof org.janelia.it.jacs.model.ontology.types.Enum) {
				enumText.init(valueEnum);	
			}
		}
		
		if (recurse && hasChildren()) {
			for(OntologyElement child : element.getChildren()) {
				populateInternalReferences(child, true);
			}
		}	
	}
	
	public boolean isPublic() {
		return isPublic;
	}

	public void setPublic(boolean isPublic) {
		this.isPublic = isPublic;
	}
}
