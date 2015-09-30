
package org.janelia.it.jacs.compute.service.export.model;

import org.janelia.it.jacs.compute.access.search.ClusterResult;
import org.janelia.it.jacs.compute.service.export.writers.ExportWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Aug 1, 2008
 * Time: 10:20:13 AM
 */
public class ClusterResultWriter {
    ExportWriter exportWriter;
    List<ClusterResult> clusterResults;

    public ClusterResultWriter(ExportWriter exportWriter, List<ClusterResult> clusterResults) {
        this.exportWriter = exportWriter;
        this.clusterResults = clusterResults;
    }

    public void write() throws IOException {
        List<String> headerList = new ArrayList<String>();
        headerList.addAll(ClusterResultFormatter.getHeaderList());
        exportWriter.writeItem(headerList);
        if (clusterResults == null || clusterResults.size() == 0)
            return; // nothing to do
        for (ClusterResult cr : clusterResults) {
            List<String> colList = ClusterResultFormatter.formatColumns(cr);
            exportWriter.writeItem(colList);
        }
    }

}
