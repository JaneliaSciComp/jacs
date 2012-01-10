
package org.janelia.it.jacs.model.user_data;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.tasks.Task;

//import org.apache.log4j.*;


/**
 * User: naxelrod
 * Date: Oct 2, 2009
 * Time: 10:16:37 AM
 */
public class GenericFileNode extends FileNode implements java.io.Serializable, IsSerializable {

    static Logger logger = Logger.getLogger(GenericFileNode.class.getName());

    private transient static final String TAG_TEXT = "txt";

    private transient static final String FILE_TYPE_NAME = "generic";

    private String filePath;

    /**
     * default constructor
     */
    public GenericFileNode() {
    }

    /**
     * constructor
     */
    public GenericFileNode(String owner, Task task, String name, String description, String visibility,
                           String type, String relativeFilePath) {
        super(owner, task, name, description, visibility, Node.DIRECTORY_DATA_TYPE, relativeFilePath);
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getSubDirectory() {
        return "Files";
    }

    @Override
    public String getFilePathByTag(String tag) {
        if (tag.equals(TAG_TEXT)) {
            return getFilePath(FILE_TYPE_NAME + "." + TAG_TEXT);
        }
        logger.error("GenericFileNode: Do not recognize tag type " + tag + " returning null");
        return null;
    }
}
