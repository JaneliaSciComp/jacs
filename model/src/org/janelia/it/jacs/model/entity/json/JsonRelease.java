package org.janelia.it.jacs.model.entity.json;

import java.util.List;

import org.hibernate.Hibernate;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Task;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.NON_NULL)
public class JsonRelease {
	
	@JsonIgnore
	private Entity entity;
	
	public JsonRelease(Entity entity) {
		this.entity = entity;
	}
	
	@JsonProperty
	public String getId() {
		return entity.getId().toString();
	}

    @JsonProperty
    public String getOwnerKey() {
        if (!Hibernate.isInitialized(entity)) return null;
        return entity.getOwnerKey();
    }
    
	@JsonProperty
	public String getName() {
		if (!Hibernate.isInitialized(entity)) return null;
		return entity.getName();
	}

    @JsonProperty
    public String getReleaseDate() {
        if (!Hibernate.isInitialized(entity)) return null;
        return entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_RELEASE_DATE);
    }

    @JsonProperty
    public Integer getLagTimeMonths() {
        if (!Hibernate.isInitialized(entity)) return null;
        String lagTimeMonthsStr = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_LAG_TIME_MONTHS);
        if (lagTimeMonthsStr!=null) {
            return new Integer(lagTimeMonthsStr);
        }
        return null;
    }

    @JsonProperty
    public List<String> getDataSets() {
        if (!Hibernate.isInitialized(entity)) return null;
        String dataSetsStr = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SETS);
        if (dataSetsStr != null) {
            return Task.listOfStringsFromCsvString(dataSetsStr);
        }
        return null;
    }
    @JsonProperty
    public List<String> getAnnotators() {
        if (!Hibernate.isInitialized(entity)) return null;
        String dataSetsStr = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATORS);
        if (dataSetsStr != null) {
            return Task.listOfStringsFromCsvString(dataSetsStr);
        }
        return null;
    }

    @JsonProperty
    public List<String> getSubscribers() {
        if (!Hibernate.isInitialized(entity)) return null;
        String dataSetsStr = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_SUBSCRIBERS);
        if (dataSetsStr != null) {
            return Task.listOfStringsFromCsvString(dataSetsStr);
        }
        return null;
    }
}
