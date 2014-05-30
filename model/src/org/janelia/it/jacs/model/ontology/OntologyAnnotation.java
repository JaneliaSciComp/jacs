package org.janelia.it.jacs.model.ontology;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

import javax.xml.bind.annotation.*;
import java.io.Serializable;

/**
 * Annotation of an Entity with an Ontology term and possible value.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class OntologyAnnotation implements Serializable {

	@XmlAttribute
	private Long id;
	
	@XmlElement
	private Long sessionId;

	@XmlElement
	private Long targetEntityId;
	
	@XmlElement
	private Long keyEntityId;
	
	@XmlElement
	private String keyString;
	
	@XmlElement
	private Long valueEntityId; 
	
	@XmlElement
	private String valueString;

    @XmlElement
    private Boolean isComputational;
    
	@XmlTransient
	private Entity entity;

	public OntologyAnnotation() {		 
	}
			 
    public OntologyAnnotation(Long sessionId, Long targetEntityId, Long keyEntityId, String keyString,
			Long valueEntityId, String valueString) {
		super();
		this.sessionId = sessionId;
		this.targetEntityId = targetEntityId;
		this.keyEntityId = keyEntityId;
		this.keyString = keyString;
		this.valueEntityId = valueEntityId;
		this.valueString = valueString;
	}

	public void init(Entity annotation) {
        
		this.entity = annotation;
		this.id = annotation.getId();
		this.keyString = annotation.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_KEY_TERM);
        this.valueString = annotation.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_VALUE_TERM);
		
		String sessionId = annotation.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_SESSION_ID);
		if (!isEmpty(sessionId)) {
        	this.sessionId = Long.parseLong(sessionId);	
        }
        
		String targetEntityId = annotation.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_TARGET_ID);
		if (!isEmpty(targetEntityId)) {
        	this.targetEntityId = Long.parseLong(targetEntityId);	
        }
		else {
			Entity targetEntity = annotation.getChildByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_TARGET_ID);
			if (targetEntity!=null) this.targetEntityId = targetEntity.getId();
		}
		
		String keyEntityId = annotation.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_KEY_ENTITY_ID);
		if (!isEmpty(keyEntityId)) {
        	this.keyEntityId = Long.parseLong(keyEntityId);	
        }
		else {
			Entity keyEntity = annotation.getChildByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_KEY_ENTITY_ID);
			if (keyEntity!=null) this.keyEntityId = keyEntity.getId();
		}
		
		String valueEntityId = annotation.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_VALUE_ENTITY_ID);
		if (!isEmpty(valueEntityId)) {
        	this.valueEntityId = Long.parseLong(valueEntityId);	
        }
		else {
			Entity valueEntity = annotation.getChildByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_VALUE_ENTITY_ID);
			if (valueEntity!=null) this.valueEntityId = valueEntity.getId();
		}
		
        String isComputationalStr = annotation.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_IS_COMPUTATIONAL);
        if (!isEmpty(isComputationalStr)) {
            this.isComputational = Boolean.TRUE;
        }
    }

	private boolean isEmpty(String s) {
		return (s == null || "".equals(s));
	}
	
	public String getOwner() {
		if (entity==null) return null;
		return entity.getOwnerKey();
	}
	
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Long getTargetEntityId() {
        return targetEntityId;
    }

    public void setTargetEntityId(Long targetEntityId) {
        this.targetEntityId = targetEntityId;
    }

    public Long getKeyEntityId() {
        return keyEntityId;
    }

    public void setKeyEntityId(Long keyEntityId) {
        this.keyEntityId = keyEntityId;
    }

    public String getKeyString() {
        return keyString;
    }

    public void setKeyString(String keyString) {
        this.keyString = keyString;
    }

    public Long getValueEntityId() {
        return valueEntityId;
    }

    public void setValueEntityId(Long valueEntityId) {
        this.valueEntityId = valueEntityId;
    }

    public String getValueString() {
        return valueString;
    }

    public void setValueString(String valueString) {
        this.valueString = valueString;
    }

    public Boolean getIsComputational() {
        return isComputational;
    }

    public void setIsComputational(Boolean isComputational) {
        this.isComputational = isComputational;
    }

    public Entity getEntity() {
        return entity;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }
    
    public boolean isComputation() {
        return isComputational==null?false:isComputational;
    }

    @Override
	public String toString() {
        if (entity!=null) {
            return entity.getName();
        }
        else {
            return "UnpersistedAnnotation["+keyString+"="+valueString+"]";
        }
	}
}
