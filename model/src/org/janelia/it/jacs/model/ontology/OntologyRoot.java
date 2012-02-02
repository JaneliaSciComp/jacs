package org.janelia.it.jacs.model.ontology;

import java.util.HashMap;
import java.util.Map;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.ontology.types.EnumText;

/**
 * The root of an ontology tree. May be public or private.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OntologyRoot extends OntologyElement {

	private final boolean isPublic;
	private final Map<Long,OntologyElement> elementMap;
	
	public OntologyRoot(Entity entity) {
		super(entity);
        String publicStr = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_IS_PUBLIC);
    	this.isPublic = (publicStr != null && !"".equals(publicStr) && !"false".equals(publicStr));
    	this.elementMap = new HashMap<Long,OntologyElement>();
        
        init();
	}

	public void populateElementMap(OntologyElement element, boolean recurse) {

		elementMap.put(element.getId(), element);
		System.out.println("OntologyRoot.populateElementMap "+element.getId()+"="+element.getName());
		
		if (recurse && hasChildren()) {
			for(OntologyElement child : element.getChildren()) {
				populateElementMap(child, recurse);
			}
		}	
	}

	public void populateInternalReferences(OntologyElement element, boolean recurse) {

		System.out.println("populateInternalReferences "+element.getName());
		
		if (element.getType() instanceof EnumText) {
			EnumText enumText = (EnumText)element.getType();
			OntologyElement valueEnum = elementMap.get(enumText.getValueEnumId());
			if (valueEnum==null) {
				System.out.println("Warning: could not find reference enum, id="+enumText.getValueEnumId());
			}
			else { 
				if (valueEnum.getType() instanceof org.janelia.it.jacs.model.ontology.types.Enum) {
					enumText.init(valueEnum);
				}
				else {
					System.out.println("Warning: referenced enum is not an enum but "+valueEnum.getType().getName());	
				}
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
}
