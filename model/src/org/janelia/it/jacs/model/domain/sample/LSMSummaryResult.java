package org.janelia.it.jacs.model.domain.sample;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.janelia.it.jacs.model.domain.interfaces.HasFileGroups;

/**
 * Summary files for all of the LSMs in an ObjectiveSample.
 * Generally this consists of MIPs and movies.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */

public class LSMSummaryResult extends PipelineResult implements HasFileGroups {

	private Map<String,FileGroup> groups = new HashMap<>();

	@Override
	@JsonIgnore
	public Set<String> getGroupKeys() {
		return groups.keySet();
	}

    @Override
	public FileGroup getGroup(String key) {
		return groups.get(key);
	}

    @Override
	public Map<String, FileGroup> getGroups() {
		return groups;
	}

	public void setGroups(Map<String, FileGroup> groups) {
	    if (groups==null) throw new IllegalArgumentException("Property cannot be null");
		this.groups = groups;
	}
}
