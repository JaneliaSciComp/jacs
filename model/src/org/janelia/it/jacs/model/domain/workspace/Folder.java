package org.janelia.it.jacs.model.domain.workspace;

import org.janelia.it.jacs.model.domain.interfaces.HasFilepath;

public class Folder extends TreeNode implements HasFilepath {

    private String filepath;

    @Override
    public String getFilepath() {
        return filepath;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

}
