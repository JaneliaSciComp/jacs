
package org.janelia.it.jacs.model.tasks.export;

import org.janelia.it.jacs.model.common.SortArgument;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jun 23, 2008
 * Time: 11:20:05 AM
 */
public class SequenceExportTask extends ExportTask {

    public SequenceExportTask() {
    }

    public SequenceExportTask(String exportFormatType, List<String> accessionList,
                              List<SortArgument> exportAttributeList) {
        super(exportFormatType, accessionList, exportAttributeList);
        String filename = "sequence";
        if (accessionList != null && accessionList.get(0) != null)
            filename += ("_" + accessionList.get(0));
        setParameter(PARAM_SUGGESTED_FILENAME, formatSuggestedFilename(filename + "." + exportFormatType));
        this.taskName = "Sequence Export Task";
    }

    public String getDataType() {
        return "Sequences";
    }

    public String getDisplayName() {
        return "Export Sequences";
    }
}