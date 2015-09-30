
package org.janelia.it.jacs.model.tasks.tools;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

/**
 * Created by IntelliJ IDEA.
 * User: jinman
 * Date: April 15, 2010
 * Time: 12:16:04 PM
 */
public class TargetpTask extends Task {

    /*
    Reference:   http://www.cbs.dtu.dk/suppl/natureprotocols/
    Also, below is the entirety of the helptext from running "targetp -h"

    Usage: targetp options files

    The options:     -P|-N  use plant/non-plant networks (mandatory)
                     -c     include cleavage site predictions
                     -h     print this note and exit
                     -v     print version info and exit
                     -p #   chloroplast prediction cutoff, default 0.00
                     -s #   secretory pathway prediction cutoff, default 0.00
                     -t #   mitochondrial prediction cutoff, default 0.00
                     -o #   "other location" prediction cutoff, default 0.00

                     If no files are specified the standard input will be used.
     */

    transient public static final String PARAM_network_type = "type of network";
    transient public static final String PARAM_include_cleavage = "include cleavage site predictions";
    transient public static final String PARAM_chloroplast_cutoff = "chloroplast prediction cutoff";
    transient public static final String PARAM_secretory_cutoff = "secretory pathway prediction cutoff";
    transient public static final String PARAM_mitochondrial_cutoff = "mitochondrial prediction cutoff";
    transient public static final String PARAM_other_cutoff = "chloroplast prediction cutoff";
    transient public static final String PARAM_fasta_input_node_id = "input node id";

    transient public static final String network_type_DEFAULT = "-N";
    transient public static final String include_cleavage_DEFAULT = "1";

    public TargetpTask() {
        super();
        setTaskName("TargetpTask");
        setParameter(PARAM_fasta_input_node_id, "");
        setParameter(PARAM_network_type, network_type_DEFAULT);
        setParameter(PARAM_include_cleavage, include_cleavage_DEFAULT);
    }

    public String getDisplayName() {
        return "TargetpTask";
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
        sb.append(getParameter(PARAM_network_type));
        sb.append(" ");
        if (getParameter(PARAM_include_cleavage).equals("1")) {
            sb.append("-c ");
        }
        addCommandParameter(sb, "-p", PARAM_chloroplast_cutoff);
        addCommandParameter(sb, "-s", PARAM_secretory_cutoff);
        addCommandParameter(sb, "-t", PARAM_mitochondrial_cutoff);
        addCommandParameter(sb, "-o", PARAM_other_cutoff);
        return sb.toString();
    }

}