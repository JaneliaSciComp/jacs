
package org.janelia.it.jacs.model.tasks.rnaSeq;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 2, 2010
 * Time: 12:11:18 PM
 */
public class TophatTask extends Task {

    transient public static final String PARAM_reads_fastQ_node_id = "reads fastQ node id";
    transient public static final String PARAM_refgenome_fasta_node_id = "reference genome fasta node";

    public TophatTask() {
        super();
        setTaskName("TophatTask");
        setParameter(PARAM_reads_fastQ_node_id, "");
        setParameter(PARAM_refgenome_fasta_node_id, "");
    }

    public String getDisplayName() {
        return "TophatTask";
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

}
