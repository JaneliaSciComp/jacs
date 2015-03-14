package org.janelia.it.jacs.compute.service.vaa3d;

/**
 * A pair of LSMs which are merged into a single V3DRAW file. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MergedLsmPair extends CombinedFile {
	
	private String orignalFilepath1;
	private String orignalFilepath2;
    private String tag;
    
	public MergedLsmPair(String orignalFilepath1, String orignalFilepath2, String lsmFilepath1, String lsmFilepath2, String mergedFilepath, String tag) {
		super(lsmFilepath1, lsmFilepath2, mergedFilepath);
		this.orignalFilepath1 = orignalFilepath1;
		this.orignalFilepath2 = orignalFilepath2;
		this.tag = tag;
	}

	public String getTag() {
        return tag;
    }

    public String getLsmFilepath1() {
		return getFilepath1();
	}

	public String getLsmFilepath2() {
		return getFilepath2();
	}
	
	public String getOrignalFilepath1() {
		return orignalFilepath1;
	}

	public String getOrignalFilepath2() {
		return orignalFilepath2;
	}

	public String getMergedFilepath() {
		return getOutputFilepath();
	}
	
	public MergedLsmPair getMovedLsmPair(String newPath1, String newPath2) {
		return new MergedLsmPair(orignalFilepath1, orignalFilepath2, newPath1, newPath2, getMergedFilepath(), tag);
	}
}
