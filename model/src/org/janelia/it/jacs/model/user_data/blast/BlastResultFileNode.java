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

package org.janelia.it.jacs.model.user_data.blast;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Nov 8, 2006
 * Time: 10:06:36 AM
 */
public class BlastResultFileNode extends FileNode implements IsSerializable, Serializable {

    public transient static final String RESULT_EXTENSION_PREFIX = "r";

    // Valid servlet tags
    public transient static final String TAG_HTML = "html";
    public transient static final String TAG_TEXT = "text";
    public transient static final String TAG_IMAGE = "image";
    public transient static final String TAG_BLAST_CSV = "blcsv";
    public transient static final String TAG_READ_META_CSV = "rmetacsv";
    public transient static final String TAG_XML = "xml";
    public transient static final String TAG_ZIP = "zip";
    public transient static final String TAG_BTAB = "btab";
    public transient static final String TAG_TOTAL_BLAST_HITS = "totalBlastHits";

    // Valid files within BlastResultFileNode.
    public transient static final String TEXT_FILENAME = "blast.out";
    public transient static final String HTML_FILENAME = "blast.out.html";
    public transient static final String ALIGNMENT_IMAGE_FILENAME = "blast.out.png";
    public transient static final String BLAST_CSV_FILENAME = "blast.out.csv";
    public transient static final String READ_METADATA_CSV_FILENAME = "read_metadata.csv";
    public transient static final String XML_FILENAME = "blastResults.xml";
    public transient static final String ZIP_FILENAME = "blastResults.zip";
    public transient static final String BTAB_FILENAME = "blastResults.btab";

    public static final String DEFAULT_SERIALIZED_PBRC_FILE_EXTENSION = ".oos";
    public static final String PARSED_BLAST_RESULTS_COLLECTION_BASENAME = "parsedBlastResultsCollection";
    public static final String PARSED_BLAST_RESULTS_COLLECTION_FILENAME = "parsedBlastResultsCollection" + DEFAULT_SERIALIZED_PBRC_FILE_EXTENSION;

    // Fields
    private Long blastHitCount;

    // Constructors

    /**
     * default constructor
     */
    public BlastResultFileNode() {
    }

    public String getSubDirectory() {
        return "BlastResults";
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
    public BlastResultFileNode(String owner, Task task, String name, String description, String visibility,
                               String relativeSessionPath) {
        super(owner, task, name, description, visibility, Node.DIRECTORY_DATA_TYPE, relativeSessionPath);
    }

    public String getFilePathByTag(String tag) {
        if (tag.equals(TAG_HTML)) return getFilePath(HTML_FILENAME);
        if (tag.equals(TAG_TEXT)) return getFilePath(TEXT_FILENAME);
        if (tag.equals(TAG_IMAGE)) return getFilePath(ALIGNMENT_IMAGE_FILENAME);
        if (tag.equals(TAG_BLAST_CSV)) return getFilePath(BLAST_CSV_FILENAME);
        if (tag.equals(TAG_READ_META_CSV)) return getFilePath(READ_METADATA_CSV_FILENAME);
        if (tag.equals(TAG_XML)) return getFilePath(XML_FILENAME);
        if (tag.equals(TAG_ZIP)) return getFilePath(ZIP_FILENAME);
        if (tag.equals(TAG_BTAB)) return getFilePath(BTAB_FILENAME);
        if (tag.equals(TAG_TOTAL_BLAST_HITS)) return getFilePath(TAG_TOTAL_BLAST_HITS);
        return null;
    }

    public String getFilePathByTagAndIndex(String tag, int index) {
        if (tag.equals(TAG_HTML)) return getFilePath(HTML_FILENAME + "_" + index);
        if (tag.equals(TAG_TEXT)) return getFilePath(TEXT_FILENAME + "_" + index);
        if (tag.equals(TAG_IMAGE)) return getFilePath(ALIGNMENT_IMAGE_FILENAME + "_" + index);
        if (tag.equals(TAG_XML)) return getFilePath(TEXT_FILENAME + "_" + index);
        return null;
    }


    public Long getBlastHitCount() {
        return blastHitCount;
    }

    public void setBlastHitCount(Long blastHitCount) {
        this.blastHitCount = blastHitCount;
    }
}
