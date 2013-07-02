package org.janelia.it.jacs.compute.service.entity.sample;

import java.util.ArrayList;
import java.util.List;

/**
 * A group of SlideImages with a tag and an anatomical area. In general, a TileImageGroup usually contains either
 * a single LSM, or two LSMs to be merged, but it may contain an arbitrary number of images.  
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SlideImageGroup {
    
    private String anatomicalArea;
    private String tag;
    private List<SlideImage> images = new ArrayList<SlideImage>();

    public SlideImageGroup(String anatomicalArea, String tag) {
        this.anatomicalArea = anatomicalArea;
        this.tag = tag;
    }
    
    public String getAnatomicalArea() {
        return anatomicalArea;
    }

    public String getTag() {
		return tag;
	}

	public List<SlideImage> getImages() {
		return images;
	}

	public void addFile(SlideImage image) {
		images.add(image);
	}
}