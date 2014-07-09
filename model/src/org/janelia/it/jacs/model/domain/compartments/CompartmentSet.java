package org.janelia.it.jacs.model.domain.compartments;

import java.util.List;

import org.janelia.it.jacs.model.domain.AbstractDomainObject;
import org.janelia.it.jacs.model.domain.interfaces.HasFilepath;

public class CompartmentSet extends AbstractDomainObject implements HasFilepath {

    private String filepath;
    private String imageSize;
    private String opticalResolution;
    private String alignmentSpace;
    private List<Compartment> compartments;

    /* EVERYTHING BELOW IS AUTO-GENERATED */

    @Override
    public String getFilepath() {
        return filepath;
    }
    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }
	public String getImageSize() {
		return imageSize;
	}
	public void setImageSize(String imageSize) {
		this.imageSize = imageSize;
	}
	public String getOpticalResolution() {
		return opticalResolution;
	}
	public void setOpticalResolution(String opticalResolution) {
		this.opticalResolution = opticalResolution;
	}
	public String getAlignmentSpace() {
		return alignmentSpace;
	}
	public void setAlignmentSpace(String alignmentSpace) {
		this.alignmentSpace = alignmentSpace;
	}
	public List<Compartment> getCompartments() {
		return compartments;
	}
	public void setCompartments(List<Compartment> compartments) {
		this.compartments = compartments;
	}
	
}
