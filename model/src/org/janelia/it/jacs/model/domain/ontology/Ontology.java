package org.janelia.it.jacs.model.domain.ontology;

import java.util.Date;
import java.util.Set;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.support.MongoMapped;
import com.fasterxml.jackson.annotation.JsonIgnore;

@MongoMapped(collectionName="ontology",label="Ontology")
public class Ontology extends OntologyTerm implements DomainObject {

    private String ownerKey;
    private Set<String> readers;
    private Set<String> writers;
    private Date creationDate;
    private Date updatedDate;

    @Override
    public boolean allowsChildren() {
        return true;
    }

    @Override
    @JsonIgnore
    public String getTypeName() {
        return "Ontology";
    }

    @Override
    @JsonIgnore
    public String getType() {
        return "Ontology"; // this must match the MongoMapped.label above
    }
    
    /* EVERYTHING BELOW IS AUTO-GENERATED */
    public String getOwnerKey() {
        return ownerKey;
    }

    public void setOwnerKey(String ownerKey) {
        this.ownerKey = ownerKey;
    }

    public Set<String> getReaders() {
        return readers;
    }

    public void setReaders(Set<String> readers) {
        this.readers = readers;
    }

    public Set<String> getWriters() {
        return writers;
    }

    public void setWriters(Set<String> writers) {
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
