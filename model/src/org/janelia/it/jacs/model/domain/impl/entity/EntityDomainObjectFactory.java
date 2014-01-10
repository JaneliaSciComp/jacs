package org.janelia.it.jacs.model.domain.impl.entity;

import org.apache.log4j.Logger;
import org.hibernate.Hibernate;
import org.janelia.it.jacs.model.domain.impl.entity.common.EntityFolder;
import org.janelia.it.jacs.model.domain.impl.entity.imaging.EntityNeuron;
import org.janelia.it.jacs.model.domain.impl.entity.imaging.EntitySample;
import org.janelia.it.jacs.model.domain.impl.entity.metamodel.EntityDomainObject;
import org.janelia.it.jacs.model.domain.impl.entity.metamodel.EntityPermission;
import org.janelia.it.jacs.model.domain.impl.entity.metamodel.EntityRelationship;
import org.janelia.it.jacs.model.domain.impl.metamodel.EntityDomainObjectStub;
import org.janelia.it.jacs.model.domain.interfaces.DomainObjectFactory;
import org.janelia.it.jacs.model.domain.interfaces.metamodel.DomainObject;
import org.janelia.it.jacs.model.domain.interfaces.metamodel.Permission;
import org.janelia.it.jacs.model.domain.interfaces.metamodel.Relationship;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityActorPermission;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Factory for instantiating domain model objects. Objects in the domain model should only be instantiated through 
 * this interface.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityDomainObjectFactory implements DomainObjectFactory<Entity,EntityData,EntityActorPermission> {

    private static final Logger log = Logger.getLogger(EntityDomainObjectFactory.class);
    
    // TODO: this should be a singleton cache
    private Cache<Long,DomainObject> domainObjectCache;
    
    public EntityDomainObjectFactory() {
        this.domainObjectCache = CacheBuilder.newBuilder().softValues().build();
    }
    
    /**
     * Instantiate the correct subclass of DomainObject for the given entity or entity tree.
     */
    public synchronized DomainObject getDomainObject(Entity entity) {
     
        DomainObject domainObj = getDomainObjectFromEntity(entity);
        
        if (domainObj!=null) {
            domainObj.setRelationshipsAreInitialized(true);

            for(EntityData ed : entity.getEntityData()) {
                Entity child = ed.getChildEntity();
                if (child!=null) {
                    DomainObject targetObj = domainObjectCache.getIfPresent(child.getId());
                    
                    if (targetObj==null && !Hibernate.isInitialized(child)) {
                        domainObj.setRelationshipsAreInitialized(false);
                        targetObj = new EntityDomainObjectStub(child.getId());
                    }
                    else {
                        // TODO: update cached instance instead of replacing it
                        targetObj = getDomainObject(child);  
                    }
                    
                    Relationship rel = new EntityRelationship(ed, domainObj, targetObj);
                    domainObj.getRelationships().add(rel);
                }   
            }
            
            domainObjectCache.put(domainObj.getGuid(), domainObj);
            
            boolean addedOwner = false;
            
            if (Hibernate.isInitialized(entity.getEntityActorPermissions())) {
                for(EntityActorPermission eap : entity.getEntityActorPermissions()) {
                    if (eap.getSubjectKey().equals(domainObj.getOwnerKey()) && eap.getPermissions().contains("o")) {
                        addedOwner = true;
                    }
                    domainObj.getPermissions().add(getPermission(eap));
                }
            }
            
            if (!addedOwner) {
                Permission ownerPermission = new EntityPermission(domainObj.getOwnerKey(), true, true, true);
                domainObj.getPermissions().add(ownerPermission);
            }
        }
        
        return domainObj;
    }

    /**
     * Instantiate the source DomainObject and return the given Relationship. 
     * @param entityData
     * @return
     */
    public Relationship getRelationship(EntityData entityData) {
     
        if (entityData.getChildEntity()==null) {
            throw new IllegalArgumentException("EntityData has no child entity");
        }
        
        DomainObject parent = getDomainObject(entityData.getParentEntity());
        
        for (Relationship rel : parent.getRelationships()) {
            if (rel.getGuid().equals(entityData.getId())) {
                return rel;
            }
        }
        
        throw new IllegalStateException("Could not instantiate relationship");
    }
    
    /**
     * Instantiate a Permission. 
     * @param eap
     * @return
     */
    public Permission getPermission(EntityActorPermission eap) {
        return new EntityPermission(eap);
    }
    
    private DomainObject getDomainObjectFromEntity(Entity entity) {
        String type = entity.getEntityTypeName();
        if (EntityConstants.TYPE_FOLDER.equalsIgnoreCase(type)) {
            return new EntityFolder(entity);
        }
        else if (EntityConstants.TYPE_SAMPLE.equalsIgnoreCase(type)) {
            return new EntitySample(entity);
        }
        else if (EntityConstants.TYPE_NEURON_FRAGMENT.equalsIgnoreCase(type)) {
            return new EntityNeuron(entity);
        }
        
        log.warn("No EntityDomainObject subclass defined for entity type "+type);
        return new EntityDomainObject(entity);
    }
}
