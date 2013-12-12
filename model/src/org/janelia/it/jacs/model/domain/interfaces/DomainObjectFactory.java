package org.janelia.it.jacs.model.domain.interfaces;

import org.janelia.it.jacs.model.domain.interfaces.metamodel.DomainObject;

public interface DomainObjectFactory<T> {

    public DomainObject createDomainObject(T sourceObject);
    
}
