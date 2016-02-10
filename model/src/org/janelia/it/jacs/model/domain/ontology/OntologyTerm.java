package org.janelia.it.jacs.model.domain.ontology;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.model.domain.interfaces.HasIdentifier;
import org.jongo.marshall.jackson.oid.MongoId;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "class")
public abstract class OntologyTerm implements HasIdentifier {
    
    @MongoId
    private Long id;
    private String name;
    private List<OntologyTerm> terms;
    private transient OntologyTerm parent;

    @JsonIgnore
    public OntologyTerm getParent() {
        return parent;
    }

    @JsonIgnore
    void setParent(OntologyTerm parent) {
        this.parent = parent;
    }

    @JsonIgnore
    public Ontology getOntology() {
        OntologyTerm curr = this;
        while(curr!=null) {
            if (curr instanceof Ontology) {
                return (Ontology)curr;
            }
            curr = curr.getParent();
        }
        return null;
    }
    
    @JsonIgnore
    public boolean hasChildren() {
        return terms!=null && !terms.isEmpty();
    }

    @JsonIgnore
    public int getNumChildren() {
        return terms==null ? 0 : terms.size();
    }

    @JsonIgnore
    public void addChild(OntologyTerm term) {
        if (terms==null) {
            this.terms = new ArrayList<>();
        }
        terms.add(term);
    }

    @JsonIgnore
    public void insertChild(int index, OntologyTerm term) {
        if (terms==null) {
            this.terms = new ArrayList<>();
        }
        terms.add(index, term);
    }

    @JsonIgnore
    public void removeChild(OntologyTerm ref) {
        if (terms==null) {
            return;
        }
        terms.remove(ref);
    }

    @JsonIgnore
    public abstract boolean allowsChildren();

    @JsonIgnore
    public abstract String getTypeName();

    @JsonProperty
    public List<OntologyTerm> getTerms() {
        return terms;
    }

    @JsonProperty
    public void setTerms(List<OntologyTerm> terms) {
        if (terms!=null) {
            for (OntologyTerm term : terms) {
                term.setParent(this);
            }
        }
        this.terms = terms;
    }

    /* EVERYTHING BELOW IS AUTO-GENERATED */
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
    
    @Override
    public String toString() {
        return name;
    }
}
