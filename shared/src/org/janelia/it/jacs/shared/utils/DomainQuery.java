package org.janelia.it.jacs.shared.utils;

import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.DomainObject;
import java.util.List;

/**
 * Created by schauderd on 8/24/15.
 */
public class DomainQuery {
    String subjectKey;
    List<Reference> references;
    List<Long> objectIds;
    String objectType;
    String propertyName;
    String propertyValue;
    DomainObject domainObject;

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
}
