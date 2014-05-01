package org.janelia.it.jacs.model.domain;

public class Reference {
    
    private String targetType;
    private Long targetId;

    /* EVERYTHING BELOW IS AUTO-GENERATED */

    public Reference() {
    }
    public Reference(String targetType, Long targetId) {
        this.targetType = targetType;
        this.targetId = targetId;
    }
    
    public String getTargetType() {
        return targetType;
    }
    public void setTargetType(String type) {
        this.targetType = type;
    }
    public Long getTargetId() {
        return targetId;
    }
    public void setTargetId(Long id) {
        this.targetId = id;
    }   
}
