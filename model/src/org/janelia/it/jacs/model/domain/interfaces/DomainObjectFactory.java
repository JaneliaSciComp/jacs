package org.janelia.it.jacs.model.domain.interfaces;

import org.janelia.it.jacs.model.domain.interfaces.metamodel.DomainObject;
import org.janelia.it.jacs.model.domain.interfaces.metamodel.Permission;
import org.janelia.it.jacs.model.domain.interfaces.metamodel.Relationship;

/**
 * A factory for creating DomainObjects, Relationships, and Permissions. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface DomainObjectFactory<T,U,V> {

    public DomainObject getDomainObject(T sourceObject);
    
    public Relationship getRelationship(U sourceObject);

    public Permission getPermission(V sourceObject);
}
