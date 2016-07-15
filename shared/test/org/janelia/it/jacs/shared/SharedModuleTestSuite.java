
package org.janelia.it.jacs.shared;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import org.janelia.it.jacs.shared.blast.BlastGridMergeSortTest;
import org.janelia.it.jacs.shared.blast.BlastWriterBtabTest;
import org.janelia.it.jacs.shared.blast.BlastWriterNCBITabTest;
import org.janelia.it.jacs.shared.blast.BlastWriterNCBITabWithHeaderTest;
import org.janelia.it.jacs.shared.blast.blastxmlparser.BlastXMLParserTest;
import org.janelia.it.jacs.shared.blast.blastxmlparser.BlastXMLWriterTest;
import org.janelia.it.jacs.shared.fasta.FASTAFileNodeHelperTest;
import org.janelia.it.jacs.shared.utils.FileUtilTest;
import org.janelia.it.jacs.shared.utils.IdGeneratorTest;
import org.janelia.it.jacs.shared.utils.StringUtilsTest;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jan 31, 2007
 * Time: 1:39:09 PM
 *
 */
public class SharedModuleTestSuite extends TestSuite {

    public static Test suite() {
        TestSuite suite = new TestSuite();
        // Works
        suite.addTestSuite(StringUtilsTest.class);
        suite.addTestSuite(FileUtilTest.class);
        suite.addTestSuite(IdGeneratorTest.class);
        suite.addTestSuite(FASTAFileNodeHelperTest.class);
        suite.addTestSuite(BlastGridMergeSortTest.class);
        suite.addTestSuite(BlastXMLParserTest.class);
        suite.addTestSuite(BlastXMLWriterTest.class);
        suite.addTestSuite(BlastWriterBtabTest.class);
        suite.addTestSuite(BlastWriterNCBITabTest.class);
        suite.addTestSuite(BlastWriterNCBITabWithHeaderTest.class);
        return suite;
    }


    public static void main(String[] args) {
        TestRunner.run(suite());
    }
}
