package org.janelia.it.jacs.shared.utils.entity;

import java.util.*;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * A builder for visiting hierarchical entity trees.
 * 
 * This is a powerful abstraction for lazily walking entity trees based on any common criteria such as entity types, 
 * attribute types, and entity names. Child entities are loaded as needed. 
 * 
 * Example:
 * 
 * <code> 
 *       EntityVistationBuilder.create(entityLoader).startAt(sampleEntity)
 *               .childrenOfType(EntityConstants.TYPE_PIPELINE_RUN)
 *               .childrenOfAttr(EntityConstants.ATTRIBUTE_RESULT)
 *               .run(new EntityVisitor() {
 *           public void visit(Entity result) throws Exception {
 *               log.info("Result: "+result.getId());
 *           }
 *       });
 * </code>
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityVistationBuilder {

	protected Logger logger = Logger.getLogger(EntityVistationBuilder.class);
	
	private Entity root;
	private List<EntityFilter> filters = new ArrayList<EntityFilter>();
	private AbstractEntityLoader loader;
	private boolean visitRootOwnerOwnedEntitiesOnly = true;
	List<Entity> visited = new ArrayList<Entity>();
    
	public static EntityVistationBuilder create(AbstractEntityLoader loader) {
	    return new EntityVistationBuilder(loader);
	}
	
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

    public EntityVistationBuilder setVisitRootOwnerOwnedEntitiesOnly(boolean ownedOnly) {
        visitRootOwnerOwnedEntitiesOnly=ownedOnly;
        return this;
    }
    
    public void run(EntityVisitor visitor) throws Exception {
    	run(root, visitor, 0);
    }
    
	private void run(Entity entity, EntityVisitor visitor, int level) throws Exception {
		
		if (entity==null || filters.isEmpty()) return;
		loader.populateChildren(entity);

		String indent = StringUtils.getIndent(level, "  ");
        logger.trace(indent+"Visiting "+entity.getName()+" (id="+entity.getId()+")");
        
		EntityFilter filter = filters.get(level);
		EntityFilter nextFilter = filters.size()>level+1 ? filters.get(level+1) : null;
		
		if (filter instanceof Descendants) {
		    logger.trace(indent+"Process descendants:");
			runRecursively(entity, visitor, new HashSet<Long>());
		}
		else if (filter instanceof Root) {
		    logger.trace(indent+"Process root:");
	    	visitor.visit(entity);
			for(EntityData ed : entity.getEntityData()) {
				visitor.visit(ed);
			}
			if (nextFilter!=null) {
				run(entity, visitor, level+1);
			}
		}
		else if (filter instanceof ChildEntityFilter) {
			ChildEntityFilter childFilter = (ChildEntityFilter)filter;
			logger.trace(indent+"Filter ("+childFilter.getClass().getName()+"):");
			List<EntityData> filtered = childFilter.getFilteredRelatives(entity);
			if (nextFilter!=null && nextFilter instanceof SameLevelFilter) {
			    logger.trace(indent+"SameLevelFilter ("+nextFilter.getClass().getName()+"):");
			    SameLevelFilter sameLevelFilter = (SameLevelFilter)nextFilter;
			    filtered = sameLevelFilter.getFiltered(filtered);
			    level++; // skip this filter
			}
			for(EntityData ed : filtered) {
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

        loader.populateChildren(entity);
        
		for(EntityData ed : entity.getEntityData()) {
			visitor.visit(ed);
			if (ed.getChildEntity()!=null) {
				runRecursively(ed.getChildEntity(), visitor, visited);		
			}
		}
    	
	}
	
	public List<Entity> getAll() throws Exception {
	    visited.clear();
	    run(new EntityVisitor() {
            public void visit(Entity entity) throws Exception {
                visited.add(entity);
            }
	    });
	    return visited;
	}

    public Entity getFirst() throws Exception {
        List<Entity> all = getAll();
        return all.get(0);
    }
    
	public Entity getLast() throws Exception {
	    List<Entity> all = getAll();
	    if (all.isEmpty()) return null;
	    return all.get(all.size()-1);
	}

    public EntityVistationBuilder startAt(Entity entity) {
    	this.root = entity;
    	return this;
    }

	public EntityVistationBuilder descendants() {
		filters.add(new Descendants());
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
	
    public EntityVistationBuilder first() {
        filters.add(new FirstEntityFilter());
        return this;
    }
    
    public EntityVistationBuilder last() {
        filters.add(new LastEntityFilter());
        return this;
    }

    public EntityVistationBuilder withAttribute(String attrName, String attrValue) {
        filters.add(new AttributeFilter(attrName, attrValue));
        return this;
    }
    
    private class Descendants implements EntityFilter {
    }

    private class Root implements EntityFilter {
    }
    
    private class ChildrenOfType extends ChildEntityFilter {
    	public ChildrenOfType(String parameter) {
    		super(parameter);
    	}
    	public boolean allow(EntityData ed) {
    		return (ed.getChildEntity()!=null && ed.getChildEntity().getEntityTypeName().equals(parameter));
    	}
    }
    
    private class ChildrenOfAttr extends ChildEntityFilter {
    	public ChildrenOfAttr(String parameter) {
    		super(parameter);
    	}
    	public boolean allow(EntityData ed) {
    		return (ed.getChildEntity()!=null && ed.getEntityAttrName().equals(parameter));
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
    		return (ed.getChildEntity()!=null && ed.getChildEntity().getEntityTypeName().equals(parameter));
    	}
    }

    private class ChildOfAttr extends SingleEntityFilter {
    	public ChildOfAttr(String parameter) {
    		super(parameter);
    	}
    	public boolean allow(EntityData ed) {
    		return (ed.getChildEntity()!=null && ed.getEntityAttrName().equals(parameter));
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
    
    private interface SameLevelFilter extends EntityFilter {        
        public List<EntityData> getFiltered(List<EntityData> entityData);
    }

    private class FirstEntityFilter implements SameLevelFilter {
        public List<EntityData> getFiltered(List<EntityData> entityData) {
            if (entityData.isEmpty()) return entityData;
            List<EntityData> orderedData = sort(entityData);
            List<EntityData> filtered = new ArrayList<EntityData>();
            filtered.add(orderedData.get(0));
            return filtered;
        }
    }
    
    private class LastEntityFilter implements SameLevelFilter {
        public List<EntityData> getFiltered(List<EntityData> entityData) {
            if (entityData.isEmpty()) return entityData;
            List<EntityData> orderedData = sort(entityData);
            List<EntityData> filtered = new ArrayList<EntityData>();
            filtered.add(orderedData.get(orderedData.size()-1));
            return filtered;
        }
    }

    private class AttributeFilter implements SameLevelFilter {
        private String attrName;
        private String attrValue;
        public AttributeFilter(String attrName, String attrValue) {
            this.attrName = attrName;
            this.attrValue = attrValue;
        }
        public List<EntityData> getFiltered(List<EntityData> entityData) {
            if (entityData.isEmpty()) return entityData;
            List<EntityData> filtered = new ArrayList<EntityData>();
            for(EntityData ed : sort(entityData)) {
                Entity child = ed.getChildEntity();
                if (child!=null) {
                    if (attrValue.equals(child.getValueByAttributeName(attrName))) {
                        filtered.add(ed);        
                    }
                }
            }
            return filtered;
        }
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
    
    private List<EntityData> sort(List <EntityData> entityData) {
        List<EntityData> orderedData = new ArrayList<EntityData>(entityData);
        Collections.sort(orderedData, new Comparator<EntityData>() {
            @Override
            public int compare(EntityData o1, EntityData o2) {
                if (o1.getId()==null) {
                    if (o2.getId()==null) {
                        return 0;
                    }
                    return -1;
                }
                else if (o2.getId()==null) {
                    return 1;
                }
                return o1.getId().compareTo(o2.getId());
            }
        });
        return orderedData;
    }
}
