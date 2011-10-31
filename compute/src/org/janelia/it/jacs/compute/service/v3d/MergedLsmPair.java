package org.janelia.it.jacs.compute.service.v3d;

import java.io.Serializable;

/**
 * A pair of LSMs which are merged into a single V3DRAW file. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MergedLsmPair implements Serializable {

	private String lsmFilepath1;
	private String lsmFilepath2;
	private String mergedFilepath;
	
	public MergedLsmPair(String lsmFilepath1, String lsmFilepath2, String mergedFilepath) {
		this.lsmFilepath1 = lsmFilepath1;
		this.lsmFilepath2 = lsmFilepath2;
		this.mergedFilepath = mergedFilepath;
	}

	public String getLsmFilepath1() {
		return lsmFilepath1;
	}

	public String getLsmFilepath2() {
		return lsmFilepath2;
	}

	public String getMergedFilepath() {
		return mergedFilepath;
	}
}
