package org.janelia.it.jacs.shared.utils.entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
	private boolean visitRootOwnerOwnedEntitiesOnly = true;
	
	public EntityVistationBuilder(AbstractEntityLoader loader) {
		this.loader = loader;
	}

    public List<Entity> getEntities() throws Exception {
    	final List<Entity> entities = new ArrayList<Entity>();
    	run(new EntityVisitor() {
			@Override
			public void visit(Entity entity) {
				entities.add(entity);
			}
    	});
    	return entities;
    }

    public List<EntityData> getEntityDatas() throws Exception {
    	final List<EntityData> entityDatas = new ArrayList<EntityData>();
    	run(new EntityVisitor() {
			@Override
			public void visit(EntityData entityData) {
				entityDatas.add(entityData);
			}
    	});
    	return entityDatas;
    }
    
    public void run(EntityVisitor visitor) throws Exception {
    	run(root, visitor, 0);
    }
    
	private void run(Entity entity, EntityVisitor visitor, int level) throws Exception {
		
		if (entity==null || filters.isEmpty()) return;
		loader.populateChildren(entity);
		
		EntityFilter filter = filters.get(level);
		
		if (filter instanceof Ancestors) {
			runRecursively(entity, visitor, new HashSet<Long>());
		}
		else if (filter instanceof Root) {
	    	visitor.visit(entity);
			for(EntityData ed : entity.getEntityData()) {
				visitor.visit(ed);
			}
		}
		else if (filter instanceof ChildEntityFilter) {
			ChildEntityFilter childFilter = (ChildEntityFilter)filter;
			for(EntityData ed : childFilter.getFilteredRelatives(entity)) {
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
		else {
			throw new IllegalStateException("Unknown filter type: "+filter.getClass().getName());
		}
	}

	public void runRecursively(Entity entity, EntityVisitor visitor, Set<Long> visited) throws Exception {

    	if (entity == null) return;
    	if (visited.contains(entity.getId())) {
    		return;
    	}
    	
    	if (visitRootOwnerOwnedEntitiesOnly && !entity.getOwnerKey().equals(root.getOwnerKey())) {
    		return;
    	}
    	
    	visited.add(entity.getId());
    	visitor.visit(entity);
    	
		for(EntityData ed : entity.getEntityData()) {
			visitor.visit(ed);
			if (ed.getChildEntity()!=null) {
				runRecursively(ed.getChildEntity(), visitor, visited);		
			}
		}
    	
	}

    public EntityVistationBuilder startAt(Entity entity) {
    	this.root = entity;
    	return this;
    }

	public EntityVistationBuilder ancestors() {
		filters.add(new Ancestors());
		return this;
	}

	public EntityVistationBuilder root() {
		filters.add(new Root());
		return this;
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

    private class Ancestors implements EntityFilter {
    }

    private class Root implements EntityFilter {
    }
    
    private class ChildrenOfType extends ChildEntityFilter {
    	public ChildrenOfType(String parameter) {
    		super(parameter);
    	}
    	public boolean allow(EntityData ed) {
    		return (ed.getChildEntity()!=null && ed.getChildEntity().getEntityType().getName().equals(parameter));
    	}
    }
    
    private class ChildrenOfAttr extends ChildEntityFilter {
    	public ChildrenOfAttr(String parameter) {
    		super(parameter);
    	}
    	public boolean allow(EntityData ed) {
    		return (ed.getChildEntity()!=null && ed.getEntityAttribute().getName().equals(parameter));
    	}
    }

    private class ChildrenOfName extends ChildEntityFilter {
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

    private interface EntityFilter {
    }
    
    private abstract class ChildEntityFilter implements EntityFilter {
    	
    	String parameter;
    	
    	public ChildEntityFilter(String parameter) {
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

    private abstract class SingleEntityFilter extends ChildEntityFilter {
    	
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
