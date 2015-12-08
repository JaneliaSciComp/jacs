package org.janelia.it.jacs.model.domain;

import org.janelia.it.jacs.model.domain.support.MongoMapped;
import org.jongo.marshall.jackson.oid.MongoId;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * A subject's preference. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@MongoMapped(collectionName = "preference")
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "class")
public class Preference {
    
    @MongoId
    private Long id;
    private String subjectKey;
    private String category;
    private String key;
    private String value;

    public Preference() {
    }
    
    public Preference(String subjectKey, String category, String key, String value) {
        this.subjectKey = subjectKey;
        this.category = category;
        this.key = key;
        this.value = value;
    }

    /* EVERYTHING BELOW IS AUTO-GENERATED */
    
    public String getCategory() {
        return category;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSubjectKey() {
        return subjectKey;
    }

    public void setSubjectId(String subjectKey) {
        this.subjectKey = subjectKey;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
