package org.janelia.it.jacs.model.domain.ontology;

import org.janelia.it.jacs.model.domain.AbstractDomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.support.MongoMapped;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

@MongoMapped(collectionName = "annotation")
public class Annotation extends AbstractDomainObject {

    @JsonUnwrapped
    private Reference target;
    private OntologyTermReference keyTerm;
    private OntologyTermReference valueTerm;
    private String key;
    private String value;

    /* EVERYTHING BELOW IS AUTO-GENERATED */

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

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
