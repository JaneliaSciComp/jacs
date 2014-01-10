package org.janelia.it.jacs.model.domain.interfaces.metamodel;

public interface Permission extends Identifiable {
    
    public String getSubjectKey();

    public void setSubjectKey(String subjectKey);

    public boolean isOwner();
    
    public boolean canRead();
    
    public boolean canWrite();
    
}