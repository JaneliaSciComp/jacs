package org.janelia.it.jacs.compute.access.neo4j;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
//import org.springframework.data.neo4j.annotation.GraphId;
//import org.springframework.data.neo4j.annotation.Indexed;
//import org.springframework.data.neo4j.annotation.NodeEntity;
//import org.springframework.data.neo4j.annotation.RelatedTo;

//@NodeEntity
public class GraphEntity {

//	@GraphId 
	private Long id;

//	@Indexed
	private Long entityId;

//	@Indexed
    private String name;
    
	private String username;

	private String entityType;
	
//    private Date creationDate;
//
//    private Date updatedDate;
//    
//    private Map<String, String> attributes = new HashMap<String, String>();
    
//    @RelatedTo(type = "PARENT_CHILD", direction = Direction.OUTGOING)
    private Set<GraphEntity> children = new HashSet<GraphEntity>();
    
//    @RelatedTo(type = "PARENT_CHILD", direction = Direction.INCOMING)
    private Set<GraphEntity> parents = new HashSet<GraphEntity>();

//    @Fetch @RelatedToVia(type = "PARENT_CHILD", direction = Direction.OUTGOING) 
//    private Set<GraphRelationship> parentRelationships;
//    
//    @Fetch @RelatedToVia(type = "PARENT_CHILD", direction = Direction.INCOMING) 
//    private Set<GraphRelationship> childRelationships;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getEntityId() {
		return entityId;
	}

	public void setEntityId(Long entityId) {
		this.entityId = entityId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getEntityType() {
		return entityType;
	}

	public void setEntityType(String entityType) {
		this.entityType = entityType;
	}

	public Set<GraphEntity> getChildren() {
		return children;
	}

	public void setChildren(Set<GraphEntity> children) {
		this.children = children;
	}

	public Set<GraphEntity> getParents() {
		return parents;
	}

	public void setParents(Set<GraphEntity> parents) {
		this.parents = parents;
	}

//	public Set<GraphRelationship> getParentRelationships() {
//		return parentRelationships;
//	}
//
//	public void setParentRelationships(Set<GraphRelationship> parentRelationships) {
//		this.parentRelationships = parentRelationships;
//	}
//
//	public Set<GraphRelationship> getChildRelationships() {
//		return childRelationships;
//	}
//
//	public void setChildRelationships(Set<GraphRelationship> childRelationships) {
//		this.childRelationships = childRelationships;
//	}
}