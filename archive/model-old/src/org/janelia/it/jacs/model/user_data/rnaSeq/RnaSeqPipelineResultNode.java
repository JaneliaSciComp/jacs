
package org.janelia.it.jacs.model.user_data.rnaSeq;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Apr 2, 2010
 * Time: 12:00:07 PM
 */
public class RnaSeqPipelineResultNode extends FileNode {

    // Constructors

    /**
     * default constructor
     */
    public RnaSeqPipelineResultNode() {
    }

    public String getSubDirectory() {
        return "RnaSeqPipelineResult";
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
     */
    public RnaSeqPipelineResultNode(String owner, Task task, String name, String description, String visibility,
                                    String relativeSessionPath) {
        super(owner, task, name, description, visibility, Node.DIRECTORY_DATA_TYPE, relativeSessionPath);
    }

    public String getFilePathByTag(String tag) {
        return null;
    }

}
