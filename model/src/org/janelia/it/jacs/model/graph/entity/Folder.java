package org.janelia.it.jacs.model.graph.entity;

import java.util.List;

import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.graph.annotations.GraphAttribute;
import org.janelia.it.jacs.model.graph.annotations.GraphNode;
import org.janelia.it.jacs.model.graph.annotations.RelatedTo;

@GraphNode(type=EntityConstants.TYPE_FOLDER)
public class Folder extends EntityNode {

	private static final long serialVersionUID = 1L;
	
    @GraphAttribute(EntityConstants.ATTRIBUTE_COMMON_ROOT)
    private Boolean isCommonRoot;
    
    @RelatedTo(relationType=EntityConstants.ATTRIBUTE_ENTITY)
    private List<EntityNode> children;

    @GraphAttribute(EntityConstants.ATTRIBUTE_IS_PROTECTED)
    private Boolean isProtected;
    
    @GraphAttribute(EntityConstants.ATTRIBUTE_SEARCH_TASK_ID)
    private Long searchTaskId;

    public boolean isCommonRoot() {
        return isCommonRoot!=null && isCommonRoot;
    }
    
    public boolean isProtected() {
        return isProtected!=null && isProtected;
    }

    /* EVERYTHING BELOW IS AUTO GENERATED */
    
    public Boolean getIsCommonRoot() {
        return isCommonRoot;
    }

    public void setIsCommonRoot(Boolean isCommonRoot) {
        this.isCommonRoot = isCommonRoot;
    }

    public List<EntityNode> getChildren() {
        return children;
    }

    public void setChildren(List<EntityNode> children) {
        this.children = children;
    }

    public Boolean getIsProtected() {
        return isProtected;
    }

    public void setIsProtected(Boolean isProtected) {
        this.isProtected = isProtected;
    }

    public Long getSearchTaskId() {
        return searchTaskId;
    }

    public void setSearchTaskId(Long searchTaskId) {
        this.searchTaskId = searchTaskId;
    }
}
