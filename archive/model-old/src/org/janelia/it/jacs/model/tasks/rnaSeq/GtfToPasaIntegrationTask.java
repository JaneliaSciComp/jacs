
package org.janelia.it.jacs.model.tasks.rnaSeq;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Apr 8, 2010
 * Time: 4:10:37 PM
 */
public class GtfToPasaIntegrationTask extends Task {
    transient public static final String PARAM_pasa_database_name = "pasa database name";
    transient public static final String PARAM_refgenome_fasta_node_id = "reference genome fasta node id";
    transient public static final String PARAM_gtf_node_id = "gtf node id";

    public GtfToPasaIntegrationTask() {
        super();
        setTaskName("GtfToPasaIntegrationTask");
        setParameter(PARAM_refgenome_fasta_node_id, "");
        setParameter(PARAM_gtf_node_id, "");
    }

    public String getDisplayName() {
        return "GtfToPasaIntegrationTask";
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
