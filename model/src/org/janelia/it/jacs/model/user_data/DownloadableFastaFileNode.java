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

import org.janelia.it.jacs.model.tasks.Task;

/**
 * Represents a downloadable fasta file node
 */
public class DownloadableFastaFileNode extends DownloadableFileNode {
    public static final String FASTA_FILE_PREFIX = "node";
    public static final String FASTA_FILE_EXTENSION = ".fasta";
    public static final String INFO_FILE_PREFIX = "node";
    public static final String INFO_FILE_EXTENSION = ".info";

    private Integer sequenceCount;
    private String sequenceType;
    private DataSource dataSource = DataSource.UNKNOWN;

    public DownloadableFastaFileNode() {
    }

    /**
     * old full constructor
     *
     * @param owner               - person who owns the node
     * @param task                - task which created this node
     * @param name                - name of the node
     * @param description         - description of the node
     * @param visibility          - visibility of the node to others
     * @param relativeSessionPath - name of the work session this node belongs to
     * @param dataType            - tag for the node
     */
    public DownloadableFastaFileNode(String owner, Task task, String name, String description, String visibility,
                                     String dataType, String relativeSessionPath) {
        super(owner, task, name, description, visibility, dataType, relativeSessionPath);
    }

    public String getFilePathByTag(String tag) {
        return null;
    }

    protected String getFileExtension() {
        return FASTA_FILE_EXTENSION;
    }

    protected String getFilePrefix() {
        return FASTA_FILE_PREFIX;
    }

    protected String getDescriptorPrefix() {
        return INFO_FILE_PREFIX;
    }

    protected String getDescriptorExtension() {
        return INFO_FILE_EXTENSION;
    }

    public Integer getSequenceCount() {
        return sequenceCount;
    }

    public void setSequenceCount(Integer sequenceCount) {
        this.sequenceCount = sequenceCount;
    }

    public String getSequenceType() {
        return sequenceType;
    }

    public void setSequenceType(String sequenceType) {
        this.sequenceType = sequenceType;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DownloadableFastaFileNode that = (DownloadableFastaFileNode) o;
        return getName().equals(that.getName());
    }

    public int hashCode() {
        return getName().hashCode();
    }

}
