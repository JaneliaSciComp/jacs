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
 * This node stores the raw recruitment data: combinedPlusSitePlusMate.hits file
 * User: tsafford
 * Date: Sep 11, 2007
 * Time: 4:15:12 PM
 */
public class RecruitmentFileNode extends FileNode implements java.io.Serializable, IsSerializable {

    public static final String SUB_DIRECTORY = "Recruitment";

    // valid FileNode tags
    public transient static final String TAG_BLAST_HITS = "blast";
    public transient static final String TAG_COMBINED_HITS = "combined";
    public transient static final String TAG_FASTA_READS = "reads";
    public transient static final String TAG_STATS = "statistics";
    public transient static final String TAG_RECRUITMENT_TASKS = "recruitmentTasks";

    // File for tag
    public transient static final String COMBINED_FILENAME = "combinedPlusSitePlusMate.hits";
    public transient static final String BLAST_COMBINED_FILENAME = "blast_comb_file";
    public transient static final String RECRUITED_READ_FASTA_FILENAME = "allRecruitedReads.fasta";
    public transient static final String STATISTICS_FILENAME = "statistics.tab";
    public transient static final String RECRUITMENT_TASKS_FILENAME = "recruitmentTasks.tab";

    // No-arg constructor for Hibernate
    public RecruitmentFileNode() {
    }

    /**
     * Constructor which most people should use
     *
     * @param owner               of the node
     * @param task                task which created the node
     * @param name                name of the node
     * @param description         full description
     * @param visibility          visibility of the node to other people
     * @param relativeSessionPath - name of the work session this node belongs to
     */
    public RecruitmentFileNode(String owner, Task task, String name, String description, String visibility,
                               String relativeSessionPath) {
        super(owner, task, name, description, visibility, Node.DIRECTORY_DATA_TYPE, relativeSessionPath);
    }

    public String getSubDirectory() {
        return SUB_DIRECTORY;
    }

    public String getFilePathByTag(String tag) {
        if (TAG_COMBINED_HITS.equals(tag)) {
            return getFilePath(COMBINED_FILENAME);
        }
        if (TAG_FASTA_READS.equals(tag)) {
            return getFilePath(RECRUITED_READ_FASTA_FILENAME);
        }
        if (TAG_STATS.equals(tag)) {
            return getFilePath(STATISTICS_FILENAME);
        }
        if (TAG_BLAST_HITS.equals(tag)) {
            return getFilePath(BLAST_COMBINED_FILENAME);
        }
        if (TAG_RECRUITMENT_TASKS.equals(tag)) {
            return getFilePath(RECRUITMENT_TASKS_FILENAME);
        }
        return null;
    }

}
