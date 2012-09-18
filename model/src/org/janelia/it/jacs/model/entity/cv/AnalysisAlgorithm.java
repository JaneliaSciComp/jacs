package org.janelia.it.jacs.model.entity.cv;

public enum AnalysisAlgorithm implements NamedEnum {

	NEURON_SEPARATOR("Gene Myers Neuron Separator");
	
	private String name;
	
	private AnalysisAlgorithm(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
