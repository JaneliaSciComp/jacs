package org.janelia.it.jacs.model.domain;

/**
 * A reference to a DomainObject in a specific collection.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Reference {

    private String targetClassName;
    private Long targetId;

    /* EVERYTHING BELOW IS AUTO-GENERATED */
    public Reference() {
    }

    public Reference(String targetClassName, Long targetId) {
        this.targetClassName = targetClassName;
        this.targetId = targetId;
    }

    public String getTargetClassName() {
        return targetClassName;
    }

    public void setTargetClassName(String type) {
        this.targetClassName = type;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime*result
                +((targetId==null) ? 0 : targetId.hashCode());
        result = prime*result
                +((targetClassName==null) ? 0 : targetClassName.hashCode());
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
        Reference other = (Reference) obj;
        if (targetId==null) {
            if (other.targetId!=null) {
                return false;
            }
        }
        else if (!targetId.equals(other.targetId)) {
            return false;
        }
        if (targetClassName==null) {
            if (other.targetClassName!=null) {
                return false;
            }
        }
        else if (!targetClassName.equals(other.targetClassName)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Reference[" + targetClassName + "#" + targetId + "]";
    }
}
