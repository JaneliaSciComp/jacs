
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
public class BlastWriterNCBITabWithHeaderTest extends BlastWriterTest {

    static{
        logger = Logger.getLogger(BlastWriterNCBITabWithHeaderTest.class);
    }

    public BlastWriterNCBITabWithHeaderTest() {
        super();

    }

    public void testBlastnNCBITabWithHeaderOutput() throws Exception {
        blastWriterTestImpl(new BlastNTask(),"blastn.xml");
    }

    public void testBlastpNCBITabWithHeaderOutput() throws Exception {
        blastWriterTestImpl(new BlastPTask(),"blastp.xml");
    }

    public void testBlastxNCBITabWithHeaderOutput() throws Exception {
        blastWriterTestImpl(new BlastXTask(),"blastx.xml");
    }

    public void testMegablastNCBITabWithHeaderOutput() throws Exception {
        blastWriterTestImpl(new MegablastTask(),"megablast.xml");
    }

    public void testTblastnNCBITabWithHeaderOutput() throws Exception {
        blastWriterTestImpl(new TBlastNTask(),"tblastn.xml");
    }

    public void testTblastxNCBITabWithHeaderOutput() throws Exception {
        blastWriterTestImpl(new TBlastXTask(),"tblastx.xml");
    }

    protected BlastWriter getBlastWriter() {
        return new BlastNCBITabWithHeaderWriter();
    }

    protected String getOutputFormatKey() {
        return BlastTask.FORMAT_TAB_WITH_HEADER;
    }
}