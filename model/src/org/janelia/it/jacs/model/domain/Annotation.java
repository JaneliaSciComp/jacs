package org.janelia.it.jacs.model.domain;

import java.util.Date;
import java.util.List;

import org.janelia.it.jacs.model.domain.ontology.OntologyTermReference;
import org.jongo.marshall.jackson.oid.Id;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

public class Annotation implements DomainObject {

    @Id
    private Long id;
    private String name;
    private String ownerKey;
    private List<String> readers;
    private List<String> writers;
    private Date creationDate;
    private String targetType;
    @JsonUnwrapped
    private Reference target;
    private String text;
    private OntologyTermReference keyTerm;
    private OntologyTermReference valueTerm;

    /* EVERYTHING BELOW IS AUTO-GENERATED */
    
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getOwnerKey() {
        return ownerKey;
    }
    public void setOwnerKey(String ownerKey) {
        this.ownerKey = ownerKey;
    }
    public List<String> getReaders() {
        return readers;
    }
    public void setReaders(List<String> readers) {
        this.readers = readers;
    }
    public List<String> getWriters() {
        return writers;
    }
    public void setWriters(List<String> writers) {
        this.writers = writers;
    }
    public Date getCreationDate() {
        return creationDate;
    }
    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
    public String getTargetType() {
        return targetType;
    }
    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }
    public Reference getTarget() {
        return target;
    }
    public void setTarget(Reference target) {
        this.target = target;
    }
    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }
    public OntologyTermReference getKeyTerm() {
        return keyTerm;
    }
    public void setKeyTerm(OntologyTermReference keyTerm) {
        this.keyTerm = keyTerm;
    }
    public OntologyTermReference getValueTerm() {
        return valueTerm;
    }
    public void setValueTerm(OntologyTermReference valueTerm) {
        this.valueTerm = valueTerm;
    }
    
    
}
