package org.janelia.it.jacs.compute.service.vaa3d;

/**
 * A pair of LSMs which are merged into a single V3DRAW file. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MergedLsmPair extends CombinedFile {
	
	private String originalFilepath1;
	private String originalFilepath2;
    private String tag;
    
	public MergedLsmPair(String originalFilepath1, String originalFilepath2, String lsmFilepath1, String lsmFilepath2, String mergedFilepath, String tag) {
		super(lsmFilepath1, lsmFilepath2, mergedFilepath);
		this.originalFilepath1 = originalFilepath1;
		this.originalFilepath2 = originalFilepath2;
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
	
	public String getOriginalFilepath1() {
		return originalFilepath1;
	}

	public String getOriginalFilepath2() {
		return originalFilepath2;
	}

	public String getMergedFilepath() {
		return getOutputFilepath();
	}
	
	public MergedLsmPair getMovedLsmPair(String newPath1, String newPath2) {
		return new MergedLsmPair(originalFilepath1, originalFilepath2, newPath1, newPath2, getMergedFilepath(), tag);
	}
}
