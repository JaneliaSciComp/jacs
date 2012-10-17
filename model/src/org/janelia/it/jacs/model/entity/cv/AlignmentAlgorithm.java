package org.janelia.it.jacs.model.entity.cv;

public enum AlignmentAlgorithm implements NamedEnum {

	CENTRAL("Peng Central Brain 40x Alignment"),
	OPTIC("FlyLight Optic Lobe 40x Alignment"),
	WHOLE_40X("FlyLight Whole Brain 40x Alignment"),
	WHOLE_63X("FlyLight Whole Brain 63x Alignment"),
	YOSHI_63X_FLPOUT("Yoshi MB Flp-out 63x Alignment"),
	YOSHI_63X_LEXAGAL4("Yoshi MB LexA-GAL4 63x Alignment");
	
	private String name;
	
	private AlignmentAlgorithm(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
}
