package org.janelia.it.jacs.model.entity;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name="EntityRoleActor")
public class EntityActorPermission implements java.io.Serializable {
	
	@XmlAttribute(name="guid")
    private Long id;

    @XmlTransient
    private Entity entity;

    @XmlElement
    private String subjectKey;

    @XmlElement
    private String permissions;
    
    public EntityActorPermission() {
    }

    public EntityActorPermission(Long id) {
        this.id = id;
    }
    
    public EntityActorPermission(Entity entity, String subjectKey, String permissions) {
		this.entity = entity;
		this.subjectKey = subjectKey;
		this.permissions = permissions;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Entity getEntity() {
		return entity;
	}

	public void setEntity(Entity entity) {
		this.entity = entity;
	}

	public String getSubjectKey() {
		return subjectKey;
	}

	public void setSubjectKey(String subjectKey) {
		this.subjectKey = subjectKey;
	}

	public String getPermissions() {
		return permissions;
	}

	public void setPermissions(String permissions) {
		this.permissions = permissions;
	}
}