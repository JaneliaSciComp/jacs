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

package org.janelia.it.jacs.model.user_data.genome;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jan 3, 2008
 * Time: 10:03:16 AM
 */
public class GenomeProjectFileNode extends FileNode {

    public static final String SUB_DIRECTORY = "GenomeProject";

    // The base class attribute dataType will denote genome type
    public static final String DATA_TYPE_VIRAL = "viral";
    public static final String DATA_TYPE_BACTERIAL = "bacterial";
    public static final String DATA_TYPE_ARCHAEA = "archaea";

    // The sequenceType attribute will denote level of genome closure
    public static final String SEQ_TYPE_COMPLETE = "complete";
    public static final String SEQ_TYPE_DRAFT = "draft";

    public static final Integer GENBANK_TYPE_GENBANK = 0;
    public static final Integer GENBANK_TYPE_REFSEQ = 1;

    public static final String GENBANK_FILE_EXTENSION = "gbk";
    public static final String PREFIX_REFSEQ_WGS_DATA = "NZ_";
    public static final String PREFIX_REFSEQ_COMPLETE = "NC_";
    public static final String PREFIX_REFSEQ_ALTERNAME_COMPLETE = "AC_";
    public static final String PREFIX_REFSEQ_NOT_STRUCTURAL = "NS_";

//    private Long taxonId = null;
    //    private Long genomeProjectId = null;
    protected String sequenceType;

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
    public GenomeProjectFileNode(String owner, Task task, String name, String description, String visibility,
                                 String dataType, String sequenceStatus, String relativeSessionPath) {
        super(owner, task, name, description, visibility, Node.DIRECTORY_DATA_TYPE, relativeSessionPath);
        setDataType(dataType);
        setSequenceType(sequenceStatus);
    }

    // No arg Constructor for Hibernate
    public GenomeProjectFileNode() {
    }

    public String getSubDirectory() {
        return SUB_DIRECTORY;
    }

    public String getFilePathByTag(String tag) {
        return null;
    }

//    public Long getTaxonId() {
//        return taxonId;
//    }
//
//    public Long getGenomeProjectId() {
//        return genomeProjectId;
//    }

    //
    public String getSequenceType() {
        return sequenceType;
    }

    public void setSequenceType(String sequenceType) {
        this.sequenceType = sequenceType;
    }
}
