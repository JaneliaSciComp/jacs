
package org.janelia.it.jacs.model.tasks.tools;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

/**
 * Created by IntelliJ IDEA.
 * User: jinman
 * Date: Jun 28, 2010
 * Time: 9:00:00 AM
 */
public class GenezillaTask extends Task {

    /*  As far as I can tell, this is the extent of the help info:

    ----------------
    genezilla <*.iso> <*.fasta> [options]
       options:
          -s <N> : ignore sequences in FASTA shorter than N bases
          -i <file> : load isochore predictions from file
          -c <file> : load CpG island predictions from file
    ----------------

    */

    transient public static final String PARAM_ignore_short_fasta = "ignore short fasta";
    transient public static final String PARAM_isochore_prediction_file = "isochore prediction file";
    transient public static final String PARAM_cpg_island_prediction_file = "cpg island prediction file";
    transient public static final String PARAM_fasta_input_node_id = "fasta input node id";
    transient public static final String PARAM_iso_input_node_id = "iso input node id";

    public GenezillaTask() {
        super();
        setTaskName("GenezillaTask");
        setParameter(PARAM_fasta_input_node_id, "");
        setParameter(PARAM_iso_input_node_id, "");
    }

    public String getDisplayName() {
        return "GenezillaTask";
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        // no match
        return null;
    }

    public String generateCommandOptions() throws ParameterException {
        StringBuffer sb = new StringBuffer();
        addCommandParameter(sb, "-s", PARAM_ignore_short_fasta);
        addCommandParameter(sb, "-i", PARAM_isochore_prediction_file);
        addCommandParameter(sb, "-c", PARAM_cpg_island_prediction_file);
        return sb.toString();
    }

}