package org.janelia.it.jacs.model.domain.ontology;

public class EnumText extends OntologyTerm {

    private Long valueEnumId;

    public EnumText() {
    }

    public EnumText(Long valueEnumId) {
        this.valueEnumId = valueEnumId;
    }

    public boolean allowsChildren() {
        return true;
    }

    public String getTypeName() {
        return "Enumerated Text";
    }

    /* EVERYTHING BELOW IS AUTO-GENERATED */
    public Long getValueEnumId() {
        return valueEnumId;
    }

    public void setValueEnumId(Long valueEnumId) {
        this.valueEnumId = valueEnumId;
    }
}
