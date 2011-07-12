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
    private List<OntologyElement> children;
    
	public OntologyElement(Entity entity, OntologyElement parentElement) {
		this.entity = entity;
		this.parentElement = parentElement;
		init();
	}
	
	/**
	 * This should be called whenever the underlying entity changes, so that the derived properties can be recomputed.
	 */
	public void init() {

		this.name = entity.getName();

        String typeName = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE);
        if (typeName != null) {
            type = OntologyElementType.createTypeByName(typeName);
            if (type != null) type.init(entity);
        }
        
        // Only reinitialize children if they've already been initialized already
        if (children != null) {
        	initChildren();
        }
	}

	public void initChildren() {
    	children = new ArrayList<OntologyElement>();
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
	
	/**
	 * This method allows for testing to see if there are children, before loading them.
	 * @return
	 */
	public boolean hasChildren() {
        for(EntityData ed : entity.getList(EntityConstants.ATTRIBUTE_ONTOLOGY_ELEMENT)) {
        	Entity child = ed.getChildEntity();
        	if (child != null) return true;
        }
        return false;
	}
	
	/**
	 * Returns the child elements. Before calling this method, the underlying children entities must have been loaded.
	 * @return
	 */
	public List<OntologyElement> getChildren() {
        if (children == null) {
        	initChildren();
        }
		return children;
	}

	/**
	 * Remove the given child from both the underlying Entity and the derived OntologyElement model.
	 * @param child
	 */
	public void removeChild(OntologyElement child) {
		List<OntologyElement> children = getChildren();
		children.remove(child);
		
		EntityData toDelete = null;
		for(EntityData ed : entity.getEntityData()) {
			if (ed.getChildEntity() != null && ed.getChildEntity().equals(child.getEntity())) {
				toDelete = ed;
			}
		}
		if (toDelete == null) {
			System.out.println("Could not find EntityData to delete for "+child.getName());
		}
		else {
			entity.getEntityData().remove(toDelete);
		}
		
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
