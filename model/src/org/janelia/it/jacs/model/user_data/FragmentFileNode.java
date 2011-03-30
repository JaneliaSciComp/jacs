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
import org.janelia.it.jacs.model.user_data.blast.Blastable;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Nov 14, 2006
 * Time: 1:13:37 PM
 */
public class FragmentFileNode extends FileNode implements java.io.Serializable, IsSerializable, Blastable {

    static Logger logger = Logger.getLogger(FragmentFileNode.class.getName());

    public transient static final String TAG_FRAGMENT = "frg";

    public transient static final String PEPTIDE = "peptide";
    public transient static final String NUCLEOTIDE = "nucleotide";

    // Fields
    private String sequenceType; // either PEPTIDE or NUCLEOTIDE
    private Integer sequenceCount;

    public String getSequenceType() {
        return sequenceType;
    }

    public void setSequenceType(String sequenceType) {
        this.sequenceType = sequenceType;
    }

    /**
     * default constructor
     */
    public FragmentFileNode() {
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
     * @param type                - sequence type for the node
     * @param sequenceCount       - the number of sequences in the file
     */
    public FragmentFileNode(String owner, Task task, String name, String description, String visibility,
                            String type, Integer sequenceCount, String relativeSessionPath) {
        super(owner, task, name, description, visibility, Node.DIRECTORY_DATA_TYPE, relativeSessionPath);
        this.sequenceType = type;
        this.sequenceCount = sequenceCount;
    }

    public String getSubDirectory() {
        return "";
    }

    public String getFragmentFilePath() {
        return getFilePath(sequenceType + "." + TAG_FRAGMENT);
    }

    public String getFilePathByTag(String tag) {
        if (tag.equals(TAG_FRAGMENT)) {
            return getFilePath(sequenceType + "." + TAG_FRAGMENT);
        }
        logger.error("FragmentFileNode: Do not recognize tag type " + tag + " returning null");
        return null;
    }

    public Integer getSequenceCount() {
        return sequenceCount;
    }

    public void setSequenceCount(Integer sequenceCount) {
        this.sequenceCount = sequenceCount;
    }

}