package org.janelia.it.jacs.model.domain.interfaces.metamodel;

import java.util.Collection;
import java.util.Date;
import java.util.Set;

import com.google.common.collect.Multimap;

public interface DomainObject {

    public Long getGuid();

    public void setGuid(Long guid);

    public String getName();

    public void setName(String name);

    public String getOwnerKey();

    public void setOwnerKey(String ownerKey);

    public String getEntityTypeName();

    public void setEntityTypeName(String entityTypeName);

    public Date getCreationDate();

    public void setCreationDate(Date creationDate);

    public Date getUpdatedDate();

    public void setUpdatedDate(Date updatedDate);

    public Multimap<String, String> getAttributes();

    public String getAttributeValue(String key);

    public Collection<String> getAttributeValues(String key);
    
    public Set<Permission> getPermissions();
    
    public Set<Relationship> getRelationships();

    public int getNumRelationships();
    
    public boolean relationshipsAreInitialized();
    
    public void loadRelationships(RelationshipLoader loader);
}