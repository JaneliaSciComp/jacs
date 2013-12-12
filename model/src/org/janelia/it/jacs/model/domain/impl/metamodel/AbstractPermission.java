package org.janelia.it.jacs.model.domain.impl.metamodel;

import org.janelia.it.jacs.model.domain.interfaces.metamodel.Permission;

public abstract class AbstractPermission implements Permission {

    private String subjectKey;
    
    @Override
    public String getSubjectKey() {
        return subjectKey;
    }
    @Override
    public void setSubjectKey(String subjectKey) {
        this.subjectKey = subjectKey;
    }
}
