package org.janelia.it.jacs.model.entity.json;

import java.util.List;

import org.janelia.it.jacs.model.domain.sample.LineRelease;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.util.ISO8601Utils;

@JsonInclude(Include.NON_NULL)
public class JsonRelease {
	
	@JsonIgnore
	private LineRelease release;
	
	public JsonRelease(LineRelease entity) {
		this.release = entity;
	}
	
	@JsonProperty
	public String getId() {
		return release.getId().toString();
	}

    @JsonProperty
    public String getOwnerKey() {
        return release.getOwnerKey();
    }
    
	@JsonProperty
	public String getName() {
		return release.getName();
	}

    @JsonProperty
    public String getCreationDate() {
        return ISO8601Utils.format(release.getCreationDate());
    }
    
    @JsonProperty
    public String getReleaseDate() {
        return ISO8601Utils.format(release.getReleaseDate());
    }

    @JsonProperty
    public Boolean getSAGESync() {
        return release.isSageSync();
    }

    @JsonProperty
    public Integer getLagTimeMonths() {
        return release.getLagTimeMonths();
    }

    @JsonProperty
    public List<String> getDataSets() {
        return release.getDataSets();
    }
    @JsonProperty
    public List<String> getAnnotators() {
        return release.getAnnotators();
    }

    @JsonProperty
    public List<String> getSubscribers() {
        return release.getSubscribers();
    }
}
