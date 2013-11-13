package org.janelia.it.jacs.compute.access.neo4j.rest;

import java.util.Map;

import com.google.gson.annotations.SerializedName;

public class RelationshipResult {
    
    @SerializedName("data") private Map<String,String> propertyMap;
    @SerializedName("type") private String type;
    
//    @SerializedName("start") private String startUri;
//    @SerializedName("self") private String selfUri;
//    @SerializedName("property") private String propertyUri;
//    @SerializedName("properties") private String propertiesUri;
//    @SerializedName("end") private String endUri;
    
    public Map<String, String> getProperties() {
        return propertyMap;
    }
    
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "RelationshipResult [type="+type+", propertyMap=" + propertyMap + "]";
    }
}
