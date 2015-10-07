
package org.janelia.it.jacs.model.user_data.reversePsiBlast;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.user_data.FileNode;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Nov 18, 2008
 * Time: 4:34:05 PM
 */
public class ReversePsiBlastDatabaseNode extends FileNode {
    static Logger logger = Logger.getLogger(ReversePsiBlastDatabaseNode.class.getName());

    public transient static String TAG_RPSDB = "TAG_RPSDB";
    public transient static String RPSBLASTDB_FILENAME = "rps_db_file";

    private Integer sequenceCount = 0;

    public String getSubDirectory() {
        return "ReversePsiBlastDatabase";
    }

    public String getFilePathByTag(String tag) {
        if (tag.equals(TAG_RPSDB)) {
            return getFilePath(RPSBLASTDB_FILENAME);
        }
        logger.error("ReversePsiBlastDatbaseNode: Do not recognize tag type " + tag + " returning null");
        return null;
    }

    public Integer getSequenceCount() {
        return sequenceCount;
    }

    public void setSequenceCount(Integer sequenceCount) {
        this.sequenceCount = sequenceCount;
    }
}
