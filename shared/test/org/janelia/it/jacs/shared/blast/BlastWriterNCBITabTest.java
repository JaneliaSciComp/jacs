
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
public class BlastWriterNCBITabTest extends BlastWriterTest {

    static{
        logger = Logger.getLogger(BlastWriterNCBITabTest.class);
    }

    public BlastWriterNCBITabTest() {
        super();
    }

    public void testBlastnNCBITabOutput() throws Exception {
        blastWriterTestImpl(new BlastNTask(),"blastn.xml");
    }

    public void testBlastpNCBITabOutput() throws Exception {
        blastWriterTestImpl(new BlastPTask(),"blastp.xml");
    }

    public void testBlastxNCBITabOutput() throws Exception {
        blastWriterTestImpl(new BlastXTask(),"blastx.xml");
    }

    public void testMegablastNCBITabOutput() throws Exception {
        blastWriterTestImpl(new MegablastTask(),"megablast.xml");
    }

    public void testTblastnNCBITabOutput() throws Exception {
        blastWriterTestImpl(new TBlastNTask(),"tblastn.xml");
    }

    public void testTblastxNCBITabOutput() throws Exception {
        blastWriterTestImpl(new TBlastXTask(),"tblastx.xml");
    }

    protected BlastWriter getBlastWriter() {
        return new BlastNCBITabWriter();
    }

    protected String getOutputFormatKey() {
        return BlastTask.FORMAT_TAB;
    }
}