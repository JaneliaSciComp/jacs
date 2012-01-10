
package org.janelia.it.jacs.model.user_data.tools;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;

/**
 * Created by IntelliJ IDEA.
 * User: ekelsey
 * Date: Aug 13, 2010
 * Time: 2:07:27 PM
 */
public class JaccardResultNode extends FileNode {

    // Valid files within JaccardResultNode.
    public transient static final String TAG_LOOKUP_OUTPUT="lookup";
    public transient static final String TAG_CLUSTER_OUTPUT="out";
    public transient static final String TAG_FASTA_OUTPUT="fsa";
    public transient static final String TAG_LIST_OUTPUT="list";
    public transient static final String TAG_FSA_LIST="jaccard.fsa.list";

    public transient static final String BASE_OUTPUT_FILENAME="jaccard";

    protected long hitCount;

    // Constructors

    /** default constructor */
    public JaccardResultNode() {}

    public String getSubDirectory() {
        return "JaccardResult";
    }

    /** constructor
     * @param owner - person who owns the node
     * @param task - task which created this node
     * @param name - name of the node
     * @param description - description of the node
     * @param visibility - visibility of the node to others
     * @param relativeSessionPath - name of the work session this node belongs to
     **/
    public JaccardResultNode(String owner, Task task, String name, String description, String visibility,
                                         String relativeSessionPath) {
        super(owner, task, name, description, visibility, Node.DIRECTORY_DATA_TYPE, relativeSessionPath);
    }

    public String getFilePathByTag(String tag) {
        if (tag.equals(TAG_LOOKUP_OUTPUT)) return getFilePath(BASE_OUTPUT_FILENAME+"."+TAG_LOOKUP_OUTPUT);
        if (tag.equals(TAG_CLUSTER_OUTPUT)) return getFilePath(BASE_OUTPUT_FILENAME+"."+TAG_CLUSTER_OUTPUT);
        if (tag.equals(TAG_FASTA_OUTPUT)) return getFilePath(BASE_OUTPUT_FILENAME+"."+TAG_FASTA_OUTPUT);
        if (tag.equals(TAG_LIST_OUTPUT)) return getFilePath(BASE_OUTPUT_FILENAME+"."+TAG_LIST_OUTPUT);
        if (tag.equals(TAG_FSA_LIST)) return getFilePath(BASE_OUTPUT_FILENAME+"."+TAG_FSA_LIST);
        return null;
    }

    public long getHitCount() {
        return hitCount;
    }

    public void setHitCount(long hitCount) {
        this.hitCount = hitCount;
    }

}