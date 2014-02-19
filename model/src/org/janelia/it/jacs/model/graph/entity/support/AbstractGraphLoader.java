package org.janelia.it.jacs.model.graph.entity.support;


public abstract class AbstractGraphLoader implements GraphLoader {

    private String subjectKey;
    
    public AbstractGraphLoader(String subjectKey) {
        this.subjectKey = subjectKey;
    }
    
    
}
