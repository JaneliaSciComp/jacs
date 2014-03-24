package org.janelia.it.jacs.compute.api.support;

import java.util.Collection;

import org.janelia.it.jacs.compute.api.GraphBeanRemote;
import org.janelia.it.jacs.model.graph.entity.EntityNode;
import org.janelia.it.jacs.model.graph.support.GraphUtils;

/**
 * GraphLoader implementation using the remote GraphBean EJB.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GraphBeanGraphLoader extends AbstractGraphLoader {

    private Access access;
    private GraphBeanRemote graphBean;
    
    public GraphBeanGraphLoader(Access access, GraphBeanRemote graphBean) {
        super(access);
        this.access = access;
        this.graphBean = graphBean;
    }

    @Override
    public EntityNode loadNode(EntityNode node) throws Exception {
		// TODO: right now this isn't fully implemented, and it's not used by anything. We'll implement it when we need it.
		if (true) throw new UnsupportedOperationException();
		return null;
		//EntityNode newNode = graphBean.getEntityNode(access, node.getId());
        //GraphUtils.copyNode(newNode, node);
        //return node;
    }

    @Override
    public EntityNode loadRelationships(EntityNode node) throws Exception {
    	// This implementation loads relationships along with the EntityNode, so this is a NOP
        return node;
    }

    @Override
    public EntityNode loadRelatedNodes(EntityNode node) throws Exception {
        
        Collection<EntityNode> relatedNodes = graphBean.getOutgoingRelatedObjects(access, node.getId());
        
        if (!node.isRelsInit()) {
        	throw new IllegalArgumentException("Cannot load related nodes for entity node without loaded relationships");
        }

        // First set the targets on all the relationship end nodes
        GraphUtils.initRelationshipTargets(node, relatedNodes);
        
        // Now re-initialize the relationships so that @RelatedTo fields can be set 
        GraphUtils.initRelationships(node, node.getRelationships());
        
        return node;
    }
}
