
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
public class ProteinSearchExportTask extends ExportTask {

    public static final String SEARCH_TASK_ID = "searchTaskId";

    public ProteinSearchExportTask() {
    }

    public ProteinSearchExportTask(String searchTaskId, String exportFormatType, List<String> accessionList,
                                   List<SortArgument> exportAttributeList) {
        super(exportFormatType, accessionList, exportAttributeList);
        setParameter(SEARCH_TASK_ID, searchTaskId);
        setParameter(PARAM_SUGGESTED_FILENAME, formatSuggestedFilename("proteinSearch." + exportFormatType));
        this.taskName = "Protein Search Export Task";
    }

    public String getDataType() {
        return "ProteinSearchResults";
    }

    public String getDisplayName() {
        return "Export Search Results";
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
        if (key.equals(SEARCH_TASK_ID)) {
            Long tmpLong = new Long(value);
            return new LongParameterVO(tmpLong, tmpLong, tmpLong);
        }
        // No match
        return null;
    }


}