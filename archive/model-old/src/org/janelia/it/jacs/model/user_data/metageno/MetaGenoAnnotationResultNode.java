
package org.janelia.it.jacs.model.user_data.metageno;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Mar 19, 2009
 * Time: 12:37:17 PM
 */
public class MetaGenoAnnotationResultNode extends FileNode implements IsSerializable, Serializable {

    protected long hitCount;
    public transient static final String FILENAME_ANNOTATION = "annotation_rules.combined.out";
    public transient static final String FILENAME_HMM_RAW = "ldhmmpfam_full.raw.combined.out";
    public transient static final String FILENAME_HMM_HTAB = "ldhmmpfam_full.htab.combined.out";
    public transient static final String FILENAME_BLAST_BTAB = "ncbi_blastp_btab.combined.out";
    //public transient static final String FILENAME_BLAST_XML = "PandaBlastpService/peptide.fasta.blastp_out.xml";
    public transient static final String FILENAME_PRIAM_RAW = "priam_ec.output.hits";
    public transient static final String FILENAME_PRIAM_TAB = "priam_ec.ectab.combined.out";
    public transient static final String FILENAME_TMHMM_RAW = "tmhmm.raw.combined.out";
    public transient static final String FILENAME_LIPOPROTEIN_BSML = "lipoprotein_bsml.parsed";
    public transient static final String FILENAME_SQLITE_ANNOTATION = "sqlPersistAnno.db";
    // Constructors

    /**
     * default constructor
     */
    public MetaGenoAnnotationResultNode() {
    }

    public String getSubDirectory() {
        return "MetaGenoAnnotationResult";
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
    public MetaGenoAnnotationResultNode(String owner, Task task, String name, String description, String visibility,
                                        String relativeSessionPath) {
        super(owner, task, name, description, visibility, Node.DIRECTORY_DATA_TYPE, relativeSessionPath);
    }

    public String getFilePathByTag(String tag) {
        if (tag.equals(FILENAME_ANNOTATION)) {
            return getFilePath(FILENAME_ANNOTATION);
        }
        else if (tag.equals(FILENAME_HMM_RAW)) {
            return getFilePath(FILENAME_HMM_RAW);
        }
        else if (tag.equals(FILENAME_HMM_HTAB)) {
            return getFilePath(FILENAME_HMM_HTAB);
        }
        else if (tag.equals(FILENAME_BLAST_BTAB)) {
            return getFilePath(FILENAME_BLAST_BTAB);
        }
        //else if(tag.equals(FILENAME_BLAST_XML)) {return getFilePath(FILENAME_BLAST_XML);}
        else if (tag.equals(FILENAME_PRIAM_RAW)) {
            return getFilePath(FILENAME_PRIAM_RAW);
        }
        else if (tag.equals(FILENAME_PRIAM_TAB)) {
            return getFilePath(FILENAME_PRIAM_TAB);
        }
        else if (tag.equals(FILENAME_TMHMM_RAW)) {
            return getFilePath(FILENAME_TMHMM_RAW);
        }
        else if (tag.equals(FILENAME_LIPOPROTEIN_BSML)) {
            return getFilePath(FILENAME_LIPOPROTEIN_BSML);
        }
        else if (tag.equals(FILENAME_SQLITE_ANNOTATION)) {
            return getFilePath(FILENAME_SQLITE_ANNOTATION);
        }
        else {
            return null;
        }

    }

    public long getHitCount() {
        return hitCount;
    }

    public void setHitCount(long hitCount) {
        this.hitCount = hitCount;
    }
}
