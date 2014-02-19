package org.janelia.it.jacs.model.graph.entity;

import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.graph.annotations.GraphNode;
import org.janelia.it.jacs.model.graph.annotations.RelatedTo;

@GraphNode(type=EntityConstants.TYPE_FILE)
public class File extends EntityNode {

    @RelatedTo(relationType=EntityConstants.ATTRIBUTE_FILE_PATH)
    private String filepath;
    
    /* EVERYTHING BELOW IS AUTO GENERATED */
    
    public String getFilepath() {
        return filepath;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }
}
