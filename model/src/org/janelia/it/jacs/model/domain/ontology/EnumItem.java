package org.janelia.it.jacs.model.domain.ontology;

public class EnumItem extends OntologyTerm {

    public boolean allowsChildren() {
        return false;
    }

    public String getTypeName() {
        return "Item";
    }
}
