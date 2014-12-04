package org.janelia.it.jacs.model.domain;

import java.util.Date;
import java.util.Set;

import org.janelia.it.jacs.model.domain.support.SearchAttribute;
import org.jongo.marshall.jackson.oid.Id;

/**
 * Every top-level "domain object" we store in MongoDB has a core set of attributes
 * which allow for identification (id/name) and permissions (owner/readers/writers)
 * as well as safe-updates with updatedDate.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class AbstractDomainObject implements DomainObject {

    @Id
    @SearchAttribute(key="id",label="GUID")
    private Long id;
    
    @SearchAttribute(key="name",label="Name")
    private String name;
    
    @SearchAttribute(key="username",label="Owner",facet=true)
    private String ownerKey;

    @SearchAttribute(key="subjects",label="Subjects")
    private Set<String> readers;
    
    private Set<String> writers;

    @SearchAttribute(key="creation_date",label="Creation Date")
    private Date creationDate;

    @SearchAttribute(key="updated_date",label="Updated Date")
    private Date updatedDate;

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
