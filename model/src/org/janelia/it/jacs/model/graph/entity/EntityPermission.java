package org.janelia.it.jacs.model.graph.entity;

import java.io.Serializable;

import org.janelia.it.jacs.model.graph.annotations.GraphId;
import org.janelia.it.jacs.model.graph.annotations.GraphPermission;
import org.janelia.it.jacs.model.graph.annotations.GraphProperty;

@GraphPermission
public class EntityPermission implements Serializable {

    @GraphId
    private Long id;
    
    @GraphProperty("subjectKey")
    private String subjectKey;
    
    @GraphProperty("permissions")
    private String rights;

    public boolean isOwner() {
        return rights.contains("o");
    }
    
    public boolean canRead() {
        return rights.contains("r");
    }

    public boolean canWrite() {
        return rights.contains("w");
    }
    
    /* EVERYTHING BELOW IS AUTO GENERATED */

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    public String getSubjectKey() {
        return subjectKey;
    }

    public void setSubjectKey(String subjectKey) {
        this.subjectKey = subjectKey;
    }

    public String getRights() {
        return rights;
    }

    public void setRights(String rights) {
        this.rights = rights;
    }
    
}
