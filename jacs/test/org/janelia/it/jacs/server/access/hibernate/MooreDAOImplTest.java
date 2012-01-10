
package org.janelia.it.jacs.server.access.hibernate;

import org.janelia.it.jacs.model.download.MooreOrganism;
import org.janelia.it.jacs.server.access.MooreDAO;
import org.janelia.it.jacs.test.JacswebTestCase;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Jan 29, 2008
 * Time: 3:57:19 PM
 *
 */
public class MooreDAOImplTest extends JacswebTestCase {
    private MooreDAO mooreDAO;

    public MooreDAOImplTest() {
        super(MooreDAOImplTest.class.getName());
    }

    public void setMooreDAO(MooreDAO mooreDAO) {
        this.mooreDAO=mooreDAO;
    }

    public MooreDAO getMooreDAO() {
        return mooreDAO;
    }

    public void testFindAllOrganisms() {
        List<MooreOrganism> organismList;
        try {
            organismList= mooreDAO.findAllOrganisms();
            assertNotNull(organismList);
            assertTrue("organismList size must be at least 168",organismList.size()>160);
        } catch (Exception ex) {
            failFromException(ex);
        }
    }

}
