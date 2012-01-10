
package org.janelia.it.jacs.model.tasks.metageno;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.*;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 13, 2009
 * Time: 1:10:40 PM
 */
public class MetaGenoOrfCallerTask extends Task {


    /*

        Example web interface:
        
        public String runMetaGenoOrfCaller(@WebParam(name = "username") String username,
                                       @WebParam(name = "token") String token,
                                       @WebParam(name = "project") String project,
                                       @WebParam(name = "workSessionId") String workSessionId,
                                       @WebParam(name = "jobName") String jobName,
                                       @WebParam(name = "inputFastaFileNodeId") String inputFastaFileNodeId,
                                       @WebParam(name = "useClearRange") String useClearRange,
                                       @WebParam(name = "translationTable") String translationTable,
                                       @WebParam(name = "beginningAsStart") String beginningAsStart,
                                       @WebParam(name = "endAsStop") String endAsStop,
                                       @WebParam(name = "aassumeStops") String assumeStops,
                                       @WebParam(name = "fullOrfs") String fullOrfs,
                                       @WebParam(name = "minOrfSize") String minOrfSize,
                                       @WebParam(name = "maxOrfSize") String maxOrfSize,
                                       @WebParam(name = "minUnmaskedSize") String minUnmaskedSize,
                                       @WebParam(name = "frames") String frames,
                                       @WebParam(name = "forceMethionine") String forceMethionine,
                                       @WebParam(name="clearRangeMinOrfSize")String clearRangeMinOrfSize
                                       )
    */

    transient public static final String PARAM_input_node_id = "Query node id";
    transient public static final String PARAM_useClearRange = "Use clear range";
    transient public static final String PARAM_translationTable = "Translation Table";
    transient public static final String PARAM_beginningAsStart = "Beginning as start";
    transient public static final String PARAM_endAsStop = "End as stop";
    transient public static final String PARAM_assumeStops = "Assume stops";
    transient public static final String PARAM_fullOrfs = "Full orfs";
    transient public static final String PARAM_minOrfSize = "Min orf size";
    transient public static final String PARAM_maxOrfSize = "Max orf size";
    transient public static final String PARAM_minUnmaskedSize = "Min unmasked size";
    transient public static final String PARAM_frames = "Frames";
    transient public static final String PARAM_forceMethionine = "Force methinonine";
    transient public static final String PARAM_clearRangeMinOrfSize = "Clear range min orf size";

    transient public static final Boolean useClearRange_DEFAULT = Boolean.FALSE;
    transient public static final String translationTable_DEFAULT = "11";
    transient public static final String beginningAsStart_DEFAULT = "1";
    transient public static final String endAsStop_DEFAULT = "1";
    transient public static final String assumeStops_DEFAULT = "0";
    transient public static final String fullOrfs_DEFAULT = "0";
    transient public static final int minOrfSize_DEFAULT = 180;
    transient public static final int maxOrfSize_DEFAULT = 0;
    transient public static final int minUnmaskedSize_DEFAULT = 150;
    transient public static final String frames_DEFAULT = "0";
    transient public static final String forceMethionine_DEFAULT = "0";
    transient public static final int clearRangeMinOrfSize_DEFAULT = 180;


    public MetaGenoOrfCallerTask() {
        super();
        setTaskName("MetaGenoOrfCallerTask");
        setParameter(PARAM_input_node_id, "");
        setParameter(PARAM_useClearRange, useClearRange_DEFAULT.toString());
    }

    public MetaGenoOrfCallerTask(Long queryNodeId,
                                 Boolean useClearRange,
                                 String translationTable,
                                 String beginningAsStart,
                                 String endAsStop,
                                 String assumeStops,
                                 String fullOrfs,
                                 int minOrfSize,
                                 int maxOrfSize,
                                 int minUnmaskedSize,
                                 String frames,
                                 String forceMethionine,
                                 int clearRangeMinOrfSize) {
        setParameter(PARAM_input_node_id, queryNodeId.toString());
        setParameter(PARAM_useClearRange, useClearRange.toString());
        setParameter(PARAM_translationTable, translationTable);
        setParameter(PARAM_beginningAsStart, beginningAsStart);
        setParameter(PARAM_endAsStop, endAsStop);
        setParameter(PARAM_assumeStops, assumeStops);
        setParameter(PARAM_fullOrfs, fullOrfs);
        setParameter(PARAM_minOrfSize, minOrfSize + "");
        setParameter(PARAM_maxOrfSize, maxOrfSize + "");
        setParameter(PARAM_minUnmaskedSize, minUnmaskedSize + "");
        setParameter(PARAM_frames, frames);
        setParameter(PARAM_forceMethionine, forceMethionine);
        setParameter(PARAM_clearRangeMinOrfSize, clearRangeMinOrfSize + "");
    }

    public String getDisplayName() {
        return "Metagenomic ORF Caller Task";
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (key.equals(PARAM_useClearRange))
            return new BooleanParameterVO(Boolean.parseBoolean(value));
        if (key.equals(PARAM_translationTable))
            return new TextParameterVO(translationTable_DEFAULT);
        if (key.equals(PARAM_beginningAsStart))
            return new TextParameterVO(beginningAsStart_DEFAULT);
        if (key.equals(PARAM_endAsStop))
            return new TextParameterVO(endAsStop_DEFAULT);
        if (key.equals(PARAM_assumeStops))
            return new TextParameterVO(assumeStops_DEFAULT);
        if (key.equals(PARAM_fullOrfs))
            return new TextParameterVO(fullOrfs_DEFAULT);
        if (key.equals(PARAM_minOrfSize))
            return new LongParameterVO(0L, 100000L, (long) minOrfSize_DEFAULT);
        if (key.equals(PARAM_maxOrfSize))
            return new LongParameterVO(0L, 100000000L, (long) maxOrfSize_DEFAULT);
        if (key.equals(PARAM_minUnmaskedSize))
            return new TextParameterVO(frames_DEFAULT);
        if (key.equals(PARAM_forceMethionine))
            return new TextParameterVO(forceMethionine_DEFAULT);
        if (key.equals(PARAM_clearRangeMinOrfSize))
            return new LongParameterVO(0L, 100000L, (long) clearRangeMinOrfSize_DEFAULT);
        if (value == null)
            return null;
        // no match
        return null;
    }

}
