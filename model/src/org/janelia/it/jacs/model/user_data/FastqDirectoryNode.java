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
 * Date: Feb 2, 2010
 * Time: 2:54:38 PM
 * To change this template use File | Settings | File Templates.
 * <p/>
 * This node is for the FASTQ standard:
 * <p/>
 * http://en.wikipedia.org/wiki/FASTQ_format
 * <p/>
 * In particular, it is for data from the Illumina sequencer.
 * <p/>
 * The data can be either non-paired, or paired.
 * <p/>
 * If the data is non-paired, each file will typically be from one 'run' and
 * have the names:
 * <p/>
 * s1.fq, s2.fq, ....
 * <p/>
 * If the data is paired, the files will have this naming convention within the directory:
 * <p/>
 * s1_1.fq, s2_1.fq, s3_1.fq, ...  (left ended reads)
 * s1_2.fq, s2_2.fq, s3_2.fq, ...  (right ended reads)
 * <p/>
 * Note that this convention is *required* for tools such as tophat.
 */
public class FastqDirectoryNode extends FileNode implements java.io.Serializable, IsSerializable {

    static Logger logger = Logger.getLogger(FastqDirectoryNode.class.getName());

    public transient static final String TAG_FASTQ = "fq";
    public transient static final String PAIRED_DATA = "paired";
    public transient static final String UNPAIRED_DATA = "unpaired";

    // Fields
    private Integer sequenceCount;
    private String isPairedData;  // using a string to avoid adding new column to node table

    /**
     * default constructor
     */
    public FastqDirectoryNode() {
    }

    /**
     * constructor
     *
     * @param owner                 - person who owns the node
     * @param task                  - task which created this node
     * @param name                  - name of the node
     * @param description           - description of the node
     * @param visibility            - visibility of the node to others
     * @param relativeSessionPath   - name of the work session this node belongs to
     * @param isPairedData          - the fastq files are paired data and are separated into left and right subfiles
     * @param mateMeanInnerDistance - ONLY relevant for paired cases, the bp length separating mates, a library characeristic. Unpaired val should be 0.
     * @param sequenceCount         - number of sequences
     */
    public FastqDirectoryNode(String owner, Task task, String name, String description, String visibility,
                              Boolean isPairedData, Long mateMeanInnerDistance, Integer sequenceCount, String relativeSessionPath) {
        super(owner, task, name, description, visibility, Node.DIRECTORY_DATA_TYPE, relativeSessionPath);
        if (isPairedData) {
            this.isPairedData = PAIRED_DATA;
        }
        else {
            this.isPairedData = UNPAIRED_DATA;
        }
        this.sequenceCount = sequenceCount;
        this.length = mateMeanInnerDistance;
    }

    public String getSubDirectory() {
        return "FastqDirectories";
    }

    public Integer getSequenceCount() {
        return sequenceCount;
    }

    public void setSequenceCount(Integer sequenceCount) {
        this.sequenceCount = sequenceCount;
    }

    // Necessary for hibernate
    private String getIsPairedData() {
        return isPairedData;
    }

    // Necessary for hibernate
    private void setIsPairedData(String isPairedData) {
        this.isPairedData = isPairedData;
    }

    public Boolean isPairedData() {
        return isPairedData.equals(PAIRED_DATA);
    }

    public void setPairedData(Boolean isPairedData) {
        if (isPairedData) {
            this.isPairedData = PAIRED_DATA;
        }
        else {
            this.isPairedData = UNPAIRED_DATA;
        }
    }

    public Long getMateMeanInnerDistance() {
        return this.length;
    }

    public void setMateMeanInnerDistance(long mateMeanInnerDistance) {
        this.length = mateMeanInnerDistance;
    }

    public String getFilePathByTag(String tag) {
        logger.error("FastqDirectoryNode: Do not recognize tag type " + tag + " returning null");
        return null;
    }

}