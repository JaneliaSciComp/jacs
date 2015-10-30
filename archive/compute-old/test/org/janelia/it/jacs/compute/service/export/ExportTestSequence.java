
package org.janelia.it.jacs.compute.service.export;

import org.janelia.it.jacs.model.tasks.export.SequenceExportTask;
import org.janelia.it.jacs.shared.export.ExportWriterConstants;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Jul 9, 2008
 * Time: 12:05:08 PM
 *
 */
public class ExportTestSequence extends ExportTestBase {

    public ExportTestSequence() {
        super();
    }

    public ExportTestSequence(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testSequenceFastaExportWithCompression() throws Exception {
        ArrayList<String> accessionList=new ArrayList<String>();
        accessionList.add("JCVI_NT_1105010130702");
        SequenceExportTask task = new SequenceExportTask(
                ExportWriterConstants.EXPORT_TYPE_FASTA,
                accessionList,
                null // SortArgument list not necessary
        );
        task.setSuggestedCompressionType(ExportWriterConstants.COMPRESSION_ZIP);
        task.setOwner(TEST_USER_NAME);
        submitJobAndWaitForCompletion("FileExport", task);
    }
    
}
