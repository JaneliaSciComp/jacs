package org.janelia.it.jacs.model.domain;

/**
 * A reverse reference to a set of DomainObjects in another collection. The referring DomainObjects
 * each have type <i>referringType</i> and contain an attribute with name <i>referenceAttr</i> and
 * value <i>referenceId</i>.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime*result
                +((referenceAttr==null) ? 0 : referenceAttr.hashCode());
        result = prime*result
                +((referenceId==null) ? 0 : referenceId.hashCode());
        result = prime*result
                +((referringType==null) ? 0 : referringType.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this==obj) {
            return true;
        }
        if (obj==null) {
            return false;
        }
        if (getClass()!=obj.getClass()) {
            return false;
        }
        ReverseReference other = (ReverseReference) obj;
        if (referenceAttr==null) {
            if (other.referenceAttr!=null) {
                return false;
            }
        }
        else if (!referenceAttr.equals(other.referenceAttr)) {
            return false;
        }
        if (referenceId==null) {
            if (other.referenceId!=null) {
                return false;
            }
        }
        else if (!referenceId.equals(other.referenceId)) {
            return false;
        }
        if (referringType==null) {
            if (other.referringType!=null) {
                return false;
            }
        }
        else if (!referringType.equals(other.referringType)) {
            return false;
        }
        return true;
    }
}
