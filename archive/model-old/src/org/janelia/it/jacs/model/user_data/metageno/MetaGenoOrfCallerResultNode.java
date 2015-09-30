
package org.janelia.it.jacs.model.user_data.metageno;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;

import java.io.File;
import java.io.FilenameFilter;
import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Mar 9, 2009
 * Time: 1:30:20 PM
 */
public class MetaGenoOrfCallerResultNode extends FileNode implements IsSerializable, Serializable {

    protected long hitCount;

    public transient static final String TAG_ORF_OUTPUT = "metagene_mapped_pep";
    public transient static final String FILENAME_TRNA = "camera_extract_trna.combined.fasta";
    public transient static final String FILENAME_RRNA = "camera_rrna_finder.combined.fasta";
    public transient static final String FILENAME_ORF_CLR_FNA = "clr_range_filter_orf.clr.combined.fna";
    public transient static final String FILENAME_ORF_FULL_FNA = "clr_range_filter_orf.full.combined.fna";
    public transient static final String FILENAME_ORF_CLR_FAA = "clr_range_filter_pep.clr.combined.faa";
    public transient static final String FILENAME_ORF_FULL_FAA = "clr_range_filter_pep.full.combined.faa";
    public transient static final String FILENAME_ORF_COMBINED_FNA = "open_reading_frames.combined.fna";
    public transient static final String FILENAME_ORF_COMBINED_FAA = "open_reading_frames.combined.faa";
    public transient static final String FILENAME_METAGENE = "metagene.combined.raw";
    public transient static final String FILENAME_ORF_FINAL = "metagene_mapped_pep.fasta";
    // Constructors

    /**
     * default constructor
     */
    public MetaGenoOrfCallerResultNode() {
    }

    public String getSubDirectory() {
        return "MetaGenoOrfCallerResult";
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
    public MetaGenoOrfCallerResultNode(String owner, Task task, String name, String description, String visibility,
                                       String relativeSessionPath) {
        super(owner, task, name, description, visibility, Node.DIRECTORY_DATA_TYPE, relativeSessionPath);
    }

    public String getFilePathByTag(String tag) {
        //return null ;
        if (tag.equals(TAG_ORF_OUTPUT)) return getOutputFilePath(FILENAME_ORF_FINAL);
        else if (tag.equals(FILENAME_TRNA)) return getOutputFilePath(FILENAME_TRNA);
        else if (tag.equals(FILENAME_RRNA)) return getOutputFilePath(FILENAME_RRNA);
        else if (tag.equals(FILENAME_ORF_COMBINED_FAA)) return getOutputFilePath(FILENAME_ORF_COMBINED_FAA);
        else if (tag.equals(FILENAME_ORF_COMBINED_FNA)) return getOutputFilePath(FILENAME_ORF_COMBINED_FNA);
        else if (tag.equals(FILENAME_METAGENE)) return getOutputFilePath(FILENAME_METAGENE);
        else if (tag.equals(FILENAME_ORF_FINAL)) return getOutputFilePath(FILENAME_ORF_FINAL);
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
