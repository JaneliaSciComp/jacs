package org.janelia.it.jacs.model.domain.sample;

import java.util.Map;

import org.janelia.it.jacs.model.domain.AbstractDomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.interfaces.HasFilepath;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.jacs.model.domain.support.MongoMapped;
import org.janelia.it.jacs.model.domain.support.SearchAttribute;
import org.janelia.it.jacs.model.domain.support.SearchType;

@MongoMapped(collectionName = "fragment")
@SearchType(key="fragment",label="Neuron Fragment")
public class NeuronFragment extends AbstractDomainObject implements HasFiles, HasFilepath {

    private Reference sample;
    private Long separationId;
        
    @SearchAttribute(key="number_i",label="Number")
    private Integer number;
    
    @SearchAttribute(key="filepath_txt",label="File Path")
    private String filepath;
    
    private Map<FileType, String> files;

    /* EVERYTHING BELOW IS AUTO-GENERATED */
    
    public Integer getNumber() {
        return number;
    }

    public Reference getSample() {
		return sample;
	}

	public void setSample(Reference sample) {
		this.sample = sample;
	}

	public Long getSeparationId() {
        return separationId;
    }

    public void setSeparationId(Long separationId) {
        this.separationId = separationId;
    }
    
    public void setNumber(Integer number) {
        this.number = number;
    }

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
