
package org.janelia.it.jacs.model.user_data.hmmer;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.user_data.FileNode;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Oct 22, 2008
 * Time: 3:02:10 PM
 */
public class HmmerPfamDatabaseNode extends FileNode {
    static Logger logger = Logger.getLogger(HmmerPfamDatabaseNode.class.getName());

    public transient static String TAG_PFAM = "TAG_PFAM";
    public transient static String PFAM_FILENAME = "pfam_db_file";

    private Integer numberOfHmms;

    public String getSubDirectory() {
        return "HmmerPfamDatabase";
    }

    public String getFilePathByTag(String tag) {
        if (tag.equals(TAG_PFAM)) {
            return getFilePath(PFAM_FILENAME);
        }
        logger.error("HmmerPfamDatabaseNode: Do not recognize tag type " + tag + " returning null");
        return null;
    }

    public Integer getNumberOfHmms() {
        return numberOfHmms;
    }

    public void setNumberOfHmms(Integer numberOfHmms) {
        this.numberOfHmms = numberOfHmms;
    }
}
