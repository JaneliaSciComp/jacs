
package org.janelia.it.jacs.model.user_data.metageno;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Mar 5, 2009
 * Time: 1:58:18 PM
 */
public class SimpleOrfCallerResultNode extends FileNode implements IsSerializable, Serializable {

    public transient static final String RESULT_EXTENSION_PREFIX = "r";

    // Valid files within SimpleOrfCallerResultNode.
    public transient static final String TAG_NUCLEOTIDE_ORF_FASTA_OUTPUT = "orf_nucleotide_fasta";
    public transient static final String TAG_PEPTIDE_ORF_FASTA_OUTPUT = "orf_peptide_fasta";

    public transient static final String BASE_OUTPUT_FILENAME = "simple_orf";
    public transient static final String ORF_NT_FASTA_OUTPUT_FILENAME = BASE_OUTPUT_FILENAME + "_nt.fasta";
    public transient static final String ORF_AA_FASTA_OUTPUT_FILENAME = BASE_OUTPUT_FILENAME + "_aa.fasta";

    protected long hitCount;

    // Constructors

    /**
     * default constructor
     */
    public SimpleOrfCallerResultNode() {
    }

    public String getSubDirectory() {
        return "SimpleOrfResult";
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
    public SimpleOrfCallerResultNode(String owner, Task task, String name, String description, String visibility,
                                     String relativeSessionPath) {
        super(owner, task, name, description, visibility, Node.DIRECTORY_DATA_TYPE, relativeSessionPath);
    }

    public String getFilePathByTag(String tag) {
        if (tag.equals(TAG_NUCLEOTIDE_ORF_FASTA_OUTPUT)) return getFilePath(ORF_NT_FASTA_OUTPUT_FILENAME);
        if (tag.equals(TAG_PEPTIDE_ORF_FASTA_OUTPUT)) return getFilePath(ORF_AA_FASTA_OUTPUT_FILENAME);
        return null;
    }

    public long getHitCount() {
        return hitCount;
    }

    public void setHitCount(long hitCount) {
        this.hitCount = hitCount;
    }

}
