package test.wsrest

import com.fasterxml.jackson.databind.ObjectMapper
import org.janelia.it.jacs.model.domain.support.DomainDAO
import org.janelia.it.jacs.compute.wsrest.UserWorkstationWebService
import org.janelia.it.jacs.model.domain.Reference
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.workspace.*
import com.fasterxml.jackson.core.type.TypeReference
import spock.lang.*

import javax.ws.rs.core.GenericType

/*
 * tests the endpoints of the RESTful server to make sure they all process args and send return results properly
 */

class UserWorkstationSpec extends Specification {
    def userWorkstationService
    def testUser
    def testTreeNode, testReorderTreeNode
    def testTreeNodeAfterAdded, testTreeNodeAfterRemoved
    def testObjectSet
    def testObjectSetAfterAdded, testObjectSetAfterRemoved
    def referenceAdds, referenceRemoves
    def referenceAddLongs, referenceRemoveLongs
    def reorder
    def testSubjects
    DomainDAO dao

    def setup() {
        def testWorkspace = new Workspace([
                id: 1980402565539430407L
        ])
        def testChildren = [
                new Reference ("sample",1831527772221079641L),
                new Reference ("treeNode",1831527772221079642L),
                new Reference ("sample",1831527772221079643L)
        ]
        def testAfterAddChildren = [
                new Reference ("sample",1831527772221079641L),
                new Reference ("treeNode",1831527772221079642L),
                new Reference ("sample",1831527772221079643L),
                new Reference ("sample",1831527772221079644L),
                new Reference ("sample",1831527772221079645L)
        ]
        def testAfterRemoveChildren = [
                new Reference ("sample",1831527772221079641L)
        ]
        def testAfterAddMembers = [1831527772221079641L,1831527772221079642L,1831527772221079643L,1831527772221079644L,1831527772221079645L]
        def testAfterRemoveMembers = [1831527772221079641L]
        def reorderedChildren = [
                new Reference ("sample",1831527772221079643L),
                new Reference ("treeNode",1831527772221079642L),
                new Reference ("sample",1831527772221079641L)
        ]
        testTreeNode = new TreeNode([
                id: 1980402565539430410L
        ])
        testTreeNodeAfterAdded = new TreeNode([
                id: 1980402565539430410L
        ])
        testTreeNodeAfterRemoved = new TreeNode([
                id: 1980402565539430410L
        ])
        testObjectSet = new ObjectSet([
                id: 1980402565539430410L,
                collectionName: "sample",
                members: [1831527772221079641L,1831527772221079642L,1831527772221079643L]
        ])
        testObjectSetAfterAdded = new ObjectSet([
                id: 1980402565539430410L
        ])
        testObjectSetAfterRemoved = new ObjectSet([
                id: 1980402565539430410L
        ])
        testReorderTreeNode = new TreeNode([
                id: 1980402565539430410L
        ])
        referenceAddLongs = [
                1831527772221079644L,
                1831527772221079645L
        ]
        referenceRemoveLongs = [
                1831527772221079642L,
                1831527772221079643L
        ]
        referenceAdds = [
                new Reference ("sample",1831527772221079644L),
                new Reference ("sample",1831527772221079645L)
        ]
        referenceRemoves = [
                new Reference ("sample",1831527772221079642L),
                new Reference ("sample",1831527772221079643L)
        ]
        testSubjects = [new Subject([ key: "user:testuser",
                      fullName : "user:testuser",
                      groups : ["group:flylight"]
                    ])]

        def treeNodeRef = new Reference("treeNode",testTreeNode.id)
        def objectSetRef = new Reference("objectSet",testObjectSet.id)
        reorder = [new Integer(3), new Integer(2), new Integer(1)];
        testWorkspace.setChildren(testChildren)
        testTreeNode.setChildren(testChildren)
        testReorderTreeNode.setChildren(reorderedChildren)
        testTreeNodeAfterAdded.setChildren(testAfterAddChildren)
        testTreeNodeAfterRemoved.setChildren(testAfterRemoveChildren)
        testObjectSetAfterAdded.setMembers(testAfterAddMembers)
        testObjectSetAfterRemoved.setMembers(testAfterRemoveMembers)
        testUser = "user:testuser"
        dao = Stub()
        dao.getDefaultWorkspace(testUser) >> testWorkspace
        dao.save(testUser, testTreeNode) >> testTreeNode
        dao.save(testUser, testObjectSet) >> testObjectSet
        dao.getDomainObject(testUser, treeNodeRef) >> testTreeNode
        dao.getDomainObject(testUser, objectSetRef) >> testObjectSet
        dao.reorderChildren(testUser, testTreeNode, reorder) >> testReorderTreeNode
        dao.addChildren(testUser, testTreeNode, referenceAdds) >> testTreeNodeAfterAdded
        dao.removeChildren(testUser, testTreeNode, referenceRemoves) >> testTreeNodeAfterRemoved
        dao.addMembers(testUser, testObjectSet, referenceAdds) >> testObjectSetAfterAdded
        dao.removeMembers(testUser, testObjectSet, referenceRemoves) >> testObjectSetAfterRemoved
        dao.getSubjects() >> testSubjects
        userWorkstationService = new UserWorkstationWebService()
        userWorkstationService.setDao(dao)
    }


    def "get Default Workspace" () {
        when:
        def results = userWorkstationService.getWorkspace(testUser, null)

        then:
        ObjectMapper mapper = new ObjectMapper()
        Workspace workspace = mapper.readValue(results, Workspace.class)
        workspace.children.size() == 3
        workspace.children.get(1).targetCollectionName == "treeNode"
    }

    def "create a TreeNode" () {
        when:
        def results = userWorkstationService.createTreeNode(testUser, testTreeNode)

        then:
        ObjectMapper mapper = new ObjectMapper()
        TreeNode treeNode = mapper.readValue(results, TreeNode.class)
        treeNode.children.size() == 3
        treeNode.children.get(1).targetCollectionName == "treeNode"
    }

    def "reorder a TreeNode" () {
        when:
        def results = userWorkstationService.reorderTreeNode(testUser, testTreeNode.id, reorder)

        then:
        ObjectMapper mapper = new ObjectMapper()
        TreeNode treeNode = mapper.readValue(results, TreeNode.class)
        treeNode.children.get(0).targetId == 1831527772221079643L
    }

    def "add children to TreeNode" () {
        when:
        def results = userWorkstationService.addChildren(testUser, testTreeNode.id, referenceAdds)

        then:
        ObjectMapper mapper = new ObjectMapper()
        TreeNode treeNode = mapper.readValue(results, TreeNode.class)
        treeNode.children.size() == 5
    }

    def "remove children from TreeNode" () {
        when:
        def results = userWorkstationService.removeChildren(testUser, testTreeNode.id, referenceRemoves)

        then:
        ObjectMapper mapper = new ObjectMapper()
        TreeNode treeNode = mapper.readValue(results, TreeNode.class)
        treeNode.children.size() == 1
    }

    def "create an ObjectSet" () {
        when:
        def results = userWorkstationService.createObjectSet(testUser, testObjectSet)

        then:
        ObjectMapper mapper = new ObjectMapper()
        ObjectSet objectSet = mapper.readValue(results, ObjectSet.class)
        //objectSet.members.size() == 3
      //  objectSet.members.get(0) == 1831527772221079641L
    }

    def "add children to ObjectSet" () {
        when:
        def results = userWorkstationService.addMembers(testUser, testObjectSet.id, referenceAddLongs)

        then:
        ObjectMapper mapper = new ObjectMapper()
        ObjectSet objectSet = mapper.readValue(results, ObjectSet.class)
        objectSet.members.size() == 5
    }

    def "remove children from ObjectSet" () {
        when:
        def results = userWorkstationService.removeMembers(testUser, testObjectSet.id, referenceRemoveLongs)

        then:
        ObjectMapper mapper = new ObjectMapper()
        ObjectSet objectSet = mapper.readValue(results, ObjectSet.class)
        objectSet.members.size() == 1
    }

    def "get list of subjects" () {
        when:
        def results = userWorkstationService.getSubjects()

        then:
        ObjectMapper mapper = new ObjectMapper()
        List<Subject> subjects = mapper.readValue(results, new TypeReference<List<Subject>>(){})
        subjects.size() == 1
    }
}
