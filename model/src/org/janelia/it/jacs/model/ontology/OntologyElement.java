package org.janelia.it.jacs.model.ontology;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.ontology.types.OntologyElementType;

/**
 * An named element in an ontology tree. Has a single parent and zero or more children. Each element also has
 * an associated ontological type. 
 * 
 * This object is reconstituted from a set of persistent Entity/EntityData objects.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OntologyElement {

    private Entity entity;
    private OntologyElement parentElement;
    
    // Derived properties
    private String name;
    private OntologyElementType type;
    private List<OntologyElement> children = new ArrayList<OntologyElement>();
    
	public OntologyElement(Entity entity, OntologyElement parentElement) {
		this.entity = entity;
		this.parentElement = parentElement;
		
		// Derive additional properties which are mutable
		// TODO: implement saving changes to these properties
		
		this.name = entity.getName();

        String typeName = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE);
        if (typeName != null) {
            type = OntologyElementType.createTypeByName(typeName);
            if (type != null) type.init(entity);
        }
        
        for(EntityData ed : entity.getList(EntityConstants.ATTRIBUTE_ONTOLOGY_ELEMENT)) {
        	Entity child = ed.getChildEntity();
        	if (child != null) children.add(new OntologyElement(child, this));
        }
        
	}

	public Long getId() {
		return entity.getId();
	}
	
    public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Entity getEntity() {
		return entity;
	}

	public OntologyElement getParent() {
		return parentElement;
	}

	public OntologyElementType getType() {
		return type;
	}
	
	public List<OntologyElement> getChildren() {
		return children;
	}

	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OntologyElement)) return false;
        OntologyElement that = (OntologyElement) o;
        return entity.getId().equals(that.getEntity().getId());
    }

    @Override
    public int hashCode() {
        return entity.getId().hashCode();
    }
}
