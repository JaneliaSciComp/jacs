
package org.janelia.it.jacs.compute.service.export;

import org.janelia.it.jacs.model.tasks.export.ClusterSearchExportTask;
import org.janelia.it.jacs.model.tasks.export.ProteinSearchExportTask;
import org.janelia.it.jacs.shared.export.ExportWriterConstants;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Jul 14, 2008
 * Time: 2:46:57 PM
 *
 */
public class ExportTestSearch extends ExportTestBase {

    public ExportTestSearch() {
        super();
    }

    public ExportTestSearch(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testProteinSearchFastaExport() throws Exception {
        ArrayList<String> accessionList=new ArrayList<String>();
        ProteinSearchExportTask task = new ProteinSearchExportTask(
                "1234645401051595385",
                ExportWriterConstants.EXPORT_TYPE_FASTA,
                accessionList,
                null // SortArgument list not necessary
        );
        task.setOwner(TEST_USER_NAME);
        submitJobAndWaitForCompletion("FileExport", task);
    }

    public void testClusterSearchCsvExport() throws Exception {
        ArrayList<String> accessionList=new ArrayList<String>();
        ClusterSearchExportTask task=new ClusterSearchExportTask(
               "1234645401051595385",
                ExportWriterConstants.EXPORT_TYPE_CSV,
                accessionList,
                null // SortArgument list not necessary
        );
        task.setOwner(TEST_USER_NAME);
        submitJobAndWaitForCompletion("FileExport", task);
    }

}
