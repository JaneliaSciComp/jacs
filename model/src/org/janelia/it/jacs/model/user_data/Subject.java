package org.janelia.it.jacs.model.user_data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlValue;

@XmlAccessorType(XmlAccessType.NONE)
public class Subject implements java.io.Serializable {
	
    private Long id;
    private String name = "";
    private String fullName = "";
    @XmlValue
    private String key = "";
    
    public Subject() {
    }
    
	public Subject(String name, String fullName, String key) {
		this.name = name;
		this.fullName = fullName;
		this.key = key;
	}
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}
}