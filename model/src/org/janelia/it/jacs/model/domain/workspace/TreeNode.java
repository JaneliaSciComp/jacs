package org.janelia.it.jacs.model.domain.workspace;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.model.domain.AbstractDomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.support.MongoMapped;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;

@MongoMapped(collectionName = "treeNode")
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "class")
public class TreeNode extends AbstractDomainObject {

    private List<Reference> children;

    public boolean hasChildren() {
    	return children!=null && !children.isEmpty();
    }

    @JsonIgnore
    public int getNumChildren() {
        return children==null ? 0 : children.size();
    }

    public void addChild(Reference ref) {
        if (children==null) {
            this.children = new ArrayList<Reference>();
        }
        children.add(ref);
    }

    public void insertChild(int index, Reference ref) {
        if (children==null) {
            this.children = new ArrayList<Reference>();
        }
        children.add(index, ref);
    }

    public void removeChild(Reference ref) {
        if (children==null) {
            return;
        }
        children.remove(ref);
    }

    /* EVERYTHING BELOW IS AUTO-GENERATED */
    public List<Reference> getChildren() {
        return children;
    }

    public void setChildren(List<Reference> children) {
        this.children = children;
    }
}
