package org.janelia.it.jacs.compute.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.janelia.it.jacs.compute.api.support.Access;
import org.janelia.it.jacs.model.domain.impl.entity.metamodel.EntityDomainObject;
import org.janelia.it.jacs.model.domain.interfaces.metamodel.DomainObject;
import org.janelia.it.jacs.model.domain.interfaces.metamodel.Permission;
import org.janelia.it.jacs.model.domain.interfaces.metamodel.Relationship;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the DomainBeanRemote remote EJB interface.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainBeanRemoteTest extends RemoteEJBTest {

    private static final String SUBJECT_KEY = "user:rokickik";
    
    private DomainBeanRemote d;
    private Access a;
    
    private List<Long> createdEntities = new ArrayList<Long>();
    
    @Before
    public void init() throws Exception {
        this.d = (DomainBeanRemote) context.lookup("compute/DomainEJB/remote");
        this.a = new Access(SUBJECT_KEY, Access.AccessPattern.ALL_ACCESSIBLE_OBJECTS);
    }
    
    @Test
    public void testAlignmentBoardsFolder() throws Exception {
        Collection<DomainObject> results = d.getDomainObjectsByName(a, "Alignment Boards");
        Assert.assertFalse(results.isEmpty());
        for(DomainObject obj : results) {
            Assert.assertNotNull(obj.getGuid());
            Assert.assertNotNull(obj.getTypeName());
            Assert.assertNotNull(obj.getOwnerKey());
            Assert.assertNotNull(obj.getCreationDate());
            Assert.assertNotNull(obj.getUpdatedDate());
            Assert.assertNotNull(obj.getAttributeValue(EntityConstants.ATTRIBUTE_COMMON_ROOT));
            Assert.assertNotNull(obj.getAttributeValue(EntityConstants.ATTRIBUTE_IS_PROTECTED));
            Assert.assertFalse(obj.relationshipsAreInitialized());
            Assert.assertFalse(obj.getPermissions().isEmpty());
            for(Permission p : obj.getPermissions()) {
                Assert.assertNotNull(p.getSubjectKey());
            }
        }
    }

    @Test
    public void testDomainObjectCreationDeletion() throws Exception {
        
        String name = "TEST_FOLDER";
        String entityTypeName = EntityConstants.TYPE_FOLDER;
        DomainObject domainObject = createDomainObject(name, entityTypeName);
        
        Assert.assertNotNull(domainObject.getGuid());
        Assert.assertEquals(domainObject.getName(), name);
        Assert.assertEquals(domainObject.getOwnerKey(), SUBJECT_KEY);
        Assert.assertEquals(domainObject.getTypeName(), entityTypeName);
        Assert.assertEquals(domainObject.getCreationDate(), domainObject.getUpdatedDate());
        
        createdEntities.add(domainObject.getGuid());
        d.deleteDomainObjectTree(a, domainObject.getGuid());
    }

    @Test
    public void testDomainObjectRelation() throws Exception {

        String name = "TEST_FOLDER";
        String entityTypeName = EntityConstants.TYPE_FOLDER;
        DomainObject sourceObject = createDomainObject(name, entityTypeName);
        
        String name2 = "TEST_FILE";
        String entityTypeName2 = EntityConstants.TYPE_FILE;
        DomainObject targetObject = createDomainObject(name2, entityTypeName2);
        
        Integer orderIndex = 5;
        String relType = EntityConstants.ATTRIBUTE_ENTITY;
        Relationship rel = d.createRelationship(a, sourceObject.getGuid(), targetObject.getGuid(), orderIndex, relType);

        Assert.assertNotNull(rel.getGuid());
        Assert.assertEquals(rel.getType(), relType);
        Assert.assertEquals(rel.getOrderIndex(), orderIndex);
        Assert.assertNotNull(rel.getCreationDate());
        Assert.assertNotNull(rel.getUpdatedDate());
        Assert.assertEquals(rel.getSource().getGuid(), sourceObject.getGuid());
        Assert.assertEquals(rel.getTarget().getGuid(), targetObject.getGuid());
        
        // Ensure that everything is actually saved in the database
        DomainObject sourceObject2 = d.getDomainObjectAndChildren(a, sourceObject.getGuid());
        Assert.assertTrue(sourceObject2.relationshipsAreInitialized());
        Assert.assertEquals(sourceObject2.getRelationships().size(), 1);
        Relationship sameRel = sourceObject2.getRelationships().iterator().next();
        Assert.assertEquals(rel.getGuid(), sameRel.getGuid());
        Assert.assertEquals(rel.getType(), sameRel.getType());
        Assert.assertEquals(rel.getOrderIndex(), sameRel.getOrderIndex());
        
        Assert.assertEquals(rel.getSource().getName(), name);
        Assert.assertEquals(rel.getSource().getOwnerKey(), SUBJECT_KEY);
        Assert.assertEquals(rel.getSource().getTypeName(), entityTypeName);
        Assert.assertEquals(rel.getSource().getGuid(), sameRel.getSource().getGuid());
        
        Assert.assertEquals(rel.getTarget().getName(), name2);
        Assert.assertEquals(rel.getTarget().getOwnerKey(), SUBJECT_KEY);
        Assert.assertEquals(rel.getTarget().getTypeName(), entityTypeName2);
        Assert.assertEquals(rel.getTarget().getGuid(), sameRel.getTarget().getGuid());
        
        d.deleteDomainObjectTree(a, targetObject.getGuid());
        
        // Ensure that relationship was deleted
        sourceObject2 = d.getDomainObjectAndChildren(a, sourceObject.getGuid());
        Assert.assertNotNull(sourceObject2);
        Assert.assertTrue(sourceObject2.relationshipsAreInitialized());
        Assert.assertTrue(sourceObject2.getRelationships().isEmpty());

        d.deleteDomainObjectTree(a, sourceObject.getGuid());
    }
    
    private DomainObject createDomainObject(String name, String entityTypeName) throws ComputeException {
        DomainObject domainObject = new EntityDomainObject(name, SUBJECT_KEY, entityTypeName);
        domainObject = d.createDomainObject(a, domainObject);
        createdEntities.add(domainObject.getGuid());
        return domainObject;
    }
    
    @After
    public void cleanup() {
        for(Long entityId : createdEntities) {
            try {
                d.deleteDomainObjectTree(a, entityId);    
            }
            catch (Exception e) {
                // This is expected. Each test case should clean up all the entities it creates, so this should fail.
            }
        }
    }
    
}
