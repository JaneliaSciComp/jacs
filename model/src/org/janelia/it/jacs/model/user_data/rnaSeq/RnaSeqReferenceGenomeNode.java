
package org.janelia.it.jacs.model.user_data.rnaSeq;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FastaFileNode;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Aug 18, 2010
 * Time: 5:11:10 PM
 */
public class RnaSeqReferenceGenomeNode extends FastaFileNode {

    public RnaSeqReferenceGenomeNode() {
        super();
        this.setSequenceType(FastaFileNode.NUCLEOTIDE);
    }

    public RnaSeqReferenceGenomeNode(String owner, Task task, String name, String description, String visibility,
                                     Integer sequenceCount, String relativeSessionPath) {
        super(owner, task, name, description, visibility, FastaFileNode.NUCLEOTIDE, sequenceCount, relativeSessionPath);
    }

}
