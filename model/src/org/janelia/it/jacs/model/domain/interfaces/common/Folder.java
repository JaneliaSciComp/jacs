package org.janelia.it.jacs.model.domain.interfaces.common;

import org.janelia.it.jacs.model.domain.interfaces.metamodel.DomainObject;

public interface Folder extends DomainObject {
    
    public boolean isRoot();
}
