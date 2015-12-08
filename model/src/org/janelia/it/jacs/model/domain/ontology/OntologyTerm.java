package org.janelia.it.jacs.model.domain.ontology;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.model.domain.interfaces.HasIdentifier;
import org.jongo.marshall.jackson.oid.MongoId;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "class")
public abstract class OntologyTerm implements HasIdentifier {
    
    @MongoId
    private Long id;
    private String name;
    private List<OntologyTerm> terms;

    @JsonIgnore
    public boolean hasChildren() {
        return terms!=null && !terms.isEmpty();
    }

    @JsonIgnore
    public int getNumChildren() {
        return terms==null ? 0 : terms.size();
    }

    public void addChild(OntologyTerm term) {
        if (terms==null) {
            this.terms = new ArrayList<>();
        }
        terms.add(term);
    }

    public void insertChild(int index, OntologyTerm term) {
        if (terms==null) {
            this.terms = new ArrayList<>();
        }
        terms.add(index, term);
    }

    public void removeChild(OntologyTerm ref) {
        if (terms==null) {
            return;
        }
        terms.remove(ref);
    }
    
    public abstract boolean allowsChildren();

    @JsonIgnore
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
