
package org.janelia.it.jacs.compute.service.blast;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Created by IntelliJ IDEA.
 * User: tnabeel
 * Date: Apr 19, 2007
 * Time: 2:58:05 PM
 *
 */
public class BlastTestMultiPartitionQDataNode extends BlastTestBase {

    private static final Long EXPECTED_HITS = 496L;

    public BlastTestMultiPartitionQDataNode() {
        super();
    }

    public BlastTestMultiPartitionQDataNode(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
        setExpectedHits(EXPECTED_HITS);
        setBlastInputFileName("clean-multi-fasta-32.fasta");
        setBlastDatasetName(MARINE_VIROMES);
    }

    public void testBlast32Seq() throws Exception {
        submitJobAndWaitForCompletion("TestBlast");
        validateHits();
    }

    public void testAllPojoBlast32Seq() throws Exception {
        submitJobAndWaitForCompletion("TestAllPojoBlast");
        validateHits();
    }

    public void testBaseServiceMDB32Seq() throws Exception {
        submitJobAndWaitForCompletion("TestBaseServiceMDB");
        validateHits();
    }

    public void testAllSlsbBlast32Seq() throws Exception {
        submitJobAndWaitForCompletion("TestAllSlsbBlast");
        validateHits();
    }

    public void testAllSlbBlastLaunchedByBlastLauncherMdb32Seq() throws Exception {
        submitJobAndWaitForCompletion("TestAllSlbBlastLaunchedByBlastLauncherMdb");
        validateHits();
    }

    public void testAllPojoBlastLaunchedByBlastLauncherMdb32Seq() throws Exception {
        submitJobAndWaitForCompletion("TestAllPojoBlastLaunchedByBlastLauncherMdb");
        validateHits();
    }

    public void testAllPojoBlastLaunchedByProcessLauncherMdb32Seq() throws Exception {
        submitJobAndWaitForCompletion("TestAllPojoBlastLaunchedByProcessLauncherMdb");
        validateHits();
    }

    public void testAllPojoBlastLaunchedBySequenceLauncherMdb32Seq() throws Exception {
        submitJobAndWaitForCompletion("TestAllPojoBlastLaunchedBySequenceLauncherMdb");
        validateHits();
    }

    public void testAllMdbBlastControlledByProcessLauncherSlsb32Seq() throws Exception {
        submitJobAndWaitForCompletion("TestAllMdbBlastControlledByProcessLauncherSlsb");
        validateHits();
    }

    public void testAllMdbBlastControlledByProcessLauncherPojo32Seq() throws Exception {
        submitJobAndWaitForCompletion("TestAllMdbBlastControlledByProcessLauncherPojo");
        validateHits();
    }

    public void testAllMdbBlastControlledByProcessLauncherMdb32Seq() throws Exception {
        submitJobAndWaitForCompletion("TestAllMdbBlastControlledByProcessLauncherMdb");
        validateHits();
    }


    public void testMdbControlledBlast32Seq() throws Exception {
        submitJobAndWaitForCompletion("TestMdbControlledBlast");
        validateHits();
    }

    public void testSequenceMessageLinking32Seq() throws Exception {
        submitJobAndWaitForCompletion("TestSequenceMessageLinking");
        validateHits();
    }

    public void testMixSlsbAndMdbBlast32Seq() throws Exception {
        submitJobAndWaitForCompletion("TestMixSlsbAndMdbBlast");
        validateHits();
    }

    public void testAllSlbBlastUsingSequenceLauncherMdbNoWait32Seq() throws Exception {
        submitJobAndWaitForCompletion("TestAllSlbBlastUsingSequenceLauncherMdbNoWait");
        validateHits();
    }

    public void testPersistBlastInSeparateLauncherMDB32Seq() throws Exception {
        submitJobAndWaitForCompletion("TestPersistBlastInSeparateLauncherMDB");
        validateHits();
    }

    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(BlastTestMultiPartitionQDataNode.class);
        return suite;
    }
}
