package org.janelia.it.jacs.compute.api.support;

/**
 * Object graph access parameters when calling remote bean methods.  
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Access {

    public enum AccessPattern {
        OWNED_OBJECTS_ONLY,
        ALL_ACCESSIBLE_OBJECTS
    }
    
    private String subjectKey;
    private AccessPattern accessPattern;
    
    public Access(String subjectKey, AccessPattern accessPattern) {
        this.subjectKey = subjectKey;
        this.accessPattern = accessPattern;
    }

    public String getSubjectKey() {
        return subjectKey;
    }

    public AccessPattern getAccessPattern() {
        return accessPattern;
    }
}
