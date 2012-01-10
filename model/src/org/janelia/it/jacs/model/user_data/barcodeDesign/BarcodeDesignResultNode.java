
package org.janelia.it.jacs.model.user_data.barcodeDesign;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;

import java.io.File;
import java.io.FilenameFilter;
import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Dec 15, 2008
 * Time: 3:32:11 PM
 */
public class BarcodeDesignResultNode extends FileNode implements IsSerializable, Serializable {

//    public transient static final String TAG_BARCODE_OUTPUT = "Barcodes.tsv";
//    public transient static final String TAG_GENERATION_PROFILE_OUTPUT = "GenerationProfile.tsv";
//    public transient static final String TAG_BARCODE_WITH_PRIMER_OUTPUT = "Barcode_wPrimer.tsv";

    /**
     * constructor
     */
    public BarcodeDesignResultNode() {
    }

    public BarcodeDesignResultNode(String owner, Task task, String name, String description, String visibility,
                                   String relativeSessionPath) {
        super(owner, task, name, description, visibility, Node.DIRECTORY_DATA_TYPE, relativeSessionPath);
    }

    public String getSubDirectory() {
        return "BarcodeDesign";
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