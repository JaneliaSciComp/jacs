package org.janelia.it.jacs.shared.utils;

import java.util.List;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Preference;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ontology.OntologyTerm;

/**
 * Created by schauderd on 8/24/15.
 */
public class DomainQuery {

    private String subjectKey;
    private List<Reference> references;
    private List<Long> objectIds;
    private List<Integer> ordering;
    private String objectType;
    private String propertyName;
    private String propertyValue;
    private DomainObject domainObject;
    private List<OntologyTerm> objectList;
    private Preference preference;

    public List<Reference> getReferences() {
        return references;
    }

    public void setReferences(List<Reference> references) {
        this.references = references;
    }

    public String getSubjectKey() {
        return subjectKey;
    }

    public void setSubjectKey(String subjectKey) {
        this.subjectKey = subjectKey;
    }


    public List<Long> getObjectIds() {
        return objectIds;
    }

    public void setObjectIds(List<Long> objectIds) {
        this.objectIds = objectIds;
    }


    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }


    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getPropertyValue() {
        return propertyValue;
    }

    public void setPropertyValue(String propertyValue) {
        this.propertyValue = propertyValue;
    }

    public DomainObject getDomainObject() {
        return domainObject;
    }

    public void setDomainObject(DomainObject domainObject) {
        this.domainObject = domainObject;
    }


    public List<Integer> getOrdering() {
        return ordering;
    }

    public void setOrdering(List<Integer> ordering) {
        this.ordering = ordering;
    }

    public List<OntologyTerm> getObjectList() {
        return objectList;
    }

    public void setObjectList(List<OntologyTerm> objectList) {
        this.objectList = objectList;
    }

    public Preference getPreference() {
        return preference;
    }

    public void setPreference(Preference preference) {
        this.preference = preference;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("DomainQuery [subjectKey=");
        sb.append(subjectKey);
        if (references!=null) {
            sb.append(", references=").append(references);
        }
        if (objectIds!=null) {
            sb.append(", objectIds=").append(objectIds);
        }
        if (ordering!=null) {
            sb.append(", ordering=").append(ordering);
        }
        if (objectType!=null) {
            sb.append(", objectType=").append(objectType);
        }
        if (propertyName!=null) {
            sb.append(", propertyName=").append(propertyName);
        }
        if (propertyValue!=null) {
            sb.append(", propertyValue=").append(propertyValue);
        }
        if (domainObject!=null) {
            sb.append(", domainObject=").append(domainObject);
        }
        if (objectList!=null) {
            sb.append(", objectList=").append(objectList);
        }
        if (preference!=null) {
            sb.append(", preference=").append(preference);
        }
        sb.append("]");
        return sb.toString();
    }
    
}
