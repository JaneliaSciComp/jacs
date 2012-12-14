package org.janelia.it.jacs.compute.service.align;

import org.janelia.it.jacs.model.entity.cv.AlignmentAlgorithm;

public class ParameterizedAlignmentAlgorithm {

	private AlignmentAlgorithm algorithm;
	private String parameter;
	
	public ParameterizedAlignmentAlgorithm(AlignmentAlgorithm algorithm,
			String parameter) {
		this.algorithm = algorithm;
		this.parameter = parameter;
	}

	public AlignmentAlgorithm getAlgorithm() {
		return algorithm;
	}

	public String getParameter() {
		return parameter;
	}
}
