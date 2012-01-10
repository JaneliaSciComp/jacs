
package org.janelia.it.jacs.model.tasks.export;

import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.vo.LongParameterVO;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jun 23, 2008
 * Time: 11:20:05 AM
 */
public class FileNodeExportTask extends ExportTask {

    public static final String FILE_NODE_ID = "fileNodeId";

    public FileNodeExportTask() {
    }

    public FileNodeExportTask(String fileNodeId, String exportFormatType,
                              List<String> accessionList,
                              List<SortArgument> exportAttributeList) {
        super(exportFormatType, accessionList, exportAttributeList);
        setParameter(FILE_NODE_ID, fileNodeId);
        setParameter(PARAM_SUGGESTED_FILENAME, formatSuggestedFilename("file_" + fileNodeId + "." + exportFormatType));
        this.taskName = "File Node Export Task";
    }

    public String getDataType() {
        return "FileNodeExport";
    }

    public String getDisplayName() {
        return "Export File Node";
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        ParameterVO pvo = super.getParameterVO(key);
        if (pvo != null)
            return pvo;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(FILE_NODE_ID)) {
            Long tmpLong = new Long(value);
            return new LongParameterVO(tmpLong, tmpLong, tmpLong);
        }
        // No match
        return null;
    }


}