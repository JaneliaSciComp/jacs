package org.janelia.it.jacs.model.user_data.tiledMicroscope;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.TextualEntropy;
import org.janelia.it.jacs.model.tasks.tiledMicroscope.TmNeuronPBUpdateTask;

import java.io.File;
import java.io.FilenameFilter;
import java.io.Serializable;

/**
 * Node to represent update process against Tiled Microscope Workspaces/Neurons
 * to transform them to ProtoBuf serialized objects.
 *
 * Created by fosterl on 2/12/2016.
 */
public class TmNeuronPBUpdateNode extends FileNode implements Serializable, IsSerializable {

    /** The no-args c'tor, in case it is required. */
    public TmNeuronPBUpdateNode() {
        super();
    }

    /**
     * Full-inputs override constructor.
     *
     * @param owner               - person who owns the node
     * @param task                - task which created this node
     * @param name                - name of the node
     * @param description         - description of the node
     * @param visibility          - visibility of the node to others
     * @param relativeSessionPath - name of the work session this node belongs to
     * @param dataType            - tag for the node
     *
     */
    public TmNeuronPBUpdateNode( String owner, Task task, String name, String description, String visibility, String dataType, String relativeSessionPath ) {
        super(owner, task, TextualEntropy.sanitizeNodeName(name), description, visibility, dataType, relativeSessionPath );
    }

    @Override
    public String getSubDirectory() {
        return TmNeuronPBUpdateTask.PROCESS_NAME;  // Borrowing process name.
    }

    @Override
    public String getFilePathByTag(String tag) {
        return getOutputFilePath( tag );
    }

    private String getOutputFilePath(final String tagTarget) {
        File tmpDir = new File(getDirectoryPath());
        File[] tmpFiles = tmpDir.listFiles(new FilenameFilter() {
            @Override
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
