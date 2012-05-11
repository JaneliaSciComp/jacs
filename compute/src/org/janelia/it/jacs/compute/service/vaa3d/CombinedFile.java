package org.janelia.it.jacs.compute.service.vaa3d;

import java.io.Serializable;

/**
 * Two files and the result of combining them in some way.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class CombinedFile implements Serializable {

	private String filepath1;
	private String filepath2;
	private String outputFilepath;
	
	public CombinedFile(String filepath1, String filepath2, String outputFilepath) {
		this.filepath1 = filepath1;
		this.filepath2 = filepath2;
		this.outputFilepath = outputFilepath;
	}

	public String getFilepath1() {
		return filepath1;
	}

	public String getFilepath2() {
		return filepath2;
	}

	public String getOutputFilepath() {
		return outputFilepath;
	}

}
