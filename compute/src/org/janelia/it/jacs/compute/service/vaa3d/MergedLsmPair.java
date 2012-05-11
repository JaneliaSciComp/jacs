package org.janelia.it.jacs.compute.service.vaa3d;

/**
 * A pair of LSMs which are merged into a single V3DRAW file. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MergedLsmPair extends CombinedFile {
	
	public MergedLsmPair(String lsmFilepath1, String lsmFilepath2, String mergedFilepath) {
		super(lsmFilepath1, lsmFilepath2, mergedFilepath);
	}

	public String getLsmFilepath1() {
		return getFilepath1();
	}

	public String getLsmFilepath2() {
		return getFilepath2();
	}

	public String getMergedFilepath() {
		return getOutputFilepath();
	}
}
