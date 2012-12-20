package org.janelia.it.jacs.model.user_data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import com.google.gwt.user.client.rpc.IsSerializable;

@XmlAccessorType(XmlAccessType.NONE)
public class SubjectRelationship implements java.io.Serializable, IsSerializable {
	
	public static final String TYPE_GROUP_OWNER = "owner";
	public static final String TYPE_GROUP_MEMBER = "member";
	
	private Long id;
	private User user;
	private Group group;
	private String relationshipType;
    
    public SubjectRelationship() {
    }

	public SubjectRelationship(User user, Group group, String relationshipType) {
		this.user = user;
		this.group = group;
		this.relationshipType = relationshipType;
	}
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Group getGroup() {
		return group;
	}

	public void setGroup(Group group) {
		this.group = group;
	}

	public String getRelationshipType() {
		return relationshipType;
	}

	public void setRelationshipType(String relationshipType) {
		this.relationshipType = relationshipType;
	}
}