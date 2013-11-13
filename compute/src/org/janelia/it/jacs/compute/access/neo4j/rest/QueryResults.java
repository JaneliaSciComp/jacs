package org.janelia.it.jacs.compute.access.neo4j.rest;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
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
    
    public static QueryResults fromJson(String json, Class... resultTypes) throws Exception {
        
//        System.out.println(json);
        
        Gson gson = new Gson();
        JsonParser parser = new JsonParser();
        JsonObject root = parser.parse(json).getAsJsonObject();
        List<String> columns = gson.fromJson(root.get("columns"), List.class);
        List<List<Object>> data = new ArrayList<List<Object>>();
        
        if (root.get("exception")!=null) {
            String message = root.get("message").getAsString();
            String fullName = root.get("fullname").getAsString();
            JsonArray stackTrace = root.get("stacktrace").getAsJsonArray();
            
            // TODO: in the future, this stacktrace should be included as a backtrace in the thrown Exception
            // for now, we are satisfied with printing it to STDERR
            
            System.err.println("Exception in Neo4j: "+fullName);
            for(int i=0; i<stackTrace.size(); i++) {
                String stackElement = stackTrace.get(i).getAsString();
                System.err.println("\tat "+stackElement);
            }
            throw new Exception(fullName+": "+message);
        }
        
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

    /**
     * Assumes your resultTypes include a NodeResult
     * @return
     */
    public List<NodeResult> getNodeResults() {
        
        List<NodeResult> nodeResults = new ArrayList<NodeResult>();
        for(List<Object> row : getData()) {
            for(Object obj : row) {
                if (obj instanceof NodeResult) {
                    NodeResult node = (NodeResult)obj;
                    nodeResults.add(node);
                }
            }
        }
        return nodeResults;
    }
    
    /**
     * Assumes your resultTypes include a NodeResult
     * @return
     */
    public List<Entity> getEntityResults() {
        List<Entity> entityResults = new ArrayList<Entity>();
        for(List<Object> row : getData()) {
            for(Object obj : row) {
                if (obj instanceof NodeResult) {
                    NodeResult node = (NodeResult)obj;
                    Entity entity = EntityConverter.convertToEntity(node);
                    entityResults.add(entity);
                }
            }
        }
        return entityResults;
    }

    /**
     * Assumes your resultTypes are a RelationshipResult and it's child NodeResult
     * @return
     */
    public List<EntityData> getEntityDataResults() {
        List<EntityData> entityDataResults = new ArrayList<EntityData>();
        for(List<Object> row : getData()) {
            RelationshipResult relation = null;
            NodeResult node = null;
            for(Object obj : row) {
                if (obj instanceof RelationshipResult) {
                    relation = (RelationshipResult)obj;
                }
                if (obj instanceof NodeResult) {
                    node = (NodeResult)obj;
                }
            }
            
            if (node!=null && relation!=null) {
                EntityData entityData = EntityConverter.convertToEntityData(relation, node);
                entityDataResults.add(entityData);
            }
            
        }
        return entityDataResults;
    }
    
}
