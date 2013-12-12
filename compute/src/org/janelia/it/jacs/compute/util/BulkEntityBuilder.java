package org.janelia.it.jacs.compute.util;

import java.util.Stack;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.entity.*;

/**
 * A concise-syntax builder for entity trees for loading with the bulk loader. See LocalTestManager for an example of usage.
 * 
 * @see org.janelia.it.jacs.compute.mbean.LocalTestManager
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class BulkEntityBuilder {

	private static final Logger logger = Logger.getLogger(BulkEntityBuilder.class);
	
	private String ownerKey;
	private Stack<Entity> ancestry = new Stack<Entity>();
	
	public BulkEntityBuilder(String ownerKey) {
		this.ownerKey = ownerKey;
	}
	
    public BulkEntityBuilder entity(String entityTypeName, String name) {
    	
    	logger.debug(getIndent()+"adding entity '"+name+"' ("+entityTypeName+")");
    	
    	Entity entity = new Entity();
    	entity.setEntityTypeName(entityTypeName);
    	entity.setName(name);
    	
    	if (ancestry.isEmpty()) {
    		// Only root gets an owner, to save on object creation
    		entity.setOwnerKey(ownerKey);
    	}
    	else {
    		Entity current = ancestry.peek();
    		EntityData ed = new EntityData();
        	ed.setEntityAttrName(EntityConstants.ATTRIBUTE_ENTITY);
        	ed.setChildEntity(entity);
        	ed.setOrderIndex(current.getMaxOrderIndex()+1);
        	current.getEntityData().add(ed);
    	}
    	
    	ancestry.push(entity);
    	
    	return this;
    }

    public BulkEntityBuilder attr(String entityAttrName, String value) {
    	if (ancestry.isEmpty()) throw new IllegalStateException("No entity to attach an attribute to");

		Entity current = ancestry.peek();
		
    	logger.debug(getIndent()+"adding attribute '"+value+"' ("+entityAttrName+")");
    	
    	EntityData ed = new EntityData();
    	ed.setEntityAttrName(entityAttrName);
    	ed.setValue(value);
    	current.getEntityData().add(ed);
    	
    	return this;
    }

    public BulkEntityBuilder up() {
    	ancestry.pop();
    	return this;
    }
    
    public Entity get() {
    	return ancestry.peek();
    }
    
    public Entity getRoot() {
    	return ancestry.get(0);
    }
    
    private String getIndent() {
    	StringBuffer sb = new StringBuffer();
    	for(Entity entity : ancestry) {
    		sb.append("  ");
    	}
    	return sb.toString();
    }
}
