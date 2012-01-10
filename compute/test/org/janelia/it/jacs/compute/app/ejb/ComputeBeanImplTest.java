
package org.janelia.it.jacs.compute.app.ejb;

import org.janelia.it.jacs.compute.ComputeTestCase;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 9, 2007
 * Time: 4:19:39 PM
 *
 */
public class ComputeBeanImplTest extends ComputeTestCase {
    ComputeBeanRemote computeBean;

    public ComputeBeanImplTest() {
        super(ComputeBeanImplTest.class.getName());
    }

    public void setUp() throws Exception {
        super.setUp();
        computeBean= EJBFactory.getRemoteComputeBean();
    }

    public void testComputeBean() {
        if (computeBean==null)
            fail("ComputeBeanImpl is null");
    }

}
