
package org.janelia.it.jacs.shared.blast;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.tasks.blast.*;

/**
 * User: aresnick
 * Date: May 18, 2009
 * Time: 3:23:48 PM
 * <p/>
 * <p/>
 * Description:
 */
public class BlastWriterBtabTest extends BlastWriterTest {

    static{
        logger = Logger.getLogger(BlastWriterBtabTest.class);
    }

    public BlastWriterBtabTest() {
        super();
    }

    public void testBlastnBtabOutput() throws Exception {
        blastWriterTestImpl(new BlastNTask(),"blastn.xml");
    }

    public void testBlastpBtabOutput() throws Exception {
        blastWriterTestImpl(new BlastPTask(),"blastp.xml");
    }

    public void testBlastxBtabOutput() throws Exception {
        blastWriterTestImpl(new BlastXTask(),"blastx.xml");
    }

    public void testMegablastBtabOutput() throws Exception {
        blastWriterTestImpl(new MegablastTask(),"megablast.xml");
    }

    public void testTblastnBtabOutput() throws Exception {
        blastWriterTestImpl(new TBlastNTask(),"tblastn.xml");
    }

    public void testTblastxBtabOutput() throws Exception {
        blastWriterTestImpl(new TBlastXTask(),"tblastx.xml");
    }

    protected BlastWriter getBlastWriter() {
        return new BlastBtabWriter();
    }

    protected String getOutputFormatKey() {
        return BlastTask.FORMAT_BTAB;
    }
}