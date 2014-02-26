package org.janelia.it.jacs.model.graph.entity;

import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.graph.annotations.GraphAttribute;
import org.janelia.it.jacs.model.graph.annotations.GraphNode;

@GraphNode(type=EntityConstants.TYPE_IMAGE_2D)
public class Image2d extends EntityNode {

	private static final long serialVersionUID = 1L;
	
    @GraphAttribute(EntityConstants.ATTRIBUTE_ARTIFACT_SOURCE_ID)
    private Long artifactSourceEntityId;
    
    @GraphAttribute(EntityConstants.ATTRIBUTE_FILE_PATH)
    private String filepath;

    @GraphAttribute(EntityConstants.ATTRIBUTE_IMAGE_FORMAT)
    private String imageFormat;

    /* EVERYTHING BELOW IS AUTO GENERATED */

    public Long getArtifactSourceEntityId() {
        return artifactSourceEntityId;
    }

    public void setArtifactSourceEntityId(Long artifactSourceEntityId) {
        this.artifactSourceEntityId = artifactSourceEntityId;
    }

    public String getFilepath() {
        return filepath;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    public String getImageFormat() {
        return imageFormat;
    }

    public void setImageFormat(String imageFormat) {
        this.imageFormat = imageFormat;
    }
}
