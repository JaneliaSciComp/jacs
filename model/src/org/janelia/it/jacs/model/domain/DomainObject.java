package org.janelia.it.jacs.model.domain;

import java.util.List;

public interface DomainObject {

    public Long getId();
    
    public void setId(Long id);
    
    public abstract String getOwnerKey();

    public abstract void setOwnerKey(String ownerKey);

    public abstract List<String> getReaders();

    public abstract void setReaders(List<String> readers);

    public abstract List<String> getWriters();

    public abstract void setWriters(List<String> writers);

}