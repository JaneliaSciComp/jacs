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

package org.janelia.it.jacs.model.user_data.hmmer3;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;

public class HMMER3ResultFileNode extends FileNode {
    protected long hitCount;
    public static final String TAG_OUTPUT_FILE = "hmmer3.out";
    public static final String TAG_PER_SEQ_HITS_FILE = "hmmer3SeqHits.tbl";
    public static final String TAG_PER_DOMAIN_HITS_FILE = "hmmer3DomainHits.tbl";

    public HMMER3ResultFileNode() {
    }

    public String getSubDirectory() {
        return "HMMER3Results";
    }

    public HMMER3ResultFileNode(String owner, Task task, String name, String description, String visibility, String relativeSessionPath) {
        super(owner, task, name, description, visibility, "directory", relativeSessionPath);
    }

    public String getFilePathByTag(String tag) {
        if (tag.equals(TAG_OUTPUT_FILE)) return getFilePath(TAG_OUTPUT_FILE);
        if (tag.equals(TAG_PER_SEQ_HITS_FILE)) return getFilePath(TAG_PER_SEQ_HITS_FILE);
        if (tag.equals(TAG_PER_DOMAIN_HITS_FILE)) return getFilePath(TAG_PER_DOMAIN_HITS_FILE);
        return null;
    }

    public long getHitCount() {
        return hitCount;
    }

    public void setHitCount(long hitCount) {
        this.hitCount = hitCount;
    }


}
