package org.janelia.it.jacs.model.domain.interfaces.metamodel;

import java.util.Date;

import com.google.common.collect.Multimap;

public interface Relationship  extends Identifiable {

    public String getType();

    public void setType(String type);

    public Integer getOrderIndex();

    public void setOrderIndex(Integer orderIndex);

    public Date getCreationDate();

    public void setCreationDate(Date creationDate);

    public Date getUpdatedDate();

    public void setUpdatedDate(Date updatedDate);
    
    public Multimap<String, String> getAttributes();

    public void setAttributes(Multimap<String, String> attributes);

    public DomainObject getSource();
    
    public void setSource(DomainObject source);
    
    public DomainObject getTarget();
    
    public void setTarget(DomainObject target);
}