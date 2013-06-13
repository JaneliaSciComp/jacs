package org.janelia.it.jacs.compute.access.neo4j;

//import org.springframework.data.neo4j.annotation.RelationshipEntity;
//import org.springframework.data.neo4j.annotation.StartNode;

//@RelationshipEntity(type="PARENT_CHILD")
public class GraphRelationship {
	
//	@StartNode private GraphEntity parent;
//	@StartNode private GraphEntity child;
	private String role;
	
//	public GraphEntity getParent() {
//		return parent;
//	}
//	public void setParent(GraphEntity parent) {
//		this.parent = parent;
//	}
//	public GraphEntity getChild() {
//		return child;
//	}
//	public void setChild(GraphEntity child) {
//		this.child = child;
//	}
	public String getRole() {
		return role;
	}
	public void setRole(String role) {
		this.role = role;
	}
}
