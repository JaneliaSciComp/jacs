
package org.janelia.it.jacs.model.tasks.metageno;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.model.vo.TextParameterVO;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Mar 5, 2009
 * Time: 1:32:53 PM
 */
public class SimpleOrfCallerTask extends Task {

    transient public static final String translation_table_DEFAULT = "11";
    transient public static final String beginning_as_start_DEFAULT = "1";
    transient public static final String end_as_stop_DEFAULT = "1";
    transient public static final String assume_stops_DEFAULT = "0";
    transient public static final String full_orfs_DEFAULT = "0";
    transient public static final String min_orf_size_DEFAULT = "180";
    transient public static final String max_orf_size_DEFAULT = "0";
    transient public static final String min_unmasked_size_DEFAULT = "150";
    transient public static final String frames_DEFAULT = "0";
    transient public static final String force_methionine_DEFAULT = "0";
    transient public static final String header_additions_DEFAULT = "";

    transient public static final String PARAM_input_fasta_node_id = "input_fasta_node_id";
    transient public static final String PARAM_translation_table = "translation_table";
    transient public static final String PARAM_beginning_as_start = "beginning_as_start";
    transient public static final String PARAM_end_as_stop = "end_as_stop";
    transient public static final String PARAM_assume_stops = "assume_stops";
    transient public static final String PARAM_full_orfs = "full_orfs";
    transient public static final String PARAM_min_orf_size = "min_orf_size";
    transient public static final String PARAM_max_orf_size = "max_orf_size";
    transient public static final String PARAM_min_unmasked_size = "min_unmasked_size";
    transient public static final String PARAM_frames = "frames";
    transient public static final String PARAM_force_methionine = "force_methionine";
    transient public static final String PARAM_header_additions = "header_additions";


    public SimpleOrfCallerTask() {
        super();
        setTaskName("SimpleOrfCallerTask");
        setParameter(PARAM_input_fasta_node_id, "");
        setParameter(PARAM_translation_table, translation_table_DEFAULT);
        setParameter(PARAM_beginning_as_start, beginning_as_start_DEFAULT);
        setParameter(PARAM_end_as_stop, end_as_stop_DEFAULT);
        setParameter(PARAM_assume_stops, assume_stops_DEFAULT);
        setParameter(PARAM_full_orfs, full_orfs_DEFAULT);
        setParameter(PARAM_min_orf_size, min_orf_size_DEFAULT);
        setParameter(PARAM_max_orf_size, max_orf_size_DEFAULT);
        setParameter(PARAM_min_unmasked_size, min_unmasked_size_DEFAULT);
        setParameter(PARAM_frames, frames_DEFAULT);
        setParameter(PARAM_force_methionine, force_methionine_DEFAULT);
        setParameter(PARAM_header_additions, header_additions_DEFAULT);
    }

    public String getDisplayName() {
        return "SimpleOrfCallerTask";
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_translation_table))
            return new TextParameterVO(value);
        if (key.equals(PARAM_beginning_as_start))
            return new TextParameterVO(value);
        if (key.equals(PARAM_end_as_stop))
            return new TextParameterVO(value);
        if (key.equals(PARAM_assume_stops))
            return new TextParameterVO(value);
        if (key.equals(PARAM_full_orfs))
            return new TextParameterVO(value);
        if (key.equals(PARAM_min_orf_size))
            return new TextParameterVO(value);
        if (key.equals(PARAM_max_orf_size))
            return new TextParameterVO(value);
        if (key.equals(PARAM_min_unmasked_size))
            return new TextParameterVO(value);
        if (key.equals(PARAM_frames))
            return new TextParameterVO(value);
        if (key.equals(PARAM_force_methionine))
            return new TextParameterVO(value);
        if (key.equals(PARAM_header_additions))
            return new TextParameterVO(value);
        // no match
        return null;
    }

}
