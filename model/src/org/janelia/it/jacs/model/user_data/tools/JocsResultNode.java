
package org.janelia.it.jacs.model.user_data.tools;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;

/**
 * Created by IntelliJ IDEA.
 * User: ekelsey
 * Date: Aug 26, 2010
 * Time: 2:07:27 PM
 */
public class JocsResultNode extends FileNode {

     // Valid files within JocsResultNode.

    //Out file extensions for CogBsmlLoader.pl
    public transient static final String TAG_BTAB_OUTPUT = "btab";
    public transient static final String TAG_FASTA_OUTPUT="fsa";
    public transient static final String TAG_COG_OUTPUT="cog";
    public transient static final String TAG_LIST_OUTPUT="list";

    public transient static final String BASE_OUTPUT_FILENAME = "jocs";

    protected long hitCount;

    // Constructors

    /**
     * default constructor
     */
    public JocsResultNode() {
    }

    public String getSubDirectory() {
        return "JocsResult";
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
    public JocsResultNode(String owner, Task task, String name, String description, String visibility,
                             String relativeSessionPath) {
        super(owner, task, name, description, visibility, Node.DIRECTORY_DATA_TYPE, relativeSessionPath);
    }

    public String getFilePathByTag(String tag) {
        if (tag.equals(TAG_BTAB_OUTPUT)) return getFilePath(BASE_OUTPUT_FILENAME + "." + TAG_BTAB_OUTPUT);
        if (tag.equals(TAG_FASTA_OUTPUT)) return getFilePath(BASE_OUTPUT_FILENAME + "." + TAG_FASTA_OUTPUT);
        if (tag.equals(TAG_COG_OUTPUT)) return getFilePath(BASE_OUTPUT_FILENAME + "." + TAG_COG_OUTPUT);
        if (tag.equals(TAG_LIST_OUTPUT)) return getFilePath(BASE_OUTPUT_FILENAME + "." + TAG_LIST_OUTPUT);

        return null;
    }

    public long getHitCount() {
        return hitCount;
    }

    public void setHitCount(long hitCount) {
        this.hitCount = hitCount;
    }

}