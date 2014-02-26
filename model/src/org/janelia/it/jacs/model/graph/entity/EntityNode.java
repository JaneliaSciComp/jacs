package org.janelia.it.jacs.model.graph.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.janelia.it.jacs.model.graph.annotations.GraphId;
import org.janelia.it.jacs.model.graph.annotations.GraphNode;
import org.janelia.it.jacs.model.graph.annotations.GraphProperty;
import org.janelia.it.jacs.model.graph.annotations.NodeInitFlag;
import org.janelia.it.jacs.model.graph.annotations.Permissions;
import org.janelia.it.jacs.model.graph.annotations.RelatedTo;
import org.janelia.it.jacs.model.graph.annotations.RelatedToVia;
import org.janelia.it.jacs.model.graph.annotations.RelationshipInitFlag;

/**
 * A node in the entity graph.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@GraphNode
public class EntityNode implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
    @GraphId
    private Long id;

    @GraphProperty("name")
    private String name;

    @GraphProperty("entityTypeName")
    private String type;
    
    @GraphProperty("ownerKey")
    private String ownerKey;

    @GraphProperty("creationDate")
    private Date creationDate;

    @GraphProperty("updatedDate")
    private Date updatedDate;

    @Permissions
    private List<EntityPermission> permissions;
    
    @RelatedToVia
    private List<EntityRelationship> relationships;

    @RelatedTo
    private List<EntityNode> relatedNodes;
    
    @NodeInitFlag
    private boolean thisInit = false;
    
    @RelationshipInitFlag
    private boolean relsInit = false;

    /* EVERYTHING BELOW IS AUTO GENERATED */
    
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
    
    public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getOwnerKey() {
        return ownerKey;
    }

    public void setOwnerKey(String ownerKey) {
        this.ownerKey = ownerKey;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public Date getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(Date updatedDate) {
        this.updatedDate = updatedDate;
    }

    public List<EntityPermission> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<EntityPermission> permissions) {
        this.permissions = permissions;
    }
    
    public List<EntityRelationship> getRelationships() {
        return relationships;
    }

    public void setRelationships(List<EntityRelationship> relationships) {
        this.relationships = relationships;
    }

    public List<EntityNode> getRelatedNodes() {
        return relatedNodes;
    }

    public void setRelatedNodes(List<EntityNode> relatedNodes) {
        this.relatedNodes = relatedNodes;
    }

    public boolean isThisInit() {
        return thisInit;
    }

    public boolean isRelsInit() {
        return relsInit;
    }
}
