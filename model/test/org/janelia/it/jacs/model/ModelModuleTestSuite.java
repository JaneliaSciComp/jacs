
package org.janelia.it.jacs.model;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.janelia.it.jacs.model.entity.DataSetTest;
import org.janelia.it.jacs.model.entity.EntityTypeTest;
import org.janelia.it.jacs.model.genomics.SeqUtilTest;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Feb 2, 2007
 * Time: 4:55:50 PM
 *
 */
public class ModelModuleTestSuite extends TestSuite {

    /**
     * Developers are supposed to add their unit tests to this suite() method
     * in order to be automatically executed by CruiseControl
     * @return suite of all test classes.
     */
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(DataSetTest.class);
        suite.addTestSuite(EntityTypeTest.class);
        suite.addTestSuite(SeqUtilTest.class);
        return suite;
    }


    /**
     * Main method which gets executed by CruiseControl
     * @param args not used.
     */
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
}
