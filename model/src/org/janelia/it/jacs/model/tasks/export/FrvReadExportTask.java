
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
public class FrvReadExportTask extends ExportTask {

    public static final String RECRUITMENT_RESULT_FILE_NODE_ID = "recruitmentResultsFileNodeId";
    public static final String RECRUITMENT_TASK_ID = "recruitmentTaskId";
    public static final String START_BP_POSITION = "startBPPosition";
    public static final String END_BP_POSITION = "endBPPosition";
    public static final String START_PCT_ID_POSITION = "startPctId";
    public static final String END_PCT_ID_POSITION = "endPctId";

    public FrvReadExportTask() {
    }

    public FrvReadExportTask(String recruitmentResultsFileNodeId, String recruitmentTaskId, long startBPPosition,
                             long endBPPosition, int startPctId, int endPctId,
                             String exportFormatType, List<String> accessionList,
                             List<SortArgument> exportAttributeList) {
        super(exportFormatType, accessionList, exportAttributeList);
        this.taskName = "Frv Read Export Task";
        setParameter(RECRUITMENT_RESULT_FILE_NODE_ID, recruitmentResultsFileNodeId);
        setParameter(RECRUITMENT_TASK_ID, recruitmentTaskId);
        setParameter(START_BP_POSITION, Long.toString(startBPPosition));
        setParameter(END_BP_POSITION, Long.toString(endBPPosition));
        setParameter(START_PCT_ID_POSITION, Integer.toString(startPctId));
        setParameter(END_PCT_ID_POSITION, Integer.toString(endPctId));
        setParameter(PARAM_SUGGESTED_FILENAME,
                "frv-" + recruitmentTaskId + "-seqs-" + startBPPosition + "-" + endBPPosition + "." + exportFormatType);
        this.taskName = "Frv Read Export Task";
    }

    public String getDataType() {
        return "FrvReads";
    }

    public String getDisplayName() {
        return "Export FRV Reads";
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
        if (key.equals(RECRUITMENT_RESULT_FILE_NODE_ID)) {
            Long tmpLong = new Long(value);
            return new LongParameterVO(tmpLong, tmpLong, tmpLong);
        }
        if (key.equals(RECRUITMENT_TASK_ID)) {
            Long tmpLong = new Long(value);
            return new LongParameterVO(tmpLong, tmpLong, tmpLong);
        }
        if (key.equals(START_BP_POSITION)) {
            Long tmpLong = new Long(value);
            return new LongParameterVO(tmpLong, tmpLong, tmpLong);
        }
        if (key.equals(END_BP_POSITION)) {
            Long tmpLong = new Long(value);
            return new LongParameterVO(tmpLong, tmpLong, tmpLong);
        }
        if (key.equals(START_PCT_ID_POSITION)) {
            Long tmpLong = new Long(value);
            return new LongParameterVO(tmpLong, tmpLong, tmpLong);
        }
        if (key.equals(END_PCT_ID_POSITION)) {
            Long tmpLong = new Long(value);
            return new LongParameterVO(tmpLong, tmpLong, tmpLong);
        }
        // No match
        return null;
    }


}