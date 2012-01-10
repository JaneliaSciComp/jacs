
package org.janelia.it.jacs.model.user_data.tools;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;

/**
 * Created by IntelliJ IDEA.
 * User: ekelsey
 * Date: Mar 30, 2010
 * Time: 2:07:27 PM
 */
public class Legacy2BsmlResultNode extends FileNode {

    // Valid files within Legacy2BsmlResultNode.
    public transient static final String TAG_LEGACY2BSML_FSA = "fsa";
    public transient static final String TAG_LEGACY2BSML_BSML = "bsml";
    public transient static final String TAG_LEGACY2BSML_IDMAP = "idmap";
    public transient static final String TAG_LEGACY2BSML_LOG = "log";
    public transient static final String TAG_LEGACY2BSML_STDERR = "stderr";
    public transient static final String TAG_LEGACY2BSML_FSALIST = "fsa_list";
    public transient static final String TAG_LEGACY2BSML_BSMLLIST = "bsml_list";

    public transient static final String BASE_OUTPUT_FILENAME = "legacy2bsml";

    protected long hitCount;

    // Constructors

    /**
     * default constructor
     */
    public Legacy2BsmlResultNode() {
    }

    public String getSubDirectory() {
        return "Legacy2BsmlResult";
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
    public Legacy2BsmlResultNode(String owner, Task task, String name, String description, String visibility,
                                 String relativeSessionPath) {
        super(owner, task, name, description, visibility, Node.DIRECTORY_DATA_TYPE, relativeSessionPath);
    }

    public String getFilePathByTag(String tag) {
        if (tag.equals(TAG_LEGACY2BSML_FSA)) return getFilePath(BASE_OUTPUT_FILENAME + "." + TAG_LEGACY2BSML_FSA);
        if (tag.equals(TAG_LEGACY2BSML_BSML)) return getFilePath(BASE_OUTPUT_FILENAME + "." + TAG_LEGACY2BSML_BSML);
        if (tag.equals(TAG_LEGACY2BSML_IDMAP)) return getFilePath(BASE_OUTPUT_FILENAME + "." + TAG_LEGACY2BSML_IDMAP);
        if (tag.equals(TAG_LEGACY2BSML_LOG)) return getFilePath(BASE_OUTPUT_FILENAME + "." + TAG_LEGACY2BSML_LOG);
        if (tag.equals(TAG_LEGACY2BSML_STDERR)) return getFilePath(BASE_OUTPUT_FILENAME + "." + TAG_LEGACY2BSML_STDERR);
        if (tag.equals(TAG_LEGACY2BSML_FSALIST))
            return getFilePath(BASE_OUTPUT_FILENAME + "." + TAG_LEGACY2BSML_FSALIST);
        if (tag.equals(TAG_LEGACY2BSML_BSMLLIST))
            return getFilePath(BASE_OUTPUT_FILENAME + "." + TAG_LEGACY2BSML_BSMLLIST);

        return null;
    }

    public long getHitCount() {
        return hitCount;
    }

    public void setHitCount(long hitCount) {
        this.hitCount = hitCount;
    }

}