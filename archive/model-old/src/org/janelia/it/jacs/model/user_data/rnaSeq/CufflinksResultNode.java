
package org.janelia.it.jacs.model.user_data.rnaSeq;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 23, 2010
 * Time: 4:00:55 PM
 */
public class CufflinksResultNode extends FileNode {

    public transient static final String RESULT_EXTENSION_PREFIX = "r";

    // Valid files within CufflinksResultNode.
    public transient static final String TAG_GTF_OUTPUT = "transcripts.gtf";
    public transient static final String TAG_GENE_EXPRESSION_OUTPUT = "genes.expr";
    public transient static final String TAG_TRANSCRIPTS_EXPRESSION_OUTPUT = "transcripts.expr";

    public transient static final String BASE_OUTPUT_FILENAME = "cufflinks";

    protected long hitCount;

    // Constructors

    /**
     * default constructor
     */
    public CufflinksResultNode() {
    }

    public String getSubDirectory() {
        return "CufflinksResult";
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
    public CufflinksResultNode(String owner, Task task, String name, String description, String visibility,
                               String relativeSessionPath) {
        super(owner, task, name, description, visibility, Node.DIRECTORY_DATA_TYPE, relativeSessionPath);
    }

    public String getFilePathByTag(String tag) {
        if (tag.equals(TAG_GTF_OUTPUT)) return getFilePath(TAG_GTF_OUTPUT);
        if (tag.equals(TAG_GENE_EXPRESSION_OUTPUT)) return getFilePath(TAG_GENE_EXPRESSION_OUTPUT);
        if (tag.equals(TAG_TRANSCRIPTS_EXPRESSION_OUTPUT)) return getFilePath(TAG_TRANSCRIPTS_EXPRESSION_OUTPUT);
        return null;
    }

    public long getHitCount() {
        return hitCount;
    }

    public void setHitCount(long hitCount) {
        this.hitCount = hitCount;
    }

}
