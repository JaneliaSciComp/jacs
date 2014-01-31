package org.janelia.it.jacs.compute.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.janelia.it.jacs.compute.api.support.Access;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.graph.entity.EntityNode;
import org.janelia.it.jacs.model.graph.entity.EntityPermission;
import org.janelia.it.jacs.model.graph.entity.EntityRelationship;
import org.janelia.it.jacs.model.graph.entity.Folder;
import org.janelia.it.jacs.model.graph.entity.Image3d;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the DomainBeanRemote remote EJB interface.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GraphBeanRemoteTest extends RemoteEJBTest {

    private static final String SUBJECT_KEY = "user:rokickik";
    
    private GraphBeanRemote d;
    private Access a;
    
    private List<Long> createdEntities = new ArrayList<Long>();
    
    @Before
    public void init() throws Exception {
        this.d = (GraphBeanRemote) context.lookup("compute/GraphEJB/remote");
        this.a = new Access(SUBJECT_KEY, Access.AccessPattern.ALL_ACCESSIBLE_OBJECTS);
    }
    
    @Test
    public void testAlignmentBoardsFolder() throws Exception {
        Collection<EntityNode> results = d.getEntityNodesByName(a, "Alignment Boards");
        Assert.assertFalse(results.isEmpty());
        for(EntityNode obj : results) {
            Assert.assertNotNull(obj.getId());
            Assert.assertNotNull(obj.getOwnerKey());
            Assert.assertNotNull(obj.getCreationDate());
            Assert.assertNotNull(obj.getUpdatedDate());
            Assert.assertTrue(obj instanceof Folder);
            Folder folder = (Folder)obj;
            Assert.assertTrue(folder.isCommonRoot());
            Assert.assertTrue(folder.isProtected());
            Assert.assertFalse(obj.isRelsInit());
            Assert.assertFalse(obj.getPermissions().isEmpty());
            for(EntityPermission p : obj.getPermissions()) {
                Assert.assertNotNull(p.getSubjectKey());
            }
        }
    }

    @Test
    public void testEntityNodeCreationDeletion() throws Exception {
        
        String name = "TEST_FOLDER";
        EntityNode entityNode = createEntityNode(name, Folder.class);
        
        Assert.assertNotNull(entityNode.getId());
        Assert.assertEquals(entityNode.getName(), name);
        Assert.assertEquals(entityNode.getOwnerKey(), SUBJECT_KEY);
        Assert.assertTrue(entityNode instanceof Folder);
        Assert.assertEquals(entityNode.getCreationDate(), entityNode.getUpdatedDate());
        
        createdEntities.add(entityNode.getId());
        d.deleteEntityNodeTree(a, entityNode.getId());
    }

    @Test
    public void testEntityNodeRelation() throws Exception {

        String name = "TEST_FOLDER";
        EntityNode sourceObject = createEntityNode(name, Folder.class);
        
        String name2 = "TEST_IMAGE";
        EntityNode targetObject = createEntityNode(name2, Image3d.class);
        
        Integer orderIndex = 5;
        String relType = EntityConstants.ATTRIBUTE_ENTITY;
        EntityRelationship rel = d.createRelationship(a, sourceObject.getId(), targetObject.getId(), orderIndex, relType);

        Assert.assertNotNull(rel.getId());
        Assert.assertEquals(rel.getType(), relType);
        Assert.assertEquals(rel.getOrderIndex(), orderIndex);
        Assert.assertNotNull(rel.getCreationDate());
        Assert.assertNotNull(rel.getUpdatedDate());
        Assert.assertEquals(rel.getSourceNode().getId(), sourceObject.getId());
        Assert.assertEquals(rel.getTargetNode().getId(), targetObject.getId());
        
        // Ensure that everything is actually saved in the database
        EntityNode sourceObject2 = d.getEntityNodeAndChildren(a, sourceObject.getId());
        Assert.assertTrue(sourceObject2.isRelsInit());
        Assert.assertEquals(sourceObject2.getRelationships().size(), 1);
        EntityRelationship sameRel = sourceObject2.getRelationships().iterator().next();
        Assert.assertEquals(rel.getId(), sameRel.getId());
        Assert.assertEquals(rel.getType(), sameRel.getType());
        Assert.assertEquals(rel.getOrderIndex(), sameRel.getOrderIndex());
        
        Assert.assertEquals(rel.getSourceNode().getName(), name);
        Assert.assertEquals(rel.getSourceNode().getOwnerKey(), SUBJECT_KEY);
        Assert.assertTrue(rel.getSourceNode() instanceof Folder);
        Assert.assertEquals(rel.getSourceNode().getId(), sameRel.getSourceNode().getId());
        
        Assert.assertEquals(rel.getTargetNode().getName(), name2);
        Assert.assertEquals(rel.getTargetNode().getOwnerKey(), SUBJECT_KEY);
        Assert.assertTrue(rel.getTargetNode() instanceof Image3d);
        Assert.assertEquals(rel.getTargetNode().getId(), sameRel.getTargetNode().getId());
        
        d.deleteEntityNodeTree(a, targetObject.getId());
        
        // Ensure that relationship was deleted
        sourceObject2 = d.getEntityNodeAndChildren(a, sourceObject.getId());
        Assert.assertNotNull(sourceObject2);
        Assert.assertTrue(sourceObject2.isRelsInit());
        Assert.assertTrue(sourceObject2.getRelationships().isEmpty());

        d.deleteEntityNodeTree(a, sourceObject.getId());
    }
    
    private EntityNode createEntityNode(String name, Class<?> entityType) throws Exception {
        EntityNode entityNode = (EntityNode)entityType.newInstance();
        entityNode.setName(name);
        entityNode.setOwnerKey(SUBJECT_KEY);
        entityNode = d.createEntityNode(a, entityNode);
        createdEntities.add(entityNode.getId());
        return entityNode;
    }
    
    @After
    public void cleanup() {
        for(Long entityId : createdEntities) {
            try {
                d.deleteEntityNodeTree(a, entityId);    
            }
            catch (Exception e) {
                // This is expected. Each test case should clean up all the entities it creates, so this should fail.
            }
        }
    }
    
}
