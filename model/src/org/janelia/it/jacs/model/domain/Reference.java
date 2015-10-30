package org.janelia.it.jacs.model.domain;

/**
 * A reference to a DomainObject in a specific collection.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Reference {

    private String collectionName;
    private Long id;

    /* EVERYTHING BELOW IS AUTO-GENERATED */
    public Reference() {
    }

    public Reference(String collectionName, Long id) {
        this.collectionName = collectionName;
        this.id = id;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String type) {
        this.collectionName = type;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime*result
                +((id==null) ? 0 : id.hashCode());
        result = prime*result
                +((collectionName==null) ? 0 : collectionName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this==obj) {
            return true;
        }
        if (obj==null) {
            return false;
        }
        if (getClass()!=obj.getClass()) {
            return false;
        }
        Reference other = (Reference) obj;
        if (id==null) {
            if (other.id!=null) {
                return false;
            }
        }
        else if (!id.equals(other.id)) {
            return false;
        }
        if (collectionName==null) {
            if (other.collectionName!=null) {
                return false;
            }
        }
        else if (!collectionName.equals(other.collectionName)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Reference[" + collectionName + "#" + id + "]";
    }
}
