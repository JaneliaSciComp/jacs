package org.janelia.it.jacs.model.domain.ontology;

import java.util.Date;
import java.util.List;

import org.janelia.it.jacs.model.domain.DomainObject;

public class Ontology extends OntologyTerm implements DomainObject {

    private String ownerKey;
    private List<String> readers;
    private List<String> writers;
    private Date creationDate;
    private Date updatedDate;

    @Override
    public boolean allowsChildren() {
        return true;
    }
    @Override
    public String getTypeName() {
        return "Ontology";
    }
    
    /* EVERYTHING BELOW IS AUTO-GENERATED */
    
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
    public Date getUpdatedDate() {
        return updatedDate;
    }
    public void setUpdatedDate(Date updatedDate) {
        this.updatedDate = updatedDate;
    }
}
