package org.janelia.it.jacs.shared.utils.entity;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;

/**
 * A builder for visiting hierarchical entity trees.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityVistationBuilder {

	protected Logger logger = Logger.getLogger(EntityVistationBuilder.class);
	
	private Entity root;
	private Queue<EntityFilter> filters = new LinkedList<EntityFilter>();
	private AbstractEntityLoader loader;
	
	public EntityVistationBuilder(AbstractEntityLoader loader) {
		this.loader = loader;
	}
	
    public EntityVistationBuilder startAt(Entity entity) {
    	this.root = entity;
    	return this;
    }
    
    public void visit(EntityVisitor visitor) {
    	run(root, visitor);
    }
    
    public List<Entity> getEntities() {
    	final List<Entity> entities = new ArrayList<Entity>();
    	run(root, new EntityVisitor() {
			@Override
			public void visit(Entity entity) {
				entities.add(entity);
			}
    	});
    	return entities;
    }

    public List<EntityData> getEntityDatas() {
    	final List<EntityData> entityDatas = new ArrayList<EntityData>();
    	run(root, new EntityVisitor() {
			@Override
			public void visit(EntityData entityData) {
				entityDatas.add(entityData);
			}
    	});
    	return entityDatas;
    }
    
	public void run(Entity entity, EntityVisitor visitor) {
		
		if (entity==null) return;
		loader.populateChildren(entity);
		
		EntityFilter filter = filters.remove();

		for(EntityData ed : filter.getFilteredRelatives(entity)) {
			if (filters.isEmpty()) {
				visitor.visit(ed);
			}
			if (ed.getChildEntity()!=null) {
				if (filters.isEmpty()) {
					visitor.visit(ed.getChildEntity());
				}
				run(ed.getChildEntity(), visitor);	
			}
		}
	}
	
	public EntityVistationBuilder childrenOfType(String entityTypeName) {
		filters.add(new ChildrenOfType(entityTypeName));
		return this;
	}

	public EntityVistationBuilder childrenOfAttr(String entityAttrName) {
		filters.add(new ChildrenOfAttr(entityAttrName));
		return this;
	}
	
	public EntityVistationBuilder childrenOfName(String entityName) {
		filters.add(new ChildrenOfName(entityName));
		return this;
	}
	
	public EntityVistationBuilder childOfType(String entityTypeName) {
		filters.add(new ChildOfType(entityTypeName));
		return this;
	}

	public EntityVistationBuilder childOfAttr(String entityAttrName) {
		filters.add(new ChildOfAttr(entityAttrName));
		return this;
	}

	public EntityVistationBuilder childOfName(String entityName) {
		filters.add(new ChildOfName(entityName));
		return this;
	}
	
    private abstract class EntityFilter {
    	
    	String parameter;
    	List<EntityData> filtered = new ArrayList<EntityData>();
    	
    	public EntityFilter(String parameter) {
    		this.parameter = parameter;
    	}
    	
    	public List<EntityData> getFilteredRelatives(Entity entity) {
    		for(EntityData ed : entity.getOrderedEntityData()) {
    			if (allow(ed)) {
    				filtered.add(ed);
    			}
    		}
    		return filtered;
    	}
    
    	public abstract boolean allow(EntityData entityData);
    }

    private abstract class SingleEntityFilter extends EntityFilter {
    	
    	int count;
    	
    	public SingleEntityFilter(String parameter) {
    		super(parameter);
    	}
    	
    	public List<EntityData> getFilteredRelatives(Entity entity) {
    		for(EntityData ed : entity.getOrderedEntityData()) {
    			if (allow(ed)) {
    				count++;
    				if (count>1) {
    					logger.warn("Expected only one child of type: "+parameter);
        				return filtered;
    				}
    				else {
    					filtered.add(ed);	
    				}        			
    			}
    		}
    		return filtered;
    	}
    }
    
    private class ChildrenOfType extends EntityFilter {
    	public ChildrenOfType(String parameter) {
    		super(parameter);
    	}
    	public boolean allow(EntityData ed) {
    		return (ed.getChildEntity()!=null && ed.getChildEntity().getEntityType().getName().equals(parameter));
    	}
    }
    
    private class ChildrenOfAttr extends EntityFilter {
    	public ChildrenOfAttr(String parameter) {
    		super(parameter);
    	}
    	public boolean allow(EntityData ed) {
    		return (ed.getChildEntity()!=null && ed.getEntityAttribute().getName().equals(parameter));
    	}
    }

    private class ChildrenOfName extends EntityFilter {
    	public ChildrenOfName(String parameter) {
    		super(parameter);
    	}
    	public boolean allow(EntityData ed) {
    		return (ed.getChildEntity()!=null && ed.getChildEntity().getName().equals(parameter));
    	}
    }

    private class ChildOfType extends SingleEntityFilter {
    	public ChildOfType(String parameter) {
    		super(parameter);
    	}
    	public boolean allow(EntityData ed) {
    		return (ed.getChildEntity()!=null && ed.getChildEntity().getEntityType().getName().equals(parameter));
    	}
    }

    private class ChildOfAttr extends SingleEntityFilter {
    	public ChildOfAttr(String parameter) {
    		super(parameter);
    	}
    	public boolean allow(EntityData ed) {
    		return (ed.getChildEntity()!=null && ed.getEntityAttribute().getName().equals(parameter));
    	}
    }
    
    private class ChildOfName extends SingleEntityFilter {
    	public ChildOfName(String parameter) {
    		super(parameter);
    	}
    	public boolean allow(EntityData ed) {
    		return (ed.getChildEntity()!=null && ed.getChildEntity().getName().equals(parameter));
    	}
    }
}
