package org.janelia.it.jacs.model.domain.impl.metamodel;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.janelia.it.jacs.model.domain.interfaces.metamodel.DomainObject;
import org.janelia.it.jacs.model.domain.interfaces.metamodel.Permission;
import org.janelia.it.jacs.model.domain.interfaces.metamodel.Relationship;
import org.janelia.it.jacs.model.domain.interfaces.metamodel.RelationshipLoader;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class AbstractDomainObject implements DomainObject {

    private Long guid;
    private String name;
    private String ownerKey;
    private String entityTypeName;
    private Date creationDate;
    private Date updatedDate;
    private Multimap<String,String> attributes = HashMultimap.<String,String>create();
    private Set<Permission> permissions = new HashSet<Permission>();
    private Set<Relationship> relationships = new HashSet<Relationship>();
    private boolean relationshipsAreInitialized = false;
    
    @Override
    public Long getGuid() {
        return guid;
    }
    @Override
    public void setGuid(Long guid) {
        this.guid = guid;
    }
    @Override
    public String getName() {
        return name;
    }
    @Override
    public void setName(String name) {
        this.name = name;
    }
    @Override
    public String getOwnerKey() {
        return ownerKey;
    }
    @Override
    public void setOwnerKey(String ownerKey) {
        this.ownerKey = ownerKey;
    }
    @Override
    public String getEntityTypeName() {
        return entityTypeName;
    }
    @Override
    public void setEntityTypeName(String entityTypeName) {
        this.entityTypeName = entityTypeName;
    }
    @Override
    public Date getCreationDate() {
        return creationDate;
    }
    @Override
    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
    @Override
    public Date getUpdatedDate() {
        return updatedDate;
    }
    @Override
    public void setUpdatedDate(Date updatedDate) {
        this.updatedDate = updatedDate;
    }
    @Override
    public Multimap<String, String> getAttributes() {
        return attributes;
    }
    @Override
    public Set<Permission> getPermissions() {
        return permissions;
    }
    @Override
    public Set<Relationship> getRelationships() {
        return relationships;
    }
    @Override
    public int getNumRelationships() {
        return relationships.size();
    }
    @Override
    public boolean relationshipsAreInitialized() {
        return relationshipsAreInitialized;
    }
    @Override
    public void setRelationshipsAreInitialized(boolean relationshipsAreInitialized) {
        this.relationshipsAreInitialized = relationshipsAreInitialized;
    }
    @Override
    public String getAttributeValue(String key) {
        Collection<String> values = getAttributes().get(key);
        if (values==null) return null;
        if (values.size()>1) throw new IllegalArgumentException("Attribute is multivalued: "+key);
        return values.iterator().next();
    }
    @Override
    public Collection<String> getAttributeValues(String key) {
        return getAttributes().get(key);   
    }
    @Override
    public void loadRelationships(RelationshipLoader loader) {
        loader.loadRelationships(this);
    }
}
