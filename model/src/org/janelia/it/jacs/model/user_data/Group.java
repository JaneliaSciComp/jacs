
package org.janelia.it.jacs.model.user_data;

import com.google.gwt.user.client.rpc.IsSerializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@XmlAccessorType(XmlAccessType.NONE)
public class Group extends Subject implements Serializable, IsSerializable
{
	private static final long serialVersionUID = 8234809090527460870l;

	public static final String ADMIN_GROUP_NAME = "admin";
    public static final String ADMIN_GROUP_KEY = "group:admin";
	public static final String ALL_USERS_GROUP_NAME = "workstation_users";
    public static final String ALL_USERS_GROUP_KEY = "group:workstation_users";
    
    @XmlElement(name="userRelationship")
    @XmlElementWrapper(name="userRelationships")
    private Set<SubjectRelationship> userRelationships = new HashSet<SubjectRelationship>(0);
    
    public Group() {
    }

	public Group(String name, String fullName) {
		super(name, fullName, "group:"+name);
	}

    @Override
    public void setName(String name) {
		super.setName(name);
		setKey("group:"+name);
	}
    
	public Set<SubjectRelationship> getUserRelationships() {
		return userRelationships;
	}

	public void setUserRelationships(Set<SubjectRelationship> userRelationships) {
		this.userRelationships = userRelationships;
	}
}