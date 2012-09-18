package org.janelia.it.jacs.model.entity.cv;

public enum MergeAlgorithm implements NamedEnum {

	FLYLIGHT("FlyLight LSM Pair Merge (3/2 to 4 channels)");
	
	private String name;
	
	private MergeAlgorithm(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
