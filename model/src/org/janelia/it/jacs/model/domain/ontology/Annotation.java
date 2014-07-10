package org.janelia.it.jacs.model.domain.ontology;

import org.janelia.it.jacs.model.domain.AbstractDomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.support.MongoMapped;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

@MongoMapped(collectionName="annotation")
public class Annotation extends AbstractDomainObject {

    private String targetType;
    @JsonUnwrapped
    private Reference target;
    private OntologyTermReference keyTerm;
    private OntologyTermReference valueTerm;

    /* EVERYTHING BELOW IS AUTO-GENERATED */
    
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
