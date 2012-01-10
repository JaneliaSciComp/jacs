
package org.janelia.it.jacs.model.tasks.metageno;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.model.vo.TextParameterVO;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 23, 2009
 * Time: 1:12:27 PM
 */
public class TrnaScanTask extends Task {

    transient public static final String options_DEFAULT = "-b";
    transient public static final String search_type_DEFAULT = "-G";

    transient public static final String PARAM_options = "options";
    transient public static final String PARAM_search_type = "search type";
    transient public static final String PARAM_input_fasta_node_id = "input fasta node";

    public TrnaScanTask() {
        super();
        setTaskName("TrnaScanTask");
        setParameter(PARAM_options, options_DEFAULT);
        setParameter(PARAM_search_type, search_type_DEFAULT);
        setParameter(PARAM_input_fasta_node_id, "");
    }

    public String getDisplayName() {
        return "TrnaScanTask";
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_options))
            return new TextParameterVO(value);
        if (key.equals(PARAM_search_type))
            return new TextParameterVO(value);
        // no match
        return null;
    }

    public String generateCommandStringNotIncludingIOParams() throws ParameterException {
        StringBuffer sb = new StringBuffer("");
        sb.append("-q ");
        sb.append(getParameterVO(PARAM_options).getStringValue()).append(" ");
        sb.append(getParameterVO(PARAM_search_type).getStringValue()).append(" ");
        return sb.toString();
    }

}
