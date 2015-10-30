package org.janelia.it.jacs.model.domain.sample;

import java.util.Map;

import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.interfaces.HasFilepath;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;

public class FileGroup implements HasFilepath, HasFiles {

    private String filepath;
	private Map<FileType, String> files;

    /* EVERYTHING BELOW IS AUTO-GENERATED */
    
    @Override
    public String getFilepath() {
        return filepath;
    }
    
    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    public Map<FileType, String> getFiles() {
        return files;
    }

    public void setFiles(Map<FileType, String> files) {
        this.files = files;
    }
}
