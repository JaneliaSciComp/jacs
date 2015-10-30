package org.janelia.it.jacs.model.domain.sample;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Summary files for all of the LSMs in an ObjectiveSample. 
 * Generally this consists of MIPs and movies. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class LSMSummaryResult extends PipelineResult {

	private Map<String,FileGroup> groups = new HashMap<>();

	public Set<String> getGroupKeys() {
		return groups.keySet();
	}
	
	public FileGroup getGroup(String key) {
		return groups.get(key);
	}

    /* EVERYTHING BELOW IS AUTO-GENERATED */
	
	public Map<String, FileGroup> getGroups() {
		return groups;
	}

	public void setGroups(Map<String, FileGroup> groups) {
		this.groups = groups;
	}
}
