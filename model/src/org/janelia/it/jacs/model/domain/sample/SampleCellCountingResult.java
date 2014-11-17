package org.janelia.it.jacs.model.domain.sample;

import java.util.Map;

import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;

public class SampleCellCountingResult extends PipelineResult implements HasFiles {

    private String name;
    private Integer cellCount;
    private Map<FileType, String> files;

    /* EVERYTHING BELOW IS AUTO-GENERATED */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getCellCount() {
        return cellCount;
    }

    public void setCellCount(Integer cellCount) {
        this.cellCount = cellCount;
    }

    public Map<FileType, String> getFiles() {
        return files;
    }

    public void setFiles(Map<FileType, String> files) {
        this.files = files;
    }
}
