package org.janelia.it.jacs.model.domain.gui;

import java.util.List;

import org.janelia.it.jacs.model.domain.AbstractDomainObject;

public class AlignmentBoard extends AbstractDomainObject {

    private String imageSize;
    private String opticalResolution;
    private String alignmentSpace;
    private String encodedUserSettings;
	private List<AlignmentBoardItem> children;

    /* EVERYTHING BELOW IS AUTO-GENERATED */
    
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
	public String getEncodedUserSettings() {
		return encodedUserSettings;
	}
	public void setEncodedUserSettings(String encodedUserSettings) {
		this.encodedUserSettings = encodedUserSettings;
	}
	public List<AlignmentBoardItem> getChildren() {
		return children;
	}
	public void setChildren(List<AlignmentBoardItem> children) {
		this.children = children;
	}
}
