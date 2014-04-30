package org.janelia.it.jacs.model.domain;

public class ReverseReference {
    
    private String referringType;
    private String referenceAttr;
    private Long referenceId;
    private Long count;

    /* EVERYTHING BELOW IS AUTO-GENERATED */
    
    public String getReferringType() {
        return referringType;
    }
    public void setReferringType(String referringType) {
        this.referringType = referringType;
    }
    public String getReferenceAttr() {
        return referenceAttr;
    }
    public void setReferenceAttr(String referenceAttr) {
        this.referenceAttr = referenceAttr;
    }
    public Long getReferenceId() {
        return referenceId;
    }
    public void setReferenceId(Long referenceId) {
        this.referenceId = referenceId;
    }
    public Long getCount() {
        return count;
    }
    public void setCount(Long count) {
        this.count = count;
    }
}
