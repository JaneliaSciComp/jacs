
package org.janelia.it.jacs.shared.blast.blastxmlparser;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.tasks.blast.*;
import org.janelia.it.jacs.shared.blast.BlastWriter;
import org.janelia.it.jacs.shared.blast.BlastWriterTest;

/**
 * User: aresnick
 * Date: May 29, 2009
 * Time: 4:13:31 PM
 * <p/>
 * <p/>
 * Description:
 */
public class BlastXMLWriterTest extends BlastWriterTest {

    static{
        logger = Logger.getLogger(BlastXMLWriterTest.class);
    }

    public BlastXMLWriterTest() {
        super();
    }

    public void testBlastnXMLOutput() throws Exception {
        blastWriterTestImpl(new BlastNTask(), "blastn.xml");
    }

    public void testBlastpXMLOutput() throws Exception {
        blastWriterTestImpl(new BlastPTask(), "blastp.xml");
    }

    public void testBlastxXMLOutput() throws Exception {
        blastWriterTestImpl(new BlastXTask(), "blastx.xml");
    }

    public void testMegablastXMLOutput() throws Exception {
        blastWriterTestImpl(new MegablastTask(), "megablast.xml");
    }

    public void testTblastnXMLOutput() throws Exception {
        blastWriterTestImpl(new TBlastNTask(), "tblastn.xml");
    }

    public void testTblastxXMLOutput() throws Exception {
        blastWriterTestImpl(new TBlastXTask(), "tblastx.xml");
    }

    protected BlastWriter getBlastWriter() throws Exception {
        return new BlastXMLWriter();
    }

    protected String getOutputFormatKey() {
        return BlastTask.FORMAT_XML;
    }
}
