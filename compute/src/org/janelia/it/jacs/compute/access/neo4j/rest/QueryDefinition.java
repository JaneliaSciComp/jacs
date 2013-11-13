package org.janelia.it.jacs.compute.access.neo4j.rest;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

public class QueryDefinition {

    @SerializedName("query") private String cypher;
    @SerializedName("params") private Map<String,Object> params = new HashMap<String,Object>();

    public QueryDefinition(String cypher) {
        this.cypher = cypher;
    }

    public void setCypher(String cypher) {
        this.cypher = cypher;
    }
    
    public String getCypher() {
        return cypher;
    }

    public void addParam(String key, Object value) {
        params.put(key, value);
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);  
    }
}