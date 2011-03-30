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

package org.janelia.it.jacs.model.user_data.recruitment;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 28, 2007
 * Time: 10:36:29 AM
 */
public class RecruitmentResultFileNode extends FileNode implements java.io.Serializable, IsSerializable {

    public static final String SUB_DIRECTORY = "Recruitment";

    // valid tags
    public transient static final String TAG_MATES = "mates";
    public transient static final String TAG_IMAGE = "image";
    public transient static final String TAG_BLAST_SVG = "svg";
    public transient static final String TAG_ARCHIVE = "archive";

    // Valid files within BlastResultFileNode.
    public transient static final String MATE_FILENAME = "mates.list";
    public transient static final String IMAGE_FILENAME = "recruit.jpg";
    public transient static final String SVG_FILENAME = "recruit.svg";
    public transient static final String OUTPUT_FILENAME = "blast.result.bz2";
    public transient static final String LEGEND_FILENAME = "legend.txt";
    public transient static final String NUM_HITS_FILENAME = "tmpNumRecruited.txt";

    // Default constructor for hibernate
    public RecruitmentResultFileNode() {
    }

    /**
     * Constructor which most people should use
     *
     * @param owner               owner of the node
     * @param task                task which created the node
     * @param name                name of the node
     * @param description         full description
     * @param visibility          visibility of the node to other people
     * @param relativeSessionPath - name of the work session this node belongs to
     */
    public RecruitmentResultFileNode(String owner, Task task, String name, String description, String visibility,
                                     String relativeSessionPath) {
        super(owner, task, name, description, visibility, Node.DIRECTORY_DATA_TYPE, relativeSessionPath);
    }

    public String getSubDirectory() {
        return SUB_DIRECTORY;
    }

    public String getFilePathByTag(String tag) {
        if (tag.equals(TAG_MATES)) return getFilePath(MATE_FILENAME);
        if (tag.equals(TAG_IMAGE)) return getFilePath(IMAGE_FILENAME);
        if (tag.equals(TAG_BLAST_SVG)) return getFilePath(SVG_FILENAME);
        if (tag.equals(TAG_ARCHIVE)) return getFilePath(OUTPUT_FILENAME);
        return null;
    }

}
