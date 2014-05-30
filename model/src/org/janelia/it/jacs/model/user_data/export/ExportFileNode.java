
package org.janelia.it.jacs.model.user_data.export;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 28, 2007
 * Time: 10:36:29 AM
 */
public class ExportFileNode extends FileNode implements Serializable, IsSerializable {

    public static final String SUB_DIRECTORY = "Export";
    public static final String EXTERNAL_LINK_FILE = "_external_link_.txt";

    private Integer numExportItems = 0;

    // Default constructor for hibernate
    public ExportFileNode() {
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
    public ExportFileNode(String owner, Task task, String name, String description, String visibility,
                          String relativeSessionPath) {
        super(owner, task, name, description, visibility, Node.DIRECTORY_DATA_TYPE, relativeSessionPath);
    }

    public String getSubDirectory() {
        return SUB_DIRECTORY;
    }

    public String getFilePathByTag(String tag) {
        return null;
    }

    public Integer getNumExportItems() {
        return numExportItems;
    }

    public void setNumExportItems(Integer numExportItems) {
        this.numExportItems = numExportItems;
    }

    public void dropExternalLink(String targetFileNodeId, String targetFileName, String desiredReplacementName) {
        FileWriter writer = null;
        try {
            writer = new FileWriter(new File(getDirectoryPath() + File.separator + ExportFileNode.EXTERNAL_LINK_FILE));
            writer.write(targetFileNodeId + "\t" + targetFileName + "\t" + desiredReplacementName + "\n");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if (null != writer) {
                try {
                    writer.flush();
                    writer.close();
                }
                catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

}