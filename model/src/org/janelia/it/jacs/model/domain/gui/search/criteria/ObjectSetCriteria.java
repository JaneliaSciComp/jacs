package org.janelia.it.jacs.model.domain.gui.search.criteria;

import org.janelia.it.jacs.model.domain.Reference;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

public class ObjectSetCriteria extends Criteria {

    private String objectSetName;
    @JsonUnwrapped
    private Reference objectSetReference;

    public String getObjectSetName() {
        return objectSetName;
    }

    public void setObjectSetName(String setName) {
        this.objectSetName = setName;
    }

    public Reference getObjectSetReference() {
        return objectSetReference;
    }

    public void setObjectSetReference(Reference objectSetReference) {
        this.objectSetReference = objectSetReference;
    }
}
