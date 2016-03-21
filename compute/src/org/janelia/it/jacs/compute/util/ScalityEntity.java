package org.janelia.it.jacs.compute.util;

public class ScalityEntity {
    
    private String store;
    private Long id;
    private String name;
    private String filepath;
    
    public ScalityEntity(String store, Long id, String name, String filepath) {
        this.store = store;
        this.id = id;
        this.name = name;
        this.filepath = filepath;
    }
    
    public String getStore() {
        return store;
    }

    public Long getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getFilepath() {
        return filepath;
    }
}