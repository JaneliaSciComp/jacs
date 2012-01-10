
package org.janelia.it.jacs.model.tasks.blast;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.DoubleParameterVO;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.model.vo.TextParameterVO;

import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Oct 3, 2008
 * Time: 10:50:49 AM
 */
public class CreateMpiBlastDatabaseTask extends Task {
    public static final String DISPLAY_NAME = "Create MPI Blast Database Task";
    public static final String PARAM_FASTA_NODE_ID = "fastaFileNodeId";
    public static final String PARAM_BLAST_DB_NAME = "blastDatabaseName";
    public static final String PARAM_BLAST_DB_DESCRIPTION = "blastDatabaseDescription";
    public static final String PARAM_NUM_FRAGS = "numFrags";

    public CreateMpiBlastDatabaseTask() {
        super();
        taskName = "Create MPI Blast Database";
    }

    public CreateMpiBlastDatabaseTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet,
                                      String blastDBName, String blastDBDescription, String fastaNodeId, int numFrags) {
        super(inputNodes, owner, events, taskParameterSet);
        this.taskName = DISPLAY_NAME;
        setParameter(PARAM_FASTA_NODE_ID, fastaNodeId);
        setParameter(PARAM_BLAST_DB_NAME, blastDBName);
        setParameter(PARAM_BLAST_DB_DESCRIPTION, blastDBDescription);
        setParameter(PARAM_NUM_FRAGS, Integer.toString(numFrags));
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_FASTA_NODE_ID)) {
            return new TextParameterVO(value, 400);
        }
        if (key.equals(PARAM_BLAST_DB_NAME)) {
            return new TextParameterVO(value, 400);
        }
        if (key.equals(PARAM_BLAST_DB_DESCRIPTION)) {
            return new TextParameterVO(value, 1000);
        }
        if (key.equals(PARAM_NUM_FRAGS)) {
            return new DoubleParameterVO(Double.valueOf(value));
        }
        // no match
        return null;
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    public String getBlastDatabaseName() {
        return getParameter(PARAM_BLAST_DB_NAME);
    }

    public String getBlastDatabaseDescription() {
        return getParameter(PARAM_BLAST_DB_DESCRIPTION);
    }

    public String getFastaNodeId() {
        return getParameter(PARAM_FASTA_NODE_ID);
    }

    public int getNumberFragments() {
        return Integer.valueOf(getParameter(PARAM_NUM_FRAGS));
    }
}