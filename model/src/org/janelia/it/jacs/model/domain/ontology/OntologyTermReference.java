package org.janelia.it.jacs.model.domain.ontology;

public class OntologyTermReference {

    private Long ontologyId;
    private Long ontologyTermId;

    public OntologyTermReference() {
    }
    
    public OntologyTermReference(Long ontologyId, Long ontologyTermId) {
        this.ontologyId = ontologyId;
        this.ontologyTermId = ontologyTermId;
    }
    
    public OntologyTermReference(Ontology ontology, OntologyTerm term) {
        this.ontologyId = ontology.getId();
        this.ontologyTermId = term.getId();
    }

    /* EVERYTHING BELOW IS AUTO-GENERATED */
    public Long getOntologyId() {
        return ontologyId;
    }

    public void setOntologyId(Long ontologyId) {
        this.ontologyId = ontologyId;
    }

    public Long getOntologyTermId() {
        return ontologyTermId;
    }

    public void setOntologyTermId(Long ontologyTermId) {
        this.ontologyTermId = ontologyTermId;
    }
}
