package org.janelia.it.jacs.model.graph.entity;

import java.io.Serializable;
import java.util.Date;

import org.janelia.it.jacs.model.graph.annotations.EndNode;
import org.janelia.it.jacs.model.graph.annotations.GraphId;
import org.janelia.it.jacs.model.graph.annotations.GraphProperty;
import org.janelia.it.jacs.model.graph.annotations.GraphRelationship;
import org.janelia.it.jacs.model.graph.annotations.RelationshipType;
import org.janelia.it.jacs.model.graph.annotations.StartNode;

/**
 * An edge in the entity graph.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@GraphRelationship
public class EntityRelationship implements Serializable {

    @GraphId
    private Long id;
    
    @GraphProperty("ownerKey")
    private String ownerKey;
    
    @GraphProperty("creationDate")
    private Date creationDate;

    @GraphProperty("updatedDate")
    private Date updatedDate;

    @GraphProperty("orderIndex")
    private Integer orderIndex;
    
    @RelationshipType
    @GraphProperty("entityAttrName")
    private String type;

    @StartNode
    private EntityNode sourceNode;

    @EndNode
    private EntityNode targetNode;
    
    /* EVERYTHING BELOW IS AUTO GENERATED */
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public EntityNode getSourceNode() {
        return sourceNode;
    }

    public void setSourceNode(EntityNode sourceNode) {
        this.sourceNode = sourceNode;
    }

    public EntityNode getTargetNode() {
        return targetNode;
    }

    public void setTargetNode(EntityNode targetNode) {
        this.targetNode = targetNode;
    }
}
