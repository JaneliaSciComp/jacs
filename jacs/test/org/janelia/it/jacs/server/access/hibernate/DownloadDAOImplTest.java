
package org.janelia.it.jacs.server.access.hibernate;

import org.janelia.it.jacs.model.download.Project;
import org.janelia.it.jacs.model.metadata.Sample;
import org.janelia.it.jacs.server.access.DownloadDAO;
import org.janelia.it.jacs.test.JacswebTestCase;

import java.util.Iterator;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 7, 2007
 * Time: 10:48:04 AM
 *
 */
public class DownloadDAOImplTest extends JacswebTestCase {

    private DownloadDAO downloadDAO;

    public DownloadDAO getDownloadDAO() {
        return downloadDAO;
    }

    public void setDownloadDAO(DownloadDAO downloadDAO) {
        this.downloadDAO = downloadDAO;
    }

    public DownloadDAOImplTest() {
        super(DownloadDAOImplTest.class.getName());
    }

    public void testFindAllProjects() {
        List projectList=null;
        try {
            projectList=downloadDAO.findAllProjects();
            assertTrue(projectList.size()>0);
            Iterator iter=projectList.iterator();
            while(iter.hasNext()) {
                Project project=(Project)iter.next();
                assertNotNull(project);
            }
        } catch (Exception ex) {
            String message="Exception: "+ex.getMessage();
            logger.warn(message);
            fail(message);
        }
    }

    public void testFindProjectSamples() {
         List sampleList=null;
        try {
            sampleList=downloadDAO.findProjectSamples("CAM_PROJ_GOS");
            assertTrue(sampleList.size()>0);
            Iterator iter=sampleList.iterator();
            while(iter.hasNext()) {
                Sample sample=(Sample)iter.next();
                assertNotNull(sample);
                assertNotNull(sample.getSampleName());
            }
        } catch (Exception ex) {
            String message="Exception: "+ex.getMessage();
            logger.warn(message);
            fail(message);
        }
    }

}
