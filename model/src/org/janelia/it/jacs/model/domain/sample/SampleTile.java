package org.janelia.it.jacs.model.domain.sample;

import java.util.List;
import java.util.Map;

import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.jacs.model.domain.support.SearchTraversal;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A sample tile consists of a set of LSMs with the same objective, 
 * and in the same anatomical area. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleTile implements HasFiles {

    private String name;
    private String anatomicalArea;
    @SearchTraversal({Sample.class})
    private List<Reference> lsmReferences;
    private Map<FileType, String> files;
    private ObjectiveSample parent;

    @JsonIgnore
    public ObjectiveSample getParent() {
        return parent;
    }

    @JsonIgnore
    void setParent(ObjectiveSample parent) {
        this.parent = parent;
    }
    
    /* EVERYTHING BELOW IS AUTO-GENERATED */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAnatomicalArea() {
        return anatomicalArea;
    }

    public void setAnatomicalArea(String anatomicalArea) {
        this.anatomicalArea = anatomicalArea;
    }

    public List<Reference> getLsmReferences() {
        return lsmReferences;
    }

    public void setLsmReferences(List<Reference> lsmReferences) {
        this.lsmReferences = lsmReferences;
    }

    public Map<FileType, String> getFiles() {
        return files;
    }

    public void setFiles(Map<FileType, String> files) {
        this.files = files;
    }

}
