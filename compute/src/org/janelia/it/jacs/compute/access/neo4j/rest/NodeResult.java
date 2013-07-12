package org.janelia.it.jacs.compute.access.neo4j.rest;

import java.util.Map;

import com.google.gson.annotations.SerializedName;

public class NodeResult {
    
    @SerializedName("data") private Map<String,String> propertyMap;
    
    @SerializedName("self") private String selfUri;
    @SerializedName("outgoing_relationships") private String outgoingRelationshipsUri;
    @SerializedName("labels") private String labelsUri;
    @SerializedName("all_typed_relationships") private String allTypedRelationshipsUri;
    @SerializedName("traverse") private String traverseUri;
    @SerializedName("property") private String propertyUri;
    @SerializedName("outgoing_typed_relationships") private String outgoingTypedRelationshipsUri;
    @SerializedName("properties") private String propertiesUri;
    @SerializedName("incoming_relationships") private String incomingRelationshipsUri;
    @SerializedName("create_relationship") private String createRelationshipUri;
    @SerializedName("paged_traverse") private String pagedTraverseUri;
    @SerializedName("all_relationships") private String allRelationshipsUri;
    @SerializedName("incoming_typed_relationships") private String incomingTypedRelationshipsUri;
    
    public Map<String, String> getProperties() {
        return propertyMap;
    }

    public String getSelfUri() {
        return selfUri;
    }

    public void setSelfUri(String selfUri) {
        this.selfUri = selfUri;
    }

    @Override
    public String toString() {
        return "NodeResult [propertyMap=" + propertyMap + "]";
    }
    
    
}
