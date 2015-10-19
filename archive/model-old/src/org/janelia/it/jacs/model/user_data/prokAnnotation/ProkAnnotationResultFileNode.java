
package org.janelia.it.jacs.model.user_data.prokAnnotation;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 28, 2007
 * Time: 10:36:29 AM
 */
public class ProkAnnotationResultFileNode extends FileNode implements Serializable, IsSerializable {

    public static final String SUB_DIRECTORY = "ProkAnnotation";

    // valid tags
    public transient static final String TAG_MATES = "mates";

    // Valid files within BlastResultFileNode.
    public transient static final String MATE_FILENAME = "mates.list";

    // Default constructor for hibernate
    public ProkAnnotationResultFileNode() {
    }

    /**
     * Constructor which most people should use
     *
     * @param owner               of the node
     * @param task                task which created the node
     * @param name                name of the node
     * @param description         full description
     * @param visibility          visibility of the node to other people
     * @param relativeSessionPath - name of the work session this node belongs to
     */
    public ProkAnnotationResultFileNode(String owner, Task task, String name, String description, String visibility,
                                        String relativeSessionPath) {
        super(owner, task, name, description, visibility, Node.DIRECTORY_DATA_TYPE, relativeSessionPath);
    }

    public String getSubDirectory() {
        return SUB_DIRECTORY;
    }

    public String getFilePathByTag(String tag) {
        if (tag.equals(TAG_MATES)) return getFilePath(MATE_FILENAME);
        return null;
    }


}