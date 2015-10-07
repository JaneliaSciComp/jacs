
package org.janelia.it.jacs.compute.service.export;

import org.janelia.it.jacs.model.tasks.export.ClusterProteinAnnotationExportTask;
import org.janelia.it.jacs.shared.export.ExportWriterConstants;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Jul 10, 2008
 * Time: 4:26:52 PM
 *
 */
public class ExportTestClusterAnnotation extends ExportTestBase {

    public ExportTestClusterAnnotation() {
        super();
    }

    public ExportTestClusterAnnotation(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testClusterAnnotationFastaExport() throws Exception {
        ArrayList<String> accessionList=new ArrayList<String>();
        accessionList.add("CAM_CL_647");
        String annotationID="CAM_CNAM_57077";
        ClusterProteinAnnotationExportTask task = new ClusterProteinAnnotationExportTask(
                annotationID,
                false /* is NR only */,
                ExportWriterConstants.EXPORT_TYPE_FASTA,
                accessionList,
                null // SortArgument list not necessary
        );
        task.setOwner(TEST_USER_NAME);
        submitJobAndWaitForCompletion("FileExport", task);
    }

}
