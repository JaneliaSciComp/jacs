
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
public class BlastTestMultiFasta extends BlastTestBase {

    private static final Long EXPECTED_HITS = 580L;

    public BlastTestMultiFasta() {
        super();
    }

    public BlastTestMultiFasta(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
        setExpectedHits(EXPECTED_HITS);
        setBlastInputFileName("clean-multi-fasta-32.fasta");
        setBlastDatasetName(GOS_CHESAPEAKE_BAY);
    }

    public void testJira526Fix() throws Exception {
        setExpectedHits(695874L);
        setBlastInputFileName("Jira526FixInput.fasta");
        submitJobAndWaitForCompletion("BlastWithGridMerge");
        validateHits();
        validateMessageCount();
    }

    public void testBlastQueryCountOne() throws Exception {
        setExpectedHits(9L);
        setBlastInputFileName("clean-multi-fasta-1.fasta");
        submitJobAndWaitForCompletion("TestBlast");
        validateHits();
    }

    public void testBlastQueryCountTwo() throws Exception {
        setExpectedHits(15L);
        setBlastInputFileName("clean-multi-fasta-2.fasta");
        submitJobAndWaitForCompletion("TestBlastXmlOut");
        validateHits();
    }

    public void testBlastHitsNoHits() throws Exception {
        setExpectedHits(15L);
        setBlastInputFileName("multi-fasta-2-hits-nohits.fasta");
        submitJobAndWaitForCompletion("TestBlastXmlOut");
        validateHits();
    }

    public void testBlastNoHitsHits() throws Exception {
        setExpectedHits(12L);
        setBlastInputFileName("multi-fasta-2-nohits-hits.fasta");
        submitJobAndWaitForCompletion("TestBlastXmlOut");
        validateHits();
    }

    public void testBlastHitsNoHitsHits() throws Exception {
        setExpectedHits(37L);
        setBlastInputFileName("multi-fasta-3-nohits-hits-hits.fasta");
        submitJobAndWaitForCompletion("TestBlastXmlOut");
        validateHits();
    }

    public void testBlastHitsHitsNoHits() throws Exception {
        setExpectedHits(21L);
        setBlastInputFileName("multi-fasta-3-hits-hits-nohits.fasta");
        submitJobAndWaitForCompletion("TestBlastXmlOut");
        validateHits();
    }

    public void testBlastNoHitsHitsHits() throws Exception {
        setExpectedHits(37L);
        setBlastInputFileName("multi-fasta-3-nohits-hits-hits.fasta");
        submitJobAndWaitForCompletion("TestBlastXmlOut");
        validateHits();
    }

    public void testBlast32Seq() throws Exception {
        submitJobAndWaitForCompletion("TestBlast");
        validateHits();
    }

    public void testBlast32Seq8Partitions() throws Exception {
        setExpectedHits(496L);
        setBlastDatasetName(MARINE_VIROMES);
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
        suite.addTestSuite(BlastTestMultiFasta.class);
        return suite;
    }
}
