package org.janelia.it.jacs.model.domain;

import java.util.Date;
import java.util.Set;

import org.janelia.it.jacs.model.domain.interfaces.HasIdentifier;

public interface DomainObject extends HasIdentifier {

    public Long getId();

    public void setId(Long id);

    public String getName();

    public void setName(String name);
    
    public abstract String getOwnerKey();

    public abstract void setOwnerKey(String ownerKey);

    public abstract Set<String> getReaders();

    public abstract void setReaders(Set<String> readers);

    public abstract Set<String> getWriters();

    public abstract void setWriters(Set<String> writers);

    public Date getCreationDate();

    public void setCreationDate(Date creationDate);

    public Date getUpdatedDate();

    public void setUpdatedDate(Date updatedDate);
}
