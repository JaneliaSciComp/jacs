package org.janelia.it.jacs.model.domain.impl.metamodel;

import java.util.Collection;
import java.util.Date;
import java.util.Set;

import org.janelia.it.jacs.model.domain.interfaces.metamodel.DomainObject;
import org.janelia.it.jacs.model.domain.interfaces.metamodel.Permission;
import org.janelia.it.jacs.model.domain.interfaces.metamodel.Relationship;
import org.janelia.it.jacs.model.domain.interfaces.metamodel.RelationshipLoader;

import com.google.common.collect.Multimap;

/**
 * A stub specifying an entity by id, but without its metadata.
 * All methods except getGuid and setGuid throw UnsupportedOperationException.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityDomainObjectStub implements DomainObject {

    private Long guid;
    
    public EntityDomainObjectStub(Long entityId) {
        setGuid(entityId);
    }

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
        throw new UnsupportedOperationException();
    }

    @Override
    public void setName(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getOwnerKey() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setOwnerKey(String ownerKey) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getEntityTypeName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setEntityTypeName(String entityTypeName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Date getCreationDate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setCreationDate(Date creationDate) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Date getUpdatedDate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setUpdatedDate(Date updatedDate) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Multimap<String, String> getAttributes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getAttributeValue(String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<String> getAttributeValues(String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Permission> getPermissions() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Relationship> getRelationships() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getNumRelationships() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean relationshipsAreInitialized() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setRelationshipsAreInitialized(boolean relationshipsAreInitialized) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void loadRelationships(RelationshipLoader loader) {
        throw new UnsupportedOperationException();
    }
}
