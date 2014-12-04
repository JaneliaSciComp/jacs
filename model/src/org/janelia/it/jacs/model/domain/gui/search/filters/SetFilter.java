package org.janelia.it.jacs.model.domain.gui.search.filters;

import org.janelia.it.jacs.model.domain.Reference;

public class SetFilter implements Filter {

    private String setName;
    private Reference setReference;

    @Override
    public String getLabel() {
        return "IN: "+setName;
    }

    /* EVERYTHING BELOW IS AUTO-GENERATED */
    public String getSetName() {
        return setName;
    }

    public void setSetName(String setName) {
        this.setName = setName;
    }

    public Reference getSetReference() {
        return setReference;
    }

    public void setSetReference(Reference setReference) {
        this.setReference = setReference;
    }
}
