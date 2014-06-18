package org.janelia.it.jacs.model.domain;

/**
 * A reference to a DomainObject in a specific collection.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
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
    
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((targetId == null) ? 0 : targetId.hashCode());
		result = prime * result
				+ ((targetType == null) ? 0 : targetType.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Reference other = (Reference) obj;
		if (targetId == null) {
			if (other.targetId != null)
				return false;
		} else if (!targetId.equals(other.targetId))
			return false;
		if (targetType == null) {
			if (other.targetType != null)
				return false;
		} else if (!targetType.equals(other.targetType))
			return false;
		return true;
	}   
    
    
}
