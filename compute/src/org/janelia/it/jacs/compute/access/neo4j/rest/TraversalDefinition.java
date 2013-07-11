package org.janelia.it.jacs.compute.access.neo4j.rest;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

public class TraversalDefinition {
    
    public static final String DEPTH_FIRST = "depth_first";
    public static final String NODE = "node";
    public static final String ALL = "all";

    private String order = DEPTH_FIRST;
    private List<Relation> relationships = new ArrayList<Relation>();
    private String uniqueness = NODE;
    private ReturnFilter returnFilter = new ReturnFilter();
    private int maxDepth = 1;
    

    public void setOrder(String order) {
        this.order = order;
    }

    public void setUniqueness(String uniqueness) {
        this.uniqueness = uniqueness;
    }

    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    public void setReturnFilter(ReturnFilter returnFilter) {
        this.returnFilter = returnFilter;
    }

    public void addRelationship(Relation relationship) {
        this.relationships.add(relationship);
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);  
    }
    
    public static class ReturnFilter {
        
        private String language = "builtin";
        private String name = "all";

        public ReturnFilter() {
        }
        
        public ReturnFilter(String language, String name) {
            this.language = language;
            this.name = name;
        }
        
        public String getLanguage() {
            return language;
        }
        
        public String getName() {
            return name;
        }
    }

    public static class Relation {
        
        public static final String OUT = "out";
        public static final String IN = "in";
        public static final String BOTH = "both";
        
        private String type;
        private String direction;

        public Relation() {
        }
        
        public Relation(String type, String direction) {
            setType(type);
            setDirection(direction);
        }

        public void setType(String type) {
            this.type = type;
        }

        public void setDirection(String direction) {
            this.direction = direction;
        }
    }
}