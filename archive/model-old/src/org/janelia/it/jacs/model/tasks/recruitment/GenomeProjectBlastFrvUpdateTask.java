
package org.janelia.it.jacs.model.tasks.recruitment;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.MultiSelectVO;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.model.vo.TextParameterVO;

import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Feb 25, 2008
 * Time: 2:31:17 PM
 */
public class GenomeProjectBlastFrvUpdateTask extends Task {
    public static final String DISPLAY_NAME = "Genome Project-to-Frv Update Task";
    public static final String GENBANK_FILE_NAME = "genbankFileName";
    public static final String GENOME_PROJECT_NODE_ID = "genomeProjectNodeId";
    public static final String NEW_BLASTABLE_DATABASE_NODES = "newBlastableDatabaseNodes";
    public static final String PREVIOUS_FRV_FILTER_TASK_ID = "previousFRVFilterDataTask";

    public GenomeProjectBlastFrvUpdateTask() {
        super();
    }

    public GenomeProjectBlastFrvUpdateTask(String previousFRVFilterDataTask, String commaSeparatedListOfBlastDBNodeIds,
                                           String genomeProjectNodeId, String genBankFileName, Set<Node> inputNodes,
                                           String owner, List<Event> events, Set<TaskParameter> parameters) {
        super(inputNodes, owner, events, parameters);
        setParameter(PREVIOUS_FRV_FILTER_TASK_ID, previousFRVFilterDataTask);
        setParameter(NEW_BLASTABLE_DATABASE_NODES, commaSeparatedListOfBlastDBNodeIds);
        setParameter(GENOME_PROJECT_NODE_ID, genomeProjectNodeId);
        setParameter(GENBANK_FILE_NAME, genBankFileName);
        this.taskName = DISPLAY_NAME;
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(GENOME_PROJECT_NODE_ID)) {
            return new TextParameterVO(value, value.length() > 500 ? value.length() : 500);
        }
        if (key.equals(GENBANK_FILE_NAME)) {
            return new TextParameterVO(value, value.length() > 500 ? value.length() : 500);
        }
        if (key.equals(NEW_BLASTABLE_DATABASE_NODES)) {
            List<String> selectList = Task.listOfStringsFromCsvString(value);
            return new MultiSelectVO(selectList, selectList);
        }
        if (key.equals(PREVIOUS_FRV_FILTER_TASK_ID)) {
            return new TextParameterVO(value, value.length() > 500 ? value.length() : 500);
        }
        // no match
        return null;
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

}
