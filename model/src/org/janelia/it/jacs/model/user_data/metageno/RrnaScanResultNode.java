
package org.janelia.it.jacs.model.user_data.metageno;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Mar 3, 2009
 * Time: 9:35:33 AM
 */
public class RrnaScanResultNode extends FileNode implements IsSerializable, Serializable {

    public transient static final String RESULT_EXTENSION_PREFIX = "r";

    // Valid files within HmmerPfamResultNode.
    public transient static final String TAG_RRNA_FASTA_OUTPUT = "rrna_fasta";
    public transient static final String TAG_POSTMASK_FASTA_OUTPUT = "postmask_fasta";

    public transient static final String BASE_OUTPUT_FILENAME = "rrnascan";
    public transient static final String RRNA_FASTA_OUTPUT_FILENAME = BASE_OUTPUT_FILENAME + "_rRNA.fasta";
    public transient static final String POSTMASK_FASTA_OUTPUT_FILENAME = BASE_OUTPUT_FILENAME + "_soft_mask_rRNA.fasta";

    protected long hitCount;

    // Constructors

    /**
     * default constructor
     */
    public RrnaScanResultNode() {
    }

    public String getSubDirectory() {
        return "RrnaScanResult";
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
    public RrnaScanResultNode(String owner, Task task, String name, String description, String visibility,
                              String relativeSessionPath) {
        super(owner, task, name, description, visibility, Node.DIRECTORY_DATA_TYPE, relativeSessionPath);
    }

    public String getFilePathByTag(String tag) {
        if (tag.equals(TAG_RRNA_FASTA_OUTPUT)) return getFilePath(RRNA_FASTA_OUTPUT_FILENAME);
        if (tag.equals(TAG_POSTMASK_FASTA_OUTPUT)) return getFilePath(POSTMASK_FASTA_OUTPUT_FILENAME);
        return null;
    }

    public long getHitCount() {
        return hitCount;
    }

    public void setHitCount(long hitCount) {
        this.hitCount = hitCount;
    }

}
