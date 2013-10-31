package org.janelia.it.jacs.model.ontology;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.ontology.types.OntologyElementType;

/**
 * An typed element in an ontology tree.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OntologyElement {

    protected Entity parent;
    protected Entity entity;
    protected OntologyElementType type;

	public OntologyElement(Entity parent, Entity entity) {
	    this.parent = parent;
	    this.entity = entity;
        String typeName = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE);
        if (typeName != null) {
            type = OntologyElementType.createTypeByName(typeName);
            if (type != null) type.init(entity);
        }
	}

    public Entity getEntity() {
        return entity;
    }

	public Long getId() {
		return entity.getId();
	}
	
    public String getName() {
		return entity.getName();
	}

	public OntologyElementType getType() {
		return type;
	}

    public List<OntologyElement> getChildren() {
        List<OntologyElement> children = new ArrayList<OntologyElement>();
        for(EntityData ed : entity.getList(EntityConstants.ATTRIBUTE_ONTOLOGY_ELEMENT)) {
            Entity child = ed.getChildEntity();
            if (child != null) {
                children.add(new OntologyElement(entity, child));
            }
        }
        return children;
    }

    public OntologyElement getParent() {
        if (parent==null) return null;
        return new OntologyElement(null, parent);
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
    
	@Override
	public String toString() {
		return getName();
	}
    
}
