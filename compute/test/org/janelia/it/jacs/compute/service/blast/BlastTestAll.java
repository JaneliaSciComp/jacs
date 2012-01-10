
package org.janelia.it.jacs.compute.service.blast;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Created by IntelliJ IDEA.
 * User: tnabeel
 * Date: Apr 23, 2007
 * Time: 6:56:42 PM
 *
 */
public class BlastTestAll extends TestCase {

    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(BlastTestErrors.class);
        suite.addTestSuite(BlastTestBasic.class);
        suite.addTestSuite(BlastTestMultiFasta.class);
        suite.addTestSuite(BlastTestMultiFastaXMLPersist.class);
        suite.addTestSuite(BlastTestMultiPartitionQDataNode.class);
        return suite;
    }


}
