/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

package org.janelia.it.jacs.model.user_data;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.tasks.Task;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 19, 2010
 * Time: 4:51:05 PM
 */
public class GtfFileNode extends FileNode implements java.io.Serializable, IsSerializable {

    static Logger logger = Logger.getLogger(FastaFileNode.class.getName());

    public transient static final String TAG_GTF = "gtf";

    // Fields
    private Integer sequenceCount;

    /**
     * default constructor
     */
    public GtfFileNode() {
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
     * @param sequenceCount       - number of sequences with features
     */
    public GtfFileNode(String owner, Task task, String name, String description, String visibility,
                       Integer sequenceCount, String relativeSessionPath) {
        super(owner, task, name, description, visibility, Node.DIRECTORY_DATA_TYPE, relativeSessionPath);
        this.sequenceCount = sequenceCount;
    }

    public String getSubDirectory() {
        return "GtfFiles";
    }

    public String getGtfFilePath() {
        return getFilePath("geneTransferFeatures_" + this.getObjectId() + ".gtf");
    }

    public String getFilePathByTag(String tag) {
        if (tag.equals(TAG_GTF)) {
            return getGtfFilePath();
        }
        logger.error("GtfFileNode: Do not recognize tag type " + tag + " returning null");
        return null;
    }

    public Integer getSequenceCount() {
        return sequenceCount;
    }

    public void setSequenceCount(Integer sequenceCount) {
        this.sequenceCount = sequenceCount;
    }

}
