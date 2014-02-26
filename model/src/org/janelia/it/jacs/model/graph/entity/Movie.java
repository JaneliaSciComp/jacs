package org.janelia.it.jacs.model.graph.entity;

import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.graph.annotations.GraphAttribute;
import org.janelia.it.jacs.model.graph.annotations.GraphNode;

@GraphNode(type=EntityConstants.TYPE_MOVIE)
public class Movie {
    
	private static final long serialVersionUID = 1L;
	
    @GraphAttribute(EntityConstants.ATTRIBUTE_FILE_PATH)
    private String filepath;

    /* EVERYTHING BELOW IS AUTO GENERATED */

    public String getFilepath() {
        return filepath;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }
}
