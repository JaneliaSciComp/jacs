
package org.janelia.it.jacs.server.access.hibernate;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.server.access.FileNodeDAO;
import org.janelia.it.jacs.test.JacswebTestCase;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 8, 2007
 * Time: 10:42:38 AM
 *
 */
public class FileNodeDAOImplTest extends JacswebTestCase {

    private FileNodeDAO fileNodeDAO;

    public FileNodeDAO getFileNodeDAO() {
        return fileNodeDAO;
    }

    public void setFileNodeDAO(FileNodeDAO featureDAO) {
        this.fileNodeDAO = featureDAO;
    }

    public FileNodeDAOImplTest() {
        super(FileNodeDAOImplTest.class.getName());
    }

    public void testFileNodeDAO() {
        assertNotNull(fileNodeDAO);
    }

    public void testGetFileNodeById() {
        FileNode fileNode=null;
        try {
            fileNode=fileNodeDAO.getFileNodeById(1037841355520344417L);
            assertNotNull(fileNode);
            Task task=fileNode.getTask();
            assertNotNull(task);
            assertEquals(task.getObjectId(),new Long(1037841351363789131L));
            String user=fileNode.getOwner();
            assertNotNull(user);
            assertEquals(user,"leonid");
        } catch (Exception ex) {
            String message="Exception: "+ex.getMessage();
            logger.warn(message);
            fail(message);
        }
    }

    public void testGetResultFileNodesByTaskId() {
        FileNode[] fileNodeArr=null;
        try {
            fileNodeArr=fileNodeDAO.getResultFileNodesByTaskId(1037841351363789131L);
            assertNotNull(fileNodeArr);
            assertTrue(fileNodeArr.length>0);
            assertTrue(fileNodeArr[0].getObjectId()==1037841355520344417L);
        } catch (Exception ex) {
            String message="Exception: "+ex.getMessage();
            logger.warn(message);
            fail(message);
        }
    }

}
