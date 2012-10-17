package org.janelia.it.jacs.shared.utils.entity;

import java.util.ArrayList;
import java.util.List;

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
	private List<EntityFilter> filters = new ArrayList<EntityFilter>();
	private AbstractEntityLoader loader;
	
	public EntityVistationBuilder(AbstractEntityLoader loader) {
		this.loader = loader;
	}
	
    public EntityVistationBuilder startAt(Entity entity) {
    	this.root = entity;
    	return this;
    }

    public List<Entity> getEntities() {
    	final List<Entity> entities = new ArrayList<Entity>();
    	visit(new EntityVisitor() {
			@Override
			public void visit(Entity entity) {
				entities.add(entity);
			}
    	});
    	return entities;
    }

    public List<EntityData> getEntityDatas() {
    	final List<EntityData> entityDatas = new ArrayList<EntityData>();
    	visit(new EntityVisitor() {
			@Override
			public void visit(EntityData entityData) {
				entityDatas.add(entityData);
			}
    	});
    	return entityDatas;
    }
    
    public void visit(EntityVisitor visitor) {
    	run(root, visitor, 0);
    }
    
	public void run(Entity entity, EntityVisitor visitor, int level) {
		
		if (entity==null || filters.isEmpty()) return;
		loader.populateChildren(entity);
		
		EntityFilter filter = filters.get(level);
		
		for(EntityData ed : filter.getFilteredRelatives(entity)) {
			if (level>=filters.size()-1) {
				visitor.visit(ed);
			}
			if (ed.getChildEntity()!=null) {
				if (level>=filters.size()-1) {
					visitor.visit(ed.getChildEntity());
				}
				else {
					run(ed.getChildEntity(), visitor, level+1);		
				}
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

    private abstract class EntityFilter {
    	
    	String parameter;
    	
    	public EntityFilter(String parameter) {
    		this.parameter = parameter;
    	}
    	
    	public List<EntityData> getFilteredRelatives(Entity entity) {
    		List<EntityData> filtered = new ArrayList<EntityData>();
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
    	
    	int count = 0;
    	
    	public SingleEntityFilter(String parameter) {
    		super(parameter);
    	}
    	
    	public List<EntityData> getFilteredRelatives(Entity entity) {
    		List<EntityData> filtered = new ArrayList<EntityData>();
    		for(EntityData ed : entity.getOrderedEntityData()) {
    			if (allow(ed)) {
    				if (++count>1) {
    					boolean seen = false;
    					for(EntityData ed2 : filtered) {
    						if (ed2.getChildEntity().getId().equals(ed.getChildEntity().getId())) {
    							seen = true;
    						}
    					}
    					if (!seen) {
    						logger.warn("Expected only one child of type "+parameter+" for entity with id="+entity.getId());
    					}
    				}
    				else {
    					filtered.add(ed);	
    				}        			
    			}
    		}
    		return filtered;
    	}
    }
}
