package org.janelia.it.jacs.model.domain.ontology;

public class Text extends OntologyTerm {

    public boolean allowsChildren() {
        return true;
    }

    public String getTypeName() {
        return "Text";
    }
}
