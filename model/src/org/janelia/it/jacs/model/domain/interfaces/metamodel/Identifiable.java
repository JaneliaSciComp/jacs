package org.janelia.it.jacs.model.domain.interfaces.metamodel;

import java.io.Serializable;

public interface Identifiable extends Serializable {

    public Long getGuid();

    public void setGuid(Long guid);
}
