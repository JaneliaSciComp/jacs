package org.janelia.it.jacs.compute.access.neo4j.rest;

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

public class QueryResults {

    @SerializedName("columns") private List<String> columns;
    @SerializedName("data") private List<List<Node>> data;
    
    public List<String> getColumns() {
        return columns;
    }

    public List<List<Node>> getData() {
        return data;
    }
    
    public static QueryResults fromJson(String json) {
        Gson gson = new Gson();
        return (QueryResults)gson.fromJson(json, QueryResults.class); 
    }
}
