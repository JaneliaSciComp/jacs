package entity

import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityData

import static org.janelia.it.jacs.model.entity.EntityConstants.TYPE_ALIGNED_BRAIN_STACK

// Globals
f = new JacsUtils("jenetta", false)
e = f.e

id = 1776112520327594155;
tree = e.getEntityTree(id);
println "Count = "+countDescendants(tree);


def countDescendants(Entity entity) {
	return countDescendants(entity, new HashSet<Long>());
}

def countDescendants(Entity entity, Set<Long> visited) {
	long count = 0;
	
	if (entity == null) return 0;
	
	if (visited.contains(entity.id)) {
		return 0;
	}
		
	if (entity.entityTypeName.equals(TYPE_ALIGNED_BRAIN_STACK)) {
		return 1;
	}
	
	visited.add(entity.getId());
	for(EntityData ed : entity.getEntityData()) {
		Entity child = ed.getChildEntity();
		if (child != null) {
			count += countDescendants(child, visited);
		}
	}
	return count;
}
