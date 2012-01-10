package org.janelia.it.jacs.model.entity;
// Generated May 26, 2011 10:42:23 AM by Hibernate Tools 3.2.1.GA


import com.google.gwt.user.client.rpc.IsSerializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlValue;
import java.util.*;

/**
 * EntityTypeGenomic generated by hbm2java
 */
@XmlAccessorType(XmlAccessType.NONE)
public class EntityType  implements java.io.Serializable, IsSerializable {
	
    private Long id;
    private Long sequence;
    @XmlValue
    private String name;
    private String style;
    private String description;
    private String iconurl;
    private Set<EntityAttribute> attributes;

    public EntityType() {
    }

    public EntityType(Long id) {
        this.id = id;
    }
    
    public EntityType(Long id, Long sequence, String name, String style, String description, String iconurl,
                      HashSet<EntityAttribute> attributes) {
       this.id = id;
       this.sequence = sequence;
       this.name = name;
       this.style = style;
       this.description = description;
       this.iconurl = iconurl;
       this.attributes = attributes;
    }
   
    public Long getId() {
        return this.id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    public Long getSequence() {
        return this.sequence;
    }
    
    public void setSequence(Long sequence) {
        this.sequence = sequence;
    }
    public String getName() {
        return this.name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    public String getStyle() {
        return this.style;
    }
    
    public void setStyle(String style) {
        this.style = style;
    }
    public String getDescription() {
        return this.description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    public String getIconurl() {
        return this.iconurl;
    }
    
    public void setIconurl(String iconurl) {
        this.iconurl = iconurl;
    }

    public Set<EntityAttribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(Set<EntityAttribute> attributes) {
        this.attributes = attributes;
    }
    
    public List<EntityAttribute> getOrderedAttributes() {
    	List<EntityAttribute> attrs = new ArrayList<EntityAttribute>(getAttributes());
        Collections.sort(attrs, new Comparator<EntityAttribute>() {
			@Override
			public int compare(EntityAttribute o1, EntityAttribute o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
        return attrs;
    }
}


