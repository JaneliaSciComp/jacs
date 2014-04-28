package org.janelia.it.jacs.model.domain;

import java.util.Set;

public interface DomainObject {

    public Long getId();
    
    public void setId(Long id);
    
    public abstract String getOwnerKey();

    public abstract void setOwnerKey(String ownerKey);

    public abstract Set<String> getReaders();

    public abstract void setReaders(Set<String> readers);

    public abstract Set<String> getWriters();

    public abstract void setWriters(Set<String> writers);

}