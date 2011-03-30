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

package org.janelia.it.jacs.model.user_data.tools;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;

/**
 * Created by IntelliJ IDEA.
 * User: ekelsey
 * Date: Sep 22, 2010
 * Time: 2:07:27 PM
 */
public class Fasta2BsmlResultNode extends FileNode {

    public transient static final String TAG_FASTA2BSML_LIST = "list";
    public transient static final String TAG_FASTA2BSML_BSML = "bsml";

    public transient static final String BASE_OUTPUT_FILENAME = "fasta2bsml";

    protected long hitCount;

    // Constructors

    /**
     * default constructor
     */
    public Fasta2BsmlResultNode() {
    }

    public String getSubDirectory() {
        return "Fasta2BsmlResult";
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
    public Fasta2BsmlResultNode(String owner, Task task, String name, String description, String visibility,
                             String relativeSessionPath) {
        super(owner, task, name, description, visibility, Node.DIRECTORY_DATA_TYPE, relativeSessionPath);
    }

    public String getFilePathByTag(String tag) {
        if (tag.equals(TAG_FASTA2BSML_BSML)) return getFilePath(BASE_OUTPUT_FILENAME + "." + TAG_FASTA2BSML_BSML);
        if (tag.equals(TAG_FASTA2BSML_LIST)) return getFilePath(BASE_OUTPUT_FILENAME + "." + TAG_FASTA2BSML_LIST);
        return null;
    }

    public long getHitCount() {
        return hitCount;
    }

    public void setHitCount(long hitCount) {
        this.hitCount = hitCount;
    }

}