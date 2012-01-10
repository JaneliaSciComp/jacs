
package org.janelia.it.jacs.model.user_data.recruitment;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.blast.BlastDatabaseFileNode;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Apr 23, 2010
 * Time: 2:55:53 PM
 */
public class RecruitmentSamplingDatabaseFileNode extends BlastDatabaseFileNode {
    public static final String TAG_SAMPLING_FASTA_NAME = "sampling.fasta";

    /**
     * default constructor
     */
    public RecruitmentSamplingDatabaseFileNode() {
    }

    /**
     * constructor
     *
     * @param owner               - person who owns the node
     * @param task                - task which created this node
     * @param name                - name of the node
     * @param description         - description of the node
     * @param visibility          - visibility of the node to others
     * @param relativeSessionPath - name of the work session this node belongs to
     * @param sequenceType        - sequence type of the data
     */
    public RecruitmentSamplingDatabaseFileNode(String owner, Task task, String name, String description, String visibility, String sequenceType,
                                               String relativeSessionPath) {
        super(owner, task, name, description, visibility, sequenceType, relativeSessionPath);
    }

    @Override
    public String getSubDirectory() {
        return "RecruitmentSamplingBlastDatabases";
    }
}
