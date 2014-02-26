package org.janelia.it.jacs.compute.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.support.Access;
import org.janelia.it.jacs.compute.api.support.GraphBeanGraphLoader;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.graph.entity.EntityNode;
import org.janelia.it.jacs.model.graph.entity.EntityPermission;
import org.janelia.it.jacs.model.graph.entity.EntityRelationship;
import org.janelia.it.jacs.model.graph.entity.Folder;
import org.janelia.it.jacs.model.graph.entity.Image3d;
import org.janelia.it.jacs.model.graph.entity.PipelineRun;
import org.janelia.it.jacs.model.graph.entity.Sample;
import org.janelia.it.jacs.model.graph.entity.support.EntityGraphUtils;
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

    private static final Logger log = Logger.getLogger(GraphBeanRemoteTest.class);
    
    private static final String SUBJECT_KEY = "user:rokickik";
    private static final String TEST_SAMPLE_NAME = "GMR_57C10_AD_01-20120206_3_E1-Left_Optic_Lobe";
    
    private GraphBeanRemote d;
    private Access a;
    
    private List<Long> createdEntities = new ArrayList<Long>();
    
    @Before
    public void init() throws Exception {
        this.d = (GraphBeanRemote) context.lookup("compute/GraphEJB/remote");
        this.a = new Access(SUBJECT_KEY, Access.AccessPattern.ALL_ACCESSIBLE_OBJECTS);
    }
    
    @After
    public void cleanup() {
    	// Clean up any entity that was created but not deleted by a test
        for(Iterator<Long> i=createdEntities.iterator(); i.hasNext(); ) {
        	Long entityId = i.next();
            try {
            	if (d.getEntityNode(a, entityId)!=null) {
            		d.deleteEntityNodeTree(a, entityId);	
            	}
            }
            catch (Exception e) {
                log.warn("Problem cleaning up created entity "+entityId,e);
            }
        }
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
            Assert.assertTrue(folder.isThisInit());
            Assert.assertTrue(folder.isRelsInit());
            Assert.assertTrue(folder.isCommonRoot());
            Assert.assertTrue(folder.isProtected());
            Assert.assertNotNull(obj.getPermissions());
            for(EntityPermission p : obj.getPermissions()) {
                Assert.assertNotNull(p.getSubjectKey());
            }
            for(EntityRelationship rel : obj.getRelationships()) {
                Assert.assertNotNull(rel.getId());
                Assert.assertFalse(rel.getTargetNode().isThisInit());
                Assert.assertFalse(rel.getTargetNode().isRelsInit());
                Assert.assertNull(rel.getTargetNode().getName());
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

        d.deleteEntityNodeTree(a, entityNode.getId());
    }

    @Test
    public void testEntityNodeRelation() throws Exception {

        String name = "TEST_FOLDER";
        EntityNode sourceObject = createEntityNode(name, Folder.class);
        Assert.assertNotNull(sourceObject.getId());
        Assert.assertEquals(sourceObject.getName(), name);
        Assert.assertEquals(sourceObject.getOwnerKey(), SUBJECT_KEY);
        Assert.assertTrue(sourceObject.isThisInit());
        Assert.assertTrue(sourceObject.isRelsInit());
        Assert.assertTrue(sourceObject instanceof Folder);
        Folder folder = (Folder)sourceObject;
        Assert.assertFalse(folder.isCommonRoot());
        Assert.assertFalse(folder.isProtected());

        String name2 = "TEST_IMAGE";
        EntityNode targetObject = createEntityNode(name2, Image3d.class);
        Assert.assertNotNull(targetObject.getId());
        Assert.assertEquals(targetObject.getName(), name2);
        Assert.assertEquals(targetObject.getOwnerKey(), SUBJECT_KEY);
        Assert.assertTrue(targetObject.isThisInit());
        Assert.assertTrue(targetObject.isRelsInit());
        Assert.assertTrue(targetObject instanceof Image3d);

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
        Assert.assertNotNull(sourceObject2.getRelationships());
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
        Assert.assertNotNull(sourceObject2.getRelationships());
        Assert.assertTrue(sourceObject2.getRelationships().isEmpty());

        d.deleteEntityNodeTree(a, sourceObject.getId());
    }

    @Test
    public void testTreeBrowsing() throws Exception {

    	String user = "user:nerna";
        Access aa = new Access(user, Access.AccessPattern.ALL_ACCESSIBLE_OBJECTS);
        
        Collection<EntityNode> results = d.getEntityNodesByName(aa, TEST_SAMPLE_NAME);
        Assert.assertFalse(results.isEmpty());
        Assert.assertEquals(results.size(),1);
        Sample sample = (Sample)results.iterator().next();
        Assert.assertEquals("A",sample.getAge());
        Assert.assertEquals("sssr",sample.getChanSpec());
        Assert.assertEquals("nerna_optic_lobe_left",sample.getDataSetIdentifier());
        Assert.assertEquals("Two_recombinase_flipouts_A",sample.getEffector());
        Assert.assertEquals("GMR_57C10_AD_01",sample.getLine());
        Assert.assertEquals("63x",sample.getObjective());
        Assert.assertEquals("20120206_3_E1",sample.getSlideCode());
        Assert.assertEquals("OPTIC_TILE",sample.getTilingPattern());

        Assert.assertTrue(sample.isRelsInit());
        Assert.assertTrue(sample.isThisInit());
        Assert.assertNotNull(sample.getPermissions());
        Assert.assertTrue(sample.getRelationships().size()>1);
        Assert.assertFalse(EntityGraphUtils.areRelatedNodesLoaded(sample));
        
        for(EntityRelationship rel : sample.getRelationships()) {
        	Assert.assertNotNull(rel.getId());
        	Assert.assertNotNull(rel.getType());
        	Assert.assertNotNull(rel.getOwnerKey());
        	Assert.assertNotNull(rel.getCreationDate());
        	Assert.assertNotNull(rel.getUpdatedDate());

        	EntityNode node = rel.getTargetNode();
        	Assert.assertNotNull(node);
        	Assert.assertNotNull(node.getId());
            Assert.assertFalse(node.isThisInit());
            Assert.assertFalse(node.isRelsInit());
        }
        
        GraphBeanGraphLoader loader = new GraphBeanGraphLoader(aa, d);
        loader.loadRelatedNodes(sample);
        
        Assert.assertTrue(EntityGraphUtils.areRelatedNodesLoaded(sample));

        for(EntityRelationship rel : sample.getRelationships()) {
        
        	EntityNode relNode = rel.getTargetNode();
            Assert.assertTrue("Related node not loaded after explicit load: "+relNode.getId(), relNode.isThisInit());
            Assert.assertFalse("Related node is not typed after explicit load: "+relNode.getId(), relNode.getClass().getName().equals("EntityNode"));
            Assert.assertTrue(relNode.isRelsInit());
        	Assert.assertNotNull(relNode.getName());
        	Assert.assertNotNull(relNode.getOwnerKey());
        	Assert.assertNotNull(relNode.getPermissions());
            Assert.assertNotNull(relNode.getPermissions());
        }
        
        Assert.assertTrue(sample.getPipelineRuns().size()>1);
        for(PipelineRun pipelineRun : sample.getPipelineRuns()) {
        	Assert.assertNotNull(pipelineRun.getName());
        	Assert.assertTrue(pipelineRun.getResults().isEmpty()); // Not yet loaded

            loader.loadRelatedNodes(pipelineRun);
        	Assert.assertFalse(pipelineRun.getResults().isEmpty()); // Should now be loaded   
        }
    }
    
    private EntityNode createEntityNode(String name, Class<?> entityType) throws Exception {
        EntityNode entityNode = (EntityNode)entityType.newInstance();
        entityNode.setName(name);
        entityNode.setOwnerKey(SUBJECT_KEY);
        entityNode = d.createEntityNode(a, entityNode);
        createdEntities.add(entityNode.getId());
        return entityNode;
    }
    
}
