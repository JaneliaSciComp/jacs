
package org.janelia.it.jacs.compute.service.blast;

/**
 * Created by IntelliJ IDEA.
 * User: tnabeel
 * Date: Jul 20, 2007
 * Time: 10:37:00 AM
 *
 */
public class BlastTestLarge extends BlastTestBase {

    public BlastTestLarge() {
        super();
    }

    public BlastTestLarge(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
        setBlastDatasetName(GOS_CHESAPEAKE_BAY);
    }

    public void testBlast1kSequence() throws Exception {
        setBlastInputFileName("multi-fasta-1k.fasta");
        submitJobAndWaitForCompletion("TestBlast");
    }

    public void testBlast5kSequence() throws Exception {
        setBlastInputFileName("multi-fasta-5k.fasta");
        submitJobAndWaitForCompletion("TestBlast");
    }

    public void testBlast25kSequence() throws Exception {
        setBlastInputFileName("multi-fasta-25k.fasta");
        submitJobAndWaitForCompletion("TestBlast");
    }

    public void testBlast50kSequence() throws Exception {
        setBlastInputFileName("multi-fasta-50k.fasta");
        submitJobAndWaitForCompletion("TestBlast");
    }

    /**
     * This test produces 49,000 hits which gets persisted to database
     *
     * @throws Exception
     */
    public void testBlastRachel_454_62_7000Sequence() throws Exception {
        setBlastInputFileName("Rachel_454_62_7000.fasta");
        submitJobAndWaitForCompletion("TestBlast");
    }

    public void testBlastRachel_454_62_10000Sequence() throws Exception {
        setBlastInputFileName("Rachel_454_62_10000.fasta");
        submitJobAndWaitForCompletion("TestBlast");
    }

    public void testBlastRachel_454_62Sequence() throws Exception {
        setBlastInputFileName("Rachel_454_62.fasta");
        submitJobAndWaitForCompletion("TestBlast");
    }

    // NOTE: when this test is run, blastx.parameters must be used.
    // This requirement needs to be removed and the testing apparatus
    // upgraded to switch these values by blast type automatically.
    public void testBlastBlastxCase() throws Exception {
        this.setBlastDatasetName("All Prokaryotic Proteins (P)");
        setBlastInputFileName("TestBlastxAllProkSingleAlign.fasta");
        submitJobAndWaitForCompletion("TestBlast");
    }

//    public void testTooManyJobs() throws Exception {
//        setBlastDatasetName("All Metagenomic Sequence Reads (N)");
//        setBlastInputFileName("nucleotide.fasta");
//        blastProperties.put(BlastRunner.PARAM_DB_ALIGNMENTS,"2631");
//        submitJobAndWaitForCompletion("TestBlast");
//    }
}
