package org.janelia.it.jacs.model.domain;

import java.util.Set;

import org.janelia.it.jacs.model.domain.interfaces.HasIdentifier;
import org.janelia.it.jacs.model.domain.support.MongoMapped;
import org.jongo.marshall.jackson.oid.MongoId;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * A user or group in the Workstation system. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@MongoMapped(collectionName = "subject")
public class Subject implements HasIdentifier {
    
    @MongoId
    private Long id;
    private String key;
    private String name;
    private String fullName;
    private String email;
    private Set<String> groups;

    /* EVERYTHING BELOW IS AUTO-GENERATED */
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Set<String> getGroups() {
        return groups;
    }

    public void setGroups(Set<String> groups) {
        this.groups = groups;
    }
}
