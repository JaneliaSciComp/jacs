package org.janelia.it.jacs.model.domain.impl.metamodel;

import java.util.Date;

import org.janelia.it.jacs.model.domain.interfaces.metamodel.DomainObject;
import org.janelia.it.jacs.model.domain.interfaces.metamodel.Relationship;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class AbstractRelationship implements Relationship {
    
    protected Long guid;
    protected String type;
    protected Integer orderIndex;
    protected Date creationDate;
    protected Date updatedDate;
    protected Multimap<String,String> attributes = HashMultimap.<String,String>create();
    protected DomainObject source;
    protected DomainObject target;
    
    @Override
    public Long getGuid() {
        return guid;
    }
    @Override
    public void setGuid(Long guid) {
        this.guid = guid;
    }
    @Override
    public String getType() {
        return type;
    }
    @Override
    public void setType(String type) {
        this.type = type;
    }
    @Override
    public Integer getOrderIndex() {
        return orderIndex;
    }
    @Override
    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }
    @Override
    public Date getCreationDate() {
        return creationDate;
    }
    @Override
    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
    @Override
    public Date getUpdatedDate() {
        return updatedDate;
    }
    @Override
    public void setUpdatedDate(Date updatedDate) {
        this.updatedDate = updatedDate;
    }
    @Override
    public Multimap<String, String> getAttributes() {
        return attributes;
    }
    @Override
    public void setAttributes(Multimap<String, String> attributes) {
        this.attributes = attributes;
    }
    @Override
    public DomainObject getSource() {
        return source;
    }
    @Override
    public void setSource(DomainObject source) {
        this.source = source;
    }
    @Override
    public DomainObject getTarget() {
        return target;
    }
    @Override
    public void setTarget(DomainObject target) {
        this.target = target;
    }
}
