
package org.janelia.it.jacs.server.access.hibernate;

import org.janelia.it.jacs.model.common.BlastableNodeVO;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.common.UserDataNodeVO;
import org.janelia.it.jacs.model.genomics.SequenceType;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.blast.BlastResultNode;
import org.janelia.it.jacs.server.access.NodeDAO;
import org.janelia.it.jacs.server.access.UserDAO;
import org.janelia.it.jacs.test.JacswebTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 8, 2007
 * Time: 2:16:35 PM
 *
 */
public class NodeDAOImplTest extends JacswebTestCase {

    private NodeDAO nodeDAO;
    private UserDAO userDAO;

    public NodeDAO getNodeDAO() {
        return nodeDAO;
    }

    public void setNodeDAO(NodeDAO nodeDAO) {
        this.nodeDAO = nodeDAO;
    }

    public UserDAO getUserDAO() {
        return userDAO;
    }

    public void setUserDAO(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public NodeDAOImplTest() {
        super(NodeDAOImplTest.class.getName());
    }

    public void testNodeDAO() {
        assertNotNull(nodeDAO);
        assertNotNull(userDAO);
    }

    public void testGetUserSpecificData() {
        List nodes;
        try {
            User user=userDAO.getUserByName("smurphy");
            assertNotNull(user);
            nodes=nodeDAO.getUserSpecificData("smurphy");
            assertNotNull(nodes);
            assertTrue(nodes.size()>0);
            for (Object node1 : nodes) {
                Node node = (Node) node1;
                assertNotNull(node);
                assertNotNull(node.getName());
            }
        } catch (Exception ex) {

            String message="Exception: "+ex.getMessage();
            logger.warn(message);
            fail(message);
        }
    }

    public void testGetBlastResultNodeByTaskId() {
        BlastResultNode blastResultNode;
        try {
            blastResultNode=nodeDAO.getBlastResultNodeByTaskId(1045153077989998908L);
            assertNotNull(blastResultNode);
            assertEquals(blastResultNode.getObjectId(),new Long(1045153130301358430L));
        } catch (Exception ex) {

            String message="Exception: "+ex.getMessage();
            logger.warn(message);
            fail(message);
        }
    }

    public void testGetBlastableDatabaseList() {
        BlastableNodeVO[] nodeArr;
        try {
            User user=userDAO.getUserByName("smurphy");
            assertNotNull(user);
            nodeArr=nodeDAO.getBlastableDatabaseList(SequenceType.NOT_SPECIFIED, "smurphy");
            assertNotNull(nodeArr);
            for (BlastableNodeVO aNodeArr : nodeArr) {
                assertNotNull(aNodeArr);
                //assertNotNull(nodeArr[i].getDataType());     deprecated
            }
        } catch (Exception ex) {

            String message="Exception: "+ex.getMessage();
            logger.warn(message);
            fail(message);
        }
    }

    public void testGetPagedBlastableSubjectNodes() {
        List nodeList;
        try {
            String[] userLogins = new String[] {
                    User.SYSTEM_USER_LOGIN
            };
            int n = nodeDAO.getNumBlastableSubjectNodes(userLogins);
            SortArgument[] sortArgs = new SortArgument[] {
                    new SortArgument("blastableSubject.class", SortArgument.SORT_DESC),
                    new SortArgument("owner",SortArgument.SORT_DESC),
                    new SortArgument("visibility", SortArgument.SORT_DESC),
                    new SortArgument("length",SortArgument.SORT_ASC),
                    new SortArgument("description",SortArgument.SORT_DESC)
            };
            Random rgen = new Random(System.currentTimeMillis());
            int offset = rgen.nextInt(n);
            nodeList=nodeDAO.getPagedBlastableSubjectNodes(userLogins,SequenceType.NOT_SPECIFIED, offset,10,sortArgs);
            assertTrue(nodeList != null && nodeList.size() > 0);
        } catch (Exception ex) {

            String message="Exception: "+ex.getMessage();
            logger.warn(message);
            fail(message);
        }
    }

    public void testGetPagedBlastableNodesForUser() {
        List nodeList;
        try {
            User user=userDAO.getUserByName("smurphy");
            assertNotNull(user);
            nodeDAO.getNumBlastableNodesForUser(null, SequenceType.NOT_SPECIFIED, "smurphy");
            SortArgument[] sortArgs = new SortArgument[] {
                    new SortArgument(UserDataNodeVO.SORT_BY_NODE_ID,SortArgument.SORT_DESC),
                    new SortArgument(UserDataNodeVO.SORT_BY_NAME, SortArgument.SORT_ASC),
                    new SortArgument(UserDataNodeVO.SORT_BY_DATE_CREATED, SortArgument.SORT_DESC),
                    new SortArgument(UserDataNodeVO.SORT_BY_TYPE,SortArgument.SORT_ASC),
                    new SortArgument(UserDataNodeVO.SORT_BY_LENGTH,SortArgument.SORT_DESC)
            };
            nodeList=nodeDAO.getPagedBlastableNodesForUser(null, SequenceType.NOT_SPECIFIED,0,10,sortArgs,"smurphy");
            assertTrue(nodeList != null && nodeList.size() > 0);
        } catch (Exception ex) {

            String message="Exception: "+ex.getMessage();
            logger.warn(message);
            fail(message);
        }
    }

    public void testGetPagedBlastHitsByTaskId() {
        try {
            SortArgument[] sortArgs = new SortArgument[] {
                    new SortArgument("rank",SortArgument.SORT_DESC),
                    new SortArgument("bitScore",SortArgument.SORT_ASC),
                    new SortArgument("lengthAlignment",SortArgument.SORT_DESC),
                    new SortArgument("subjectAcc", SortArgument.SORT_ASC),
                    new SortArgument("queryDef",SortArgument.SORT_DESC),
                    new SortArgument("subjectDef", SortArgument.SORT_DESC),
                    new SortArgument("sampleName",SortArgument.SORT_DESC)
            };
            List<Object[]> results = nodeDAO.getPagedBlastHitsByTaskId(1052773497945719111L, 0, -1, false, sortArgs);
            assertTrue(results != null && results.size() == 25);
        } catch (Exception ex) {

            String message="Exception: "+ex.getMessage();
            logger.warn(message);
            fail(message);
        }
    }

    public void testGetTaskOutputNodes() {
        List<Node> nodeList;
        try {
            nodeList=nodeDAO.getTaskOutputNodes(1101226362225557854L,null);
            assertTrue(nodeList != null && nodeList.size() == 2);
            nodeList=nodeDAO.getTaskOutputNodes(1101226362225557854L,BlastResultNode.class);
            assertTrue(nodeList != null && nodeList.size() == 1);
            assertTrue(nodeList.get(0) instanceof BlastResultNode);
        } catch (Exception ex) {

            String message="Exception: "+ex.getMessage();
            logger.warn(message);
            fail(message);
        }
    }

    public void testGetNodeNames() {
        List nodes;
        try {
            ArrayList<Object> nodeList=new ArrayList<Object>();
            nodeList.add(1045153068942885180L);
            nodeList.add("1045126144073924996");
            nodes=nodeDAO.getNodeNames(nodeList);
            assertNotNull(nodes);
            for (Object node : nodes) {
                assertNotNull(node);
            }
        } catch (Exception ex) {

            String message="Exception: "+ex.getMessage();
            logger.warn(message);
            fail(message);
        }
    }

    public void testGetNodeById() {
        Node node;
        try {
            node=nodeDAO.getNodeById(1045153068942885180L);
            assertNotNull(node);
            //assertNotNull(node.getDataType());     deprecated
            //assertTrue(node.getDataType().trim().length()>0);   deprecated
        } catch (Exception ex) {
            
            String message="Exception: "+ex.getMessage();
            logger.warn(message);
            fail(message);
        }        
    }

}
