package org.janelia.it.jacs.model.domain.sample;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.janelia.it.jacs.model.domain.interfaces.HasFileGroups;

/**
 * Post-processing that is run on the tile images and (if relevant) the stitched image.
 * 
 * This differs from the SampleProcessingResult in that it is not specific to a single
 * anatomical area. Thus, a single post-processing result may contain results from the 
 * processing of multiple areas. For example, the Brain and VNC may be processed together 
 * in order to normalize both.  
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SamplePostProcessingResult extends PipelineResult implements HasFileGroups {

	private Map<String,FileGroup> groups = new HashMap<>();

	@Override
	public Set<String> getGroupKeys() {
		return groups.keySet();
	}

    @Override
	public FileGroup getGroup(String key) {
		return groups.get(key);
	}

    /* EVERYTHING BELOW IS AUTO-GENERATED */

    @Override
	public Map<String, FileGroup> getGroups() {
		return groups;
	}

	public void setGroups(Map<String, FileGroup> groups) {
		this.groups = groups;
	}
}
