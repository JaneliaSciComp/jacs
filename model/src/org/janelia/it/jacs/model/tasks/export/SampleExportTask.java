
package org.janelia.it.jacs.model.tasks.export;

import org.janelia.it.jacs.model.common.SortArgument;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jun 23, 2008
 * Time: 11:20:05 AM
 */
public class SampleExportTask extends ExportTask {

    public SampleExportTask() {
    }

    public SampleExportTask(String exportFormatType, List<String> accessionList,
                            List<SortArgument> exportAttributeList) {
        super(exportFormatType, accessionList, exportAttributeList);
        setParameter(PARAM_SUGGESTED_FILENAME, formatSuggestedFilename("Sample_reads_" + accessionList.get(0) +
                "." + exportFormatType));
        this.taskName = "Sample Export Task";
    }

    public String getDataType() {
        return "Samples";
    }

    public String getDisplayName() {
        return "Export Samples";
    }
}