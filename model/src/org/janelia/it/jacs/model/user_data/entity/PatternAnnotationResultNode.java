package org.janelia.it.jacs.model.user_data.entity;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;

import java.io.File;
import java.io.FilenameFilter;
import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 2/2/12
 * Time: 8:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class PatternAnnotationResultNode extends FileNode implements IsSerializable, Serializable {

    /**
     * constructor
     */
    public PatternAnnotationResultNode() {
    }

    public PatternAnnotationResultNode(String owner, Task task, String name, String description, String visibility,
                                     String relativeSessionPath) {
        super(owner, task, name, description, visibility, Node.DIRECTORY_DATA_TYPE, relativeSessionPath);
    }

    public String getSubDirectory() {
        return "PatternAnnotation";
    }

    public String getFilePathByTag(String tag) {
        return getOutputFilePath(tag);
    }

    private String getOutputFilePath(final String tagTarget) {
        File tmpDir = new File(getDirectoryPath());
        File[] tmpFiles = tmpDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return (name.toLowerCase().endsWith(tagTarget.toLowerCase()));
            }
        });
        // Assumes one and only one file, and that the tag is a unique part of the filename!
        if (1 <= tmpFiles.length) {
            return tmpFiles[0].getAbsolutePath();
        }
        return null;
    }

}

