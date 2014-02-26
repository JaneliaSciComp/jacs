package org.janelia.it.jacs.model.graph.entity;

import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.graph.annotations.GraphAttribute;
import org.janelia.it.jacs.model.graph.annotations.RelatedTo;

public abstract class Result extends Renderable {

	private static final long serialVersionUID = 1L;
	
    @GraphAttribute(EntityConstants.ATTRIBUTE_FILE_PATH)
    private String filepath;
    
    @RelatedTo(relationType=EntityConstants.ATTRIBUTE_SUPPORTING_FILES)
    private SupportingData supportingFiles;

    /* EVERYTHING BELOW IS AUTO GENERATED */
    
    public String getFilepath() {
        return filepath;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    public SupportingData getSupportingFiles() {
        return supportingFiles;
    }

    public void setSupportingFiles(SupportingData supportingFiles) {
        this.supportingFiles = supportingFiles;
    }
}
