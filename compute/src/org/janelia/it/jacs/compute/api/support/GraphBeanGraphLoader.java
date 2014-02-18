package org.janelia.it.jacs.compute.api.support;

import org.janelia.it.jacs.compute.api.GraphBeanRemote;
import org.janelia.it.jacs.model.graph.entity.EntityNode;
import org.janelia.it.jacs.model.graph.entity.support.AbstractGraphLoader;

public class GraphBeanGraphLoader extends AbstractGraphLoader {

    private String subjectKey;
    private GraphBeanRemote graphBean;
    
    public GraphBeanGraphLoader(String subjectKey, GraphBeanRemote graphBean) {
        super(subjectKey);
        this.subjectKey = subjectKey;
        this.graphBean = graphBean;
    }

    @Override
    public EntityNode loadNode(EntityNode node) throws Exception {
//        Access access = new Access(subjectKey, AccessPattern.ALL_ACCESSIBLE_OBJECTS);
//        Entity entity = graphBean.getEntityNode(access, node.getId());
//        graphBean.getObjectFactory().initNodeInstance(node, entity);
        return node;
    }

    @Override
    public EntityNode loadRelationships(EntityNode node) throws Exception {
        
        
//        Entity entity = _annotationDAO.getEntityById(null, node.getId());
//        getObjectFactory().initNodeInstance(node, entity);
        return node;
    }

    @Override
    public EntityNode loadRelatedNodes(EntityNode node) throws Exception {
//        
//        Set<Entity> children = _annotationDAO.getChildEntities(access.getSubjectKey(), entityId);
//        
//        Entity entity = _annotationDAO.getEntityAndChildren(null, node.getId());
//        getObjectFactory().initNodeInstance(node, entity);
        return node;
    }
}
