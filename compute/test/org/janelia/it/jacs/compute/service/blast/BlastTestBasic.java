
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
public class BlastTestBasic extends BlastTestBase {

    private static final Long EXPECTED_HITS = 18L;

    public BlastTestBasic() {
        super();
    }

    public BlastTestBasic(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
        setExpectedHits(EXPECTED_HITS);
        setBlastInputFileName("test.fasta");
        setBlastDatasetName(GOS_CHESAPEAKE_BAY);
    }


    public void testBlast() throws Exception {
        submitJobAndWaitForCompletion("TestBlast");
        validateHits();
    }

    public void testAllPojoBlast() throws Exception {
        submitJobAndWaitForCompletion("TestAllPojoBlast");
        validateHits();
    }

    public void testBaseServiceMDB() throws Exception {
        submitJobAndWaitForCompletion("TestBaseServiceMDB");
        validateHits();
    }

//    public void testSelfContainedBlast() throws Exception {
//        submitSelfContainedBlastAndWaitForCompletion("TestSelfContainedBlast");
//        validateHits();
//    }

    public void testAllSlsbBlast() throws Exception {
        submitJobAndWaitForCompletion("TestAllSlsbBlast");
        validateHits();
    }

    public void testAllSlbBlastLaunchedByBlastLauncherMdb() throws Exception {
        submitJobAndWaitForCompletion("TestAllSlbBlastLaunchedByBlastLauncherMdb");
        validateHits();
    }

    public void testAllPojoBlastLaunchedByBlastLauncherMdb() throws Exception {
        submitJobAndWaitForCompletion("TestAllPojoBlastLaunchedByBlastLauncherMdb");
        validateHits();
    }

    public void testAllPojoBlastLaunchedByProcessLauncherMdb() throws Exception {
        submitJobAndWaitForCompletion("TestAllPojoBlastLaunchedByProcessLauncherMdb");
        validateHits();
    }

    public void testAllPojoBlastLaunchedBySequenceLauncherMdb() throws Exception {
        submitJobAndWaitForCompletion("TestAllPojoBlastLaunchedBySequenceLauncherMdb");
        validateHits();
    }

    public void testAllMdbBlastControlledByProcessLauncherSlsb() throws Exception {
        submitJobAndWaitForCompletion("TestAllMdbBlastControlledByProcessLauncherSlsb");
        validateHits();
    }

    public void testAllMdbBlastControlledByProcessLauncherPojo() throws Exception {
        submitJobAndWaitForCompletion("TestAllMdbBlastControlledByProcessLauncherPojo");
        validateHits();
    }

    public void testAllMdbBlastControlledByProcessLauncherMdb() throws Exception {
        submitJobAndWaitForCompletion("TestAllMdbBlastControlledByProcessLauncherMdb");
        validateHits();
    }

    public void testMdbControlledBlast() throws Exception {
        submitJobAndWaitForCompletion("TestMdbControlledBlast");
        validateHits();
    }

    public void testSequenceMessageLinking() throws Exception {
        submitJobAndWaitForCompletion("TestSequenceMessageLinking");
        validateHits();
    }

    public void testMixSlsbAndMdbBlast() throws Exception {
        submitJobAndWaitForCompletion("TestMixSlsbAndMdbBlast");
        validateHits();
    }

    public void testAllSlbBlastUsingSequenceLauncherMdbNoWait() throws Exception {
        submitJobAndWaitForCompletion("TestAllSlbBlastUsingSequenceLauncherMdbNoWait");
        validateHits();
    }

    public void testPersistBlastInSeparateLauncherMDB() throws Exception {
        submitJobAndWaitForCompletion("TestPersistBlastInSeparateLauncherMDB");
        validateHits();
    }

    public static Test suite() {
        TestSuite suite = new TestSuite();
//        suite.addTest(new BlastTestBasic("testBlast"));
        suite.addTestSuite(BlastTestBasic.class);
        return suite;
    }

}
