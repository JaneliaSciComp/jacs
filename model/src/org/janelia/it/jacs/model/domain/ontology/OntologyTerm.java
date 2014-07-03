package org.janelia.it.jacs.model.domain.ontology;

import java.util.List;

import org.jongo.marshall.jackson.oid.Id;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use= JsonTypeInfo.Id.CLASS,property="class")
public abstract class OntologyTerm {

    @Id
    private Long id;
    private String name;
    private List<OntologyTerm> terms;

    public abstract boolean allowsChildren();
    
    public abstract String getTypeName();
    
    /* EVERYTHING BELOW IS AUTO-GENERATED */
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<OntologyTerm> getTerms() {
        return terms;
    }

    public void setTerms(List<OntologyTerm> terms) {
        this.terms = terms;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
}
