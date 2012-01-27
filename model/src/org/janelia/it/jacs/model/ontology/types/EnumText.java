/*
 * Created by IntelliJ IDEA.
 * User: rokickik
 * Date: 6/22/11
 * Time: 6:10 PM
 */
package org.janelia.it.jacs.model.ontology.types;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.ontology.OntologyElement;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EnumText extends OntologyElementType {

	private Long valueEnumId;
	transient private OntologyElement valueEnum;
	
	public void init(OntologyElement valueEnum) {
		this.valueEnum = valueEnum;
		this.valueEnumId = valueEnum.getId();
	}
	
    public void init(Entity entity) {
    	String enumIdStr = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE_ENUMTEXT_ENUMID);
    	if (enumIdStr==null) throw new IllegalStateException("EnumText entity has null enum id: "+entity.getId());
    	this.valueEnumId = new Long(enumIdStr);
    }
    
    public boolean allowsChildren() {
        return false;
    }

    public String getName() {
    	return "Enumerated Text";
    }

	public Long getValueEnumId() {
		return valueEnumId;
	}
    
	public OntologyElement getValueEnum() {
		return valueEnum;
	}
}
