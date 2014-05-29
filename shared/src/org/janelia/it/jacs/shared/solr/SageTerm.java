package org.janelia.it.jacs.shared.solr;

import java.io.Serializable;

/**
 * A controlled vocabulary term from the Sage database.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SageTerm implements Serializable {
	
	private String name;
	private String displayName;
	private String dataType;
	private String definition;

    public SageTerm() {
    }

    public SageTerm(String name,
                    String displayName,
                    String dataType,
                    String definition) {
        this.name = name;
        this.displayName = displayName;
        this.dataType = dataType;
        this.definition = definition;
    }

    public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDisplayName() {
		return displayName;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	public String getDataType() {
		return dataType;
	}
	public void setDataType(String dataType) {
		this.dataType = dataType;
	}
	public String getDefinition() {
		return definition;
	}
	public void setDefinition(String definition) {
		this.definition = definition;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		SageTerm other = (SageTerm) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return "SageTerm[name:"+name+",displayName:"+displayName+",dataType:"+dataType+"]";
	}
}