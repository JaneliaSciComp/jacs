package org.janelia.it.jacs.model.entity.cv;

public enum PipelineProcess implements NamedEnum {

	FlyLightUnaligned("FlyLight Unaligned Pipeline"),
	FlyLightOpticLobe("FlyLight Optic Lobe Pipeline"),
	FlyLightCentralBrain("FlyLight Central Brain Pipeline"),
	FlyLightWholeBrain("FlyLight Whole Brain Pipeline"),
	FlyLightWholeBrain64x("FlyLight Whole Brain 63x Pipeline"),
	LeetWholeBrain40x("Lee Whole Brain 40x Pipeline"),
	LeetCentralBrain63x("Lee Central Brain 63x Pipeline");
	
	private String name;
	
	private PipelineProcess(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
}
