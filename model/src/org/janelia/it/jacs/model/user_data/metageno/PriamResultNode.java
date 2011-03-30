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

package org.janelia.it.jacs.model.user_data.metageno;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Oct 5, 2009
 * Time: 2:12:09 PM
 */
public class PriamResultNode extends FileNode implements IsSerializable, Serializable {

    public transient static final String BASE_OUTPUT_FILENAME = "priamEc";

    // Valid files within PriamResultNode.
    public transient static final String TAG_PRIAM_EC_HIT_FILE = "priam_ec_hits";
    public transient static final String TAG_PRIAM_EC_HIT_TAB_FILE = "priam_ec_hits.ectab";
    public transient static final String TAG_PRIAM_EC_HIT_TAB_PARSED_FILE = "priam_ec_hits.ectab.parsed";

    // Constructors

    /**
     * default constructor
     */
    public PriamResultNode() {
    }

    public String getSubDirectory() {
        return "PriamResult";
    }

    protected long hitCount;

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
    public PriamResultNode(String owner, Task task, String name, String description, String visibility,
                           String relativeSessionPath) {
        super(owner, task, name, description, visibility, Node.DIRECTORY_DATA_TYPE, relativeSessionPath);
    }

    public String getFilePathByTag(String tag) {
        if (tag.equals(TAG_PRIAM_EC_HIT_FILE)) return getFilePath(TAG_PRIAM_EC_HIT_FILE);
        if (tag.equals(TAG_PRIAM_EC_HIT_TAB_FILE)) return getFilePath(TAG_PRIAM_EC_HIT_TAB_FILE);
        if (tag.equals(TAG_PRIAM_EC_HIT_TAB_PARSED_FILE)) return getFilePath(TAG_PRIAM_EC_HIT_TAB_PARSED_FILE);

        return null;
    }

    public long getHitCount() {
        return hitCount;
    }

    public void setHitCount(long hitCount) {
        this.hitCount = hitCount;
    }

}
