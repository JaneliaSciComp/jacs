package org.janelia.it.jacs.model.domain.workspace;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.model.domain.AbstractDomainObject;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.interfaces.IsParent;
import org.janelia.it.jacs.model.domain.support.MongoMapped;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A generic node in a domain object tree. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@MongoMapped(collectionName="treeNode",label="Folder")
public class TreeNode extends AbstractDomainObject implements IsParent {

    private List<Reference> children = new ArrayList<>();

    /**
     * Return true if the given tree node has the specified domain object as a child. 
     * @param treeNode
     * @param domainObject
     * @return
     */
    public boolean hasChild(DomainObject domainObject) {
        for(Reference ref : getChildren()) {
            if (ref.getTargetId().equals(domainObject.getId())) {
                return true;
            }
        }
        return false;
    }
    
    @JsonIgnore
    public boolean hasChildren() {
    	return !children.isEmpty();
    }

    @JsonIgnore
    public int getNumChildren() {
        return children.size();
    }

    public void addChild(Reference ref) {
        children.add(ref);
    }

    public void insertChild(int index, Reference ref) {
        children.add(index, ref);
    }

    public void removeChild(Reference ref) {
        children.remove(ref);
    }

    public List<Reference> getChildren() {
        return children;
    }

    public void setChildren(List<Reference> children) {
        if (children==null) throw new IllegalArgumentException("Property cannot be null");
        this.children = children;
    }
}
