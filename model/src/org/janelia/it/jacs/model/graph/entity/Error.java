package org.janelia.it.jacs.model.graph.entity;

import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.graph.annotations.GraphNode;
import org.janelia.it.jacs.model.graph.annotations.RelatedTo;

@GraphNode(type=EntityConstants.TYPE_ERROR)
public class Error extends EntityNode {

    @RelatedTo(relationType=EntityConstants.ATTRIBUTE_MESSAGE)
    private String message;
    
    /* EVERYTHING BELOW IS AUTO GENERATED */

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
