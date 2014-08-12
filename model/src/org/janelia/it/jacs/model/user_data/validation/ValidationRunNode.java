package org.janelia.it.jacs.model.user_data.validation;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;

import java.io.File;
import java.io.FilenameFilter;
import java.io.Serializable;
import java.util.regex.Pattern;

/**
 * This type of node holds all files created during a validation run. It 'belongs' to the ultimate parent task,
 * or the 'submitter task' for all samples.
 *
 * Created by fosterl on 8/11/14.
 */
public class ValidationRunNode extends FileNode implements Serializable, IsSerializable {

    public static final String PUNCTUATION = "[\\[\\]{}().\\\\/,;:\\-=+|*&^%$#@!~`'?<>@\t\n ]";
    private static final Pattern PUNCT_PATTERN = Pattern.compile(PUNCTUATION);

    /** The no-args c'tor, in case it is required. */
    public ValidationRunNode() {
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
    public ValidationRunNode( String owner, Task task, String name, String description, String visibility, String dataType, String relativeSessionPath ) {
        super(owner, task, sanitizeNodeName(name), description, visibility, dataType, relativeSessionPath );
    }

    @Override
    public String getSubDirectory() {
        return "ValidationRun";
    }

    @Override
    public String getFilePathByTag(String tag) {
        return getOutputFilePath( tag );
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

    public static String sanitizeNodeName( String nodeName ) {
        return PUNCT_PATTERN.matcher(nodeName).replaceAll( "_" ).toLowerCase();
    }

}
