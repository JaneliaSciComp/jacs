package org.janelia.it.jacs.model.entity.cv;

public enum StitchAlgorithm implements NamedEnum {

	FLYLIGHT("FlyLight Tile Stitching");
	
	private String name;
	
	private StitchAlgorithm(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
}
