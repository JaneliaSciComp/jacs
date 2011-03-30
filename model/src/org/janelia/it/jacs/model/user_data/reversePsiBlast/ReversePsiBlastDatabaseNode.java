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

package org.janelia.it.jacs.model.user_data.reversePsiBlast;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.user_data.FileNode;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Nov 18, 2008
 * Time: 4:34:05 PM
 */
public class ReversePsiBlastDatabaseNode extends FileNode {
    static Logger logger = Logger.getLogger(ReversePsiBlastDatabaseNode.class.getName());

    public transient static String TAG_RPSDB = "TAG_RPSDB";
    public transient static String RPSBLASTDB_FILENAME = "rps_db_file";

    private Integer sequenceCount = 0;

    public String getSubDirectory() {
        return "ReversePsiBlastDatabase";
    }

    public String getFilePathByTag(String tag) {
        if (tag.equals(TAG_RPSDB)) {
            return getFilePath(RPSBLASTDB_FILENAME);
        }
        logger.error("ReversePsiBlastDatbaseNode: Do not recognize tag type " + tag + " returning null");
        return null;
    }

    public Integer getSequenceCount() {
        return sequenceCount;
    }

    public void setSequenceCount(Integer sequenceCount) {
        this.sequenceCount = sequenceCount;
    }
}
