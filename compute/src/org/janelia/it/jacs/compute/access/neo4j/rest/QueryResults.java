package org.janelia.it.jacs.compute.access.neo4j.rest;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;

public class QueryResults {

    @SerializedName("columns") private List<String> columns;
    @SerializedName("data") private List<List<Object>> data;

    private QueryResults(List<String> columns, List<List<Object>> data) {
        this.columns = columns;
        this.data = data;
    }

    public List<String> getColumns() {
        return columns;
    }

    public List<List<Object>> getData() {
        return data;
    }
    
    public static QueryResults fromJson(String json, Class... resultTypes) {
        
        Gson gson = new Gson();
        JsonParser parser = new JsonParser();
        JsonObject root = parser.parse(json).getAsJsonObject();
        List<String> columns = gson.fromJson(root.get("columns"), List.class);
        List<List<Object>> data = new ArrayList<List<Object>>();
        
        for(JsonElement e : root.get("data").getAsJsonArray()) {
            List<Object> row = new ArrayList<Object>();
            int c=0;
            for(JsonElement f : e.getAsJsonArray()) {
                Class resultType = resultTypes[c];
                row.add(gson.fromJson(f, resultType));
                c++;
            }
            data.add(row);
        }
        
        return new QueryResults(columns, data); 
    }
}
