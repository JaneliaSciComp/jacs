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
	private String cv;

    public SageTerm() {
    }

    public SageTerm(String name,
                    String displayName,
                    String dataType,
                    String definition,
					String cv) {
        this.name = name;
        this.displayName = displayName;
        this.dataType = dataType;
        this.definition = definition;
		this.cv = cv;
    }

    public String getKey() {
		return cv+"_"+name;
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

	public String getCv() {
		return cv;
	}

	public void setCv(String cv) {
		this.cv = cv;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof SageTerm)) return false;

		SageTerm sageTerm = (SageTerm) o;

		if (!cv.equals(sageTerm.cv)) return false;
		if (!name.equals(sageTerm.name)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = name.hashCode();
		result = 31 * result + cv.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "SageTerm[" +
				"name='" + name + '\'' +
				", displayName='" + displayName + '\'' +
				", dataType='" + dataType + '\'' +
				", definition='" + definition + '\'' +
				", cv='" + cv + '\'' +
				']';
	}
}