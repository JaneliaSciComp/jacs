
package org.janelia.it.jacs.model.user_data.hmmer3;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.user_data.FileNode;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Oct 22, 2008
 * Time: 3:02:10 PM
 */
public class Hmmer3DatabaseNode extends FileNode {
    static Logger logger = Logger.getLogger(Hmmer3DatabaseNode.class.getName());

    public transient static String TAG_PFAM = "TAG_PFAM";
    public transient static String TAG_ALL  = "TAG_ALL";
    public transient static String PFAM_FILENAME = "pfam_db_file";
    public transient static String ALL_FILENAME = "ALL_LIB.HMM";

    private Integer numberOfHmms;

    public String getSubDirectory() {
        return "Hmmer3Databases";
    }

    public String getFilePathByTag(String tag) {
        if (TAG_PFAM.equals(tag)) {
            return getFilePath(PFAM_FILENAME);
        }
        else if (TAG_ALL.equals(tag)) {
            return getFilePath(ALL_FILENAME);
        }
        logger.error("Hmmer3DatabaseNode: Do not recognize tag type " + tag + " returning null");
        return null;
    }

    public Integer getNumberOfHmms() {
        return numberOfHmms;
    }

    public void setNumberOfHmms(Integer numberOfHmms) {
        this.numberOfHmms = numberOfHmms;
    }
}