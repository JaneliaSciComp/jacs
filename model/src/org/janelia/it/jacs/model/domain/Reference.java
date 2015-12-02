package org.janelia.it.jacs.model.domain;

/**
 * A reference to a DomainObject in a specific collection.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Reference {

    private String targetCollectionName;
    private Long targetId;

    /* EVERYTHING BELOW IS AUTO-GENERATED */
    public Reference() {
    }

    public Reference(String targetCollectionName, Long targetId) {
        this.targetCollectionName = targetCollectionName;
        this.targetId = targetId;
    }

    public String getCollectionName() {
        return targetCollectionName;
    }

    public void setCollectionName(String type) {
        this.targetCollectionName = type;
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
                +((targetCollectionName==null) ? 0 : targetCollectionName.hashCode());
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
        if (targetCollectionName==null) {
            if (other.targetCollectionName!=null) {
                return false;
            }
        }
        else if (!targetCollectionName.equals(other.targetCollectionName)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Reference[" + targetCollectionName + "#" + targetId + "]";
    }
}
