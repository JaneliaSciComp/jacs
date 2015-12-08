package org.janelia.it.jacs.shared.utils;

import org.janelia.it.jacs.model.domain.Preference;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.interfaces.HasIdentifier;
import org.janelia.it.jacs.model.domain.ontology.OntologyTerm;

import java.util.List;

/**
 * Created by schauderd on 8/24/15.
 */
public class DomainQuery {
    String subjectKey;
    List<Reference> references;
    List<Long> objectIds;
    List<Integer> ordering;
    String objectType;
    String propertyName;
    String propertyValue;
    DomainObject domainObject;
    List<OntologyTerm> objectList;
    Preference preference;

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
}
