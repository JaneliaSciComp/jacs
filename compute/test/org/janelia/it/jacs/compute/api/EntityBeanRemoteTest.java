package org.janelia.it.jacs.compute.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.util.EntityBeanEntityLoader;
import org.janelia.it.jacs.model.TestCategories;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityActorPermission;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.user_data.Group;
import org.janelia.it.jacs.model.user_data.SubjectRelationship;
import org.janelia.it.jacs.model.user_data.User;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class EntityBeanRemoteTest extends RemoteEJBTest {

    private static final Logger log = Logger.getLogger(EntityBeanRemoteTest.class);

    private static ComputeBeanRemote c;
    private static EntityBeanRemote e;
    private static EntityBeanEntityLoader l;
    private static String tester1;
    private static String tester2;
    private static String testers;
    
    private static List<Entity> createdEntities = new ArrayList<Entity>();
    
    @BeforeClass
    public static void init() throws Exception {
        c = (ComputeBeanRemote) context.lookup("compute/ComputeEJB/remote");
        e = (EntityBeanRemote) context.lookup("compute/EntityEJB/remote");
        l = new EntityBeanEntityLoader(e);
        
        User user1 = c.getUserByNameOrKey("tester1");
        if (user1==null) {
            user1 = c.createUser("tester1", "Tester 1");
        }
        tester1 = user1.getKey();
        
        User user2 = c.getUserByNameOrKey("tester2");
        if (user2==null) {
            user2 = c.createUser("tester2", "Tester 2");
        }
        tester2 = user2.getKey();

        Group group = c.getGroupByNameOrKey("testers");
        if (group==null) {
            group = c.createGroup(tester1, "testers");
        }
        testers = group.getKey();
        
        boolean groupContains1 = false;
        boolean groupContains2 = false;
        
        for(SubjectRelationship rel : group.getUserRelationships()) {
            if (SubjectRelationship.TYPE_GROUP_MEMBER.equals(rel.getRelationshipType())) {
                if (rel.getUser().getKey().equals(tester1)) {
                    groupContains1 = true;
                }
                if (rel.getUser().getKey().equals(tester1)) {
                    groupContains2 = true;
                }
            }
        }
        
        if (!groupContains1) {
            c.addUserToGroup(tester1, testers);
        }
        if (!groupContains2) {
            c.addUserToGroup(tester2, testers);
        }
    }
    
    @AfterClass
    public static void cleanup() {
        // Clean up any entity that was created but not deleted by a test
        for(Iterator<Entity> i=createdEntities.iterator(); i.hasNext(); ) {
            Entity entity = i.next();
            try {
                Entity freshEntity = e.getEntityById(entity.getOwnerKey(), entity.getId());
                if (freshEntity!=null) {
                    log.info("Cleaning up "+freshEntity.getId());
                    e.deleteEntityTreeById(freshEntity.getOwnerKey(), freshEntity.getId(), true);    
                }
            }
            catch (Exception e) {
                log.warn("Problem cleaning up created entity "+entity.getId(),e);
            }
        }
    }

    /**
     * Scenario: entity tree owned by a single user.
     * Expected result: deletion of folder tree removes all entities in the tree
     */
    @Test
    @Category(TestCategories.FastIntegrationTests.class)
    public void testEntityDeletionScenario1() throws Exception {
        
        Entity entity1 = createEntity(tester1, "Folder", "Test1");
        Entity entity2 = createEntity(tester1, "Folder", "Test2");
        Entity entity3 = createEntity(tester1, "Folder", "Test3");
        e.addEntityToParent(tester1, entity1.getId(), entity2.getId(), null, EntityConstants.ATTRIBUTE_ENTITY);
        e.addEntityToParent(tester1, entity2.getId(), entity3.getId(), null, EntityConstants.ATTRIBUTE_ENTITY);
        
        entity1 = e.getEntityById(tester1, entity1.getId());
        l.populateChildren(entity1);
        
        assertEquals("Test1", entity1.getName());
        assertFalse(entity1.getChildren().isEmpty());
        
        e.deleteEntityTreeById(tester1, entity1.getId());
        
        entity1 = e.getEntityById(tester1, entity1.getId());
        assertNull(entity1);
        entity2 = e.getEntityById(tester1, entity2.getId());
        assertNull(entity2);
        entity3 = e.getEntityById(tester1, entity3.getId());
        assertNull(entity3);
    }

    /**
     * Scenario: folder full of entities owned by group
     * Expected result: deletion of folder tree results in deletion of the folder but not the items inside it
     */
    @Test
    @Category(TestCategories.FastIntegrationTests.class)
    public void testEntityDeletionScenario2() throws Exception {
                
        Entity entity1 = createEntity(tester1, "Folder", "Test1");
        Entity entity2 = createEntity(testers, "Folder", "Test2");
        Entity entity3 = createEntity(testers, "Folder", "Test3");
        e.addEntityToParent(tester1, entity1.getId(), entity2.getId(), null, EntityConstants.ATTRIBUTE_ENTITY);
        e.addEntityToParent(tester1, entity1.getId(), entity3.getId(), null, EntityConstants.ATTRIBUTE_ENTITY);
                
        e.deleteEntityTreeById(tester1, entity1.getId());
        
        entity1 = e.getEntityById(tester1, entity1.getId());
        assertNull(entity1);
        entity2 = e.getEntityById(testers, entity2.getId());
        assertNotNull(entity2);
        entity3 = e.getEntityById(testers, entity3.getId());
        assertNotNull(entity3);
    }

    /**
     * Scenario: folder full of entities owned by another user, but with read permission
     * Expected result: deletion of folder tree results in deletion of the folder but not the items inside it
     */
    @Test
    @Category(TestCategories.FastIntegrationTests.class)
    public void testEntityDeletionScenario3() throws Exception {
                
        Entity entity1 = createEntity(tester1, "Folder", "Test1");
        Entity entity2 = createEntity(tester2, "Folder", "Test2");
        Entity entity3 = createEntity(tester2, "Folder", "Test3");
        grantPermission(tester2, entity2, tester1, "r");
        grantPermission(tester2, entity3, tester1, "r");
        e.addEntityToParent(tester1, entity1.getId(), entity2.getId(), null, EntityConstants.ATTRIBUTE_ENTITY);
        e.addEntityToParent(tester1, entity1.getId(), entity3.getId(), null, EntityConstants.ATTRIBUTE_ENTITY);
                
        e.deleteEntityTreeById(tester1, entity1.getId());
        
        entity1 = e.getEntityById(tester1, entity1.getId());
        assertNull(entity1);
        entity2 = e.getEntityById(tester2, entity2.getId());
        assertNotNull(entity2);
        entity3 = e.getEntityById(tester2, entity3.getId());
        assertNotNull(entity3);
    }

    /**
     * Scenario: folder full of entities owned by another user, but with write permission
     * Expected result: deletion of folder tree results in deletion of entire folder tree
     */
    @Test
    @Category(TestCategories.FastIntegrationTests.class)
    public void testEntityDeletionScenario4() throws Exception {
                
        Entity entity1 = createEntity(tester1, "Folder", "Test1");
        Entity entity2 = createEntity(tester2, "Folder", "Test2");
        Entity entity3 = createEntity(tester2, "Folder", "Test3");
        grantPermission(tester2, entity2, tester1, "rw");
        grantPermission(tester2, entity3, tester1, "rw");
        e.addEntityToParent(tester1, entity1.getId(), entity2.getId(), null, EntityConstants.ATTRIBUTE_ENTITY);
        e.addEntityToParent(tester1, entity1.getId(), entity3.getId(), null, EntityConstants.ATTRIBUTE_ENTITY);
                
        e.deleteEntityTreeById(tester1, entity1.getId());

        entity1 = e.getEntityById(tester1, entity1.getId());
        assertNull(entity1);
        entity2 = e.getEntityById(tester2, entity2.getId());
        assertNull(entity2);
        entity3 = e.getEntityById(tester2, entity3.getId());
        assertNull(entity3);
    }

    /**
     * Scenario: folder full of entities referenced from another location
     * Expected result: deletion of folder tree results in deletion of top level folder but not contents
     */
    @Test
    @Category(TestCategories.FastIntegrationTests.class)
    public void testEntityDeletionScenario5() throws Exception {
                
        Entity entity1 = createEntity(tester1, "Folder", "Test1");
        Entity entity2 = createEntity(tester1, "Folder", "Test2");
        Entity entity3 = createEntity(tester1, "Folder", "Test3");
        Entity entity4 = createEntity(tester1, "Folder", "Test4");
        e.addEntityToParent(tester1, entity1.getId(), entity2.getId(), null, EntityConstants.ATTRIBUTE_ENTITY);
        e.addEntityToParent(tester1, entity1.getId(), entity3.getId(), null, EntityConstants.ATTRIBUTE_ENTITY);
        e.addEntityToParent(tester1, entity4.getId(), entity2.getId(), null, EntityConstants.ATTRIBUTE_ENTITY);
        e.addEntityToParent(tester1, entity4.getId(), entity3.getId(), null, EntityConstants.ATTRIBUTE_ENTITY);
                
        e.deleteEntityTreeById(tester1, entity4.getId());
        entity4 = e.getEntityById(tester1, entity4.getId());
        assertNull(entity4);

        entity1 = e.getEntityById(tester1, entity1.getId());
        assertNotNull(entity1);
        entity2 = e.getEntityById(tester1, entity2.getId());
        assertNotNull(entity2);
        entity3 = e.getEntityById(tester1, entity3.getId());
        assertNotNull(entity3);
    }

    /**
     * Scenario: add folder to folder with permissions
     * Expected result: the permissions on the folder propagate to the child
     */
    @Test
    @Category(TestCategories.FastIntegrationTests.class)
    public void testEntityPermissionScenario1() throws Exception {
                
        Entity entity1 = createEntity(tester1, "Folder", "Test1");
        grantPermission(tester1, entity1, tester2, "r");
        
        Entity entity2 = createEntity(tester1, "Folder", "Test2");
        e.addEntityToParent(tester1, entity1.getId(), entity2.getId(), null, EntityConstants.ATTRIBUTE_ENTITY);

        entity1 = e.getEntityById(tester2, entity1.getId());
        assertNotNull(entity1);
        entity2 = e.getEntityById(tester2, entity2.getId());
        assertNotNull(entity2);
    }

    /**
     * Scenario: add non-owned folder to folder with rw permissions
     * Expected result: the permissions on the folder propagate to the child, and the owner gets read access
     */
    @Test
    @Category(TestCategories.FastIntegrationTests.class)
    public void testEntityPermissionScenario2() throws Exception {
                
        Entity entity1 = createEntity(tester1, "Folder", "Test1");
        grantPermission(tester1, entity1, tester2, "rw");
        
        Entity entity2 = createEntity(tester2, "Folder", "Test2");
        e.addEntityToParent(tester2, entity1.getId(), entity2.getId(), null, EntityConstants.ATTRIBUTE_ENTITY);

        entity1 = e.getEntityById(tester1, entity1.getId());
        assertNotNull(entity1);
        entity2 = e.getEntityById(tester1, entity2.getId());
        assertNotNull(entity2);
        entity1 = e.getEntityById(tester2, entity1.getId());
        assertNotNull(entity1);
        entity2 = e.getEntityById(tester2, entity2.getId());
        assertNotNull(entity2);
    }
    
    /**
     * Simple permission grant without creation of Shared Data links.
     */
    private void grantPermission(String subjectKey, Entity entity, String granteeKey, String permissions) throws Exception {
        EntityActorPermission eap = new EntityActorPermission();
        eap.setEntity(entity);
        eap.setPermissions(permissions);
        eap.setSubjectKey(granteeKey);
        eap = e.saveOrUpdatePermission(subjectKey, eap);
        entity.getEntityActorPermissions().add(eap);
    }
    
    /**
     * Create a test entity but keep track of it so it can be cleaned up.
     */
    private Entity createEntity(String ownerKey, String type, String name) throws ComputeException {
        Entity entity = e.createEntity(ownerKey, type, name);
        createdEntities.add(entity);
        return entity;
    }
}

