package org.janelia.it.jacs.compute.service.align;

import org.janelia.it.jacs.model.entity.cv.AlignmentAlgorithm;

public class ParameterizedAlignmentAlgorithm {

	private AlignmentAlgorithm algorithm;
	private String parameter;
	private String resultName;
	
	public ParameterizedAlignmentAlgorithm(AlignmentAlgorithm algorithm,
			String parameter, String resultName) {
		this.algorithm = algorithm;
		this.parameter = parameter;
		this.resultName = resultName;
	}

	public AlignmentAlgorithm getAlgorithm() {
		return algorithm;
	}

	public String getParameter() {
		return parameter;
	}
	
	public String getResultName() {
	    return resultName;
	}
}
