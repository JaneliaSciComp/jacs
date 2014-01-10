package org.janelia.it.jacs.model.domain.impl.metamodel;

import org.janelia.it.jacs.model.domain.interfaces.metamodel.Permission;

public abstract class AbstractPermission implements Permission {

    private Long guid;
    private String subjectKey;

    @Override
    public Long getGuid() {
        return guid;
    }
    @Override
    public void setGuid(Long guid) {
        this.guid = guid;
    }
    @Override
    public String getSubjectKey() {
        return subjectKey;
    }
    @Override
    public void setSubjectKey(String subjectKey) {
        this.subjectKey = subjectKey;
    }
}
