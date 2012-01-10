
package org.janelia.it.jacs.model.user_data.tools;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Mar 30, 2010
 * Time: 2:07:27 PM
 */
public class Clustalw2ResultNode extends FileNode {

    // Valid files within Clustalw2ResultNode.
    public transient static final String TAG_CLUSTALW2_GCG = "msf";
    public transient static final String TAG_CLUSTALW2_GDE = "gde";
    public transient static final String TAG_CLUSTALW2_PIR = "pir";
    public transient static final String TAG_CLUSTALW2_PHYLIP = "phy";
    public transient static final String TAG_CLUSTALW2_NEXUS = "nxs";
    public transient static final String TAG_CLUSTALW2_FASTA = "fsa";
    public transient static final String TAG_CLUSTALW2_NJ = "nj";
    public transient static final String TAG_CLUSTALW2_DIST = "dst";
    public transient static final String TAG_CLUSTALW2_DND = "dnd";
    public transient static final String TAG_CLUSTALW2_CLW = "clw";
    public transient static final String TAG_CLUSTALW2_ALN = "aln";
    public transient static final String TAG_CLUSTALW2_MSF = "msf";
    public transient static final String TAG_CLUSTALW2_BSML = "bsml";
    public transient static final String TAG_CLUSTALW2_LIST = "list";

    public transient static final String BASE_OUTPUT_FILENAME = "clustalw2";

    protected long hitCount;

    // Constructors

    /**
     * default constructor
     */
    public Clustalw2ResultNode() {
    }

    public String getSubDirectory() {
        return "Clustalw2Result";
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
    public Clustalw2ResultNode(String owner, Task task, String name, String description, String visibility,
                               String relativeSessionPath) {
        super(owner, task, name, description, visibility, Node.DIRECTORY_DATA_TYPE, relativeSessionPath);
    }

    public String getFilePathByTag(String tag) {
        //COPY FOR EACH OUTPUT TAG
        if (tag.equals(TAG_CLUSTALW2_GCG)) return getFilePath(BASE_OUTPUT_FILENAME + "." + TAG_CLUSTALW2_GCG);
        if (tag.equals(TAG_CLUSTALW2_GDE)) return getFilePath(BASE_OUTPUT_FILENAME + "." + TAG_CLUSTALW2_GDE);
        if (tag.equals(TAG_CLUSTALW2_PIR)) return getFilePath(BASE_OUTPUT_FILENAME + "." + TAG_CLUSTALW2_PIR);
        if (tag.equals(TAG_CLUSTALW2_PHYLIP)) return getFilePath(BASE_OUTPUT_FILENAME + "." + TAG_CLUSTALW2_PHYLIP);
        if (tag.equals(TAG_CLUSTALW2_NEXUS)) return getFilePath(BASE_OUTPUT_FILENAME + "." + TAG_CLUSTALW2_NEXUS);
        if (tag.equals(TAG_CLUSTALW2_FASTA)) return getFilePath(BASE_OUTPUT_FILENAME + "." + TAG_CLUSTALW2_FASTA);
        if (tag.equals(TAG_CLUSTALW2_NJ)) return getFilePath(BASE_OUTPUT_FILENAME + "." + TAG_CLUSTALW2_NJ);
        if (tag.equals(TAG_CLUSTALW2_DIST)) return getFilePath(BASE_OUTPUT_FILENAME + "." + TAG_CLUSTALW2_DIST);
        if (tag.equals(TAG_CLUSTALW2_DND)) return getFilePath(BASE_OUTPUT_FILENAME + "." + TAG_CLUSTALW2_DND);
        if (tag.equals(TAG_CLUSTALW2_CLW)) return getFilePath(BASE_OUTPUT_FILENAME + "." + TAG_CLUSTALW2_CLW);
        if (tag.equals(TAG_CLUSTALW2_ALN)) return getFilePath(BASE_OUTPUT_FILENAME + "." + TAG_CLUSTALW2_ALN);
        if (tag.equals(TAG_CLUSTALW2_MSF)) return getFilePath(BASE_OUTPUT_FILENAME + "." + TAG_CLUSTALW2_MSF);
        if (tag.equals(TAG_CLUSTALW2_BSML)) return getFilePath(BASE_OUTPUT_FILENAME + "." + TAG_CLUSTALW2_BSML);
        if (tag.equals(TAG_CLUSTALW2_LIST)) return getFilePath(BASE_OUTPUT_FILENAME + "." + TAG_CLUSTALW2_LIST);

        return null;
    }

    public long getHitCount() {
        return hitCount;
    }

    public void setHitCount(long hitCount) {
        this.hitCount = hitCount;
    }

}