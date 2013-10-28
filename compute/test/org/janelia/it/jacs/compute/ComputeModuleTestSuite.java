
package org.janelia.it.jacs.compute;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.janelia.it.jacs.compute.access.ComputeDAOTest;
import org.janelia.it.jacs.compute.access.SageDAOTest;
import org.janelia.it.jacs.compute.app.ejb.ComputeBeanImplTest;
import org.janelia.it.jacs.compute.service.blast.*;
import org.janelia.it.jacs.compute.service.search.AccessionSearcherTest;
import org.janelia.it.jacs.compute.service.search.JacsAccessionSearchResultBuilderTest;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jan 31, 2007
 * Time: 1:39:09 PM
 *
 */
public class ComputeModuleTestSuite extends TestSuite {

    public static Test suite() {
        TestSuite suite = new TestSuite();
        //  BlastTestAll is a suite, not a test. SEE MEMBERS BELOW:
        //suite.addTestSuite(BlastTestAll.class);
        // begin BlastTestAll members
        suite.addTestSuite(BlastTestErrors.class);
        suite.addTestSuite(BlastTestBasic.class);
        suite.addTestSuite(BlastTestMultiFasta.class);
        suite.addTestSuite(BlastTestMultiFastaXMLPersist.class);
        suite.addTestSuite(BlastTestMultiPartitionQDataNode.class);
        // end BlastTestAll members
        suite.addTestSuite(ComputeBeanImplTest.class);
        suite.addTestSuite(ComputeDAOTest.class);
        suite.addTestSuite(JacsAccessionSearchResultBuilderTest.class);
        suite.addTestSuite(AccessionSearcherTest.class);

        suite.addTestSuite(SageDAOTest.class);
        return suite;
    }


    public static void main(String[] args) {
        junit.textui.TestRunner.run(ComputeModuleTestSuite.suite());
    }
}
