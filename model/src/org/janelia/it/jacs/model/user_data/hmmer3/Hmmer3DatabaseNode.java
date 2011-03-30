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

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.user_data.FileNode;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Oct 22, 2008
 * Time: 3:02:10 PM
 */
public class Hmmer3DatabaseNode extends FileNode {
    static Logger logger = Logger.getLogger(Hmmer3DatabaseNode.class.getName());

    public transient static String TAG_PFAM = "TAG_PFAM";
    public transient static String TAG_ALL  = "TAG_ALL";
    public transient static String PFAM_FILENAME = "pfam_db_file";
    public transient static String ALL_FILENAME = "ALL_LIB.HMM";

    private Integer numberOfHmms;

    public String getSubDirectory() {
        return "Hmmer3Databases";
    }

    public String getFilePathByTag(String tag) {
        if (TAG_PFAM.equals(tag)) {
            return getFilePath(PFAM_FILENAME);
        }
        else if (TAG_ALL.equals(tag)) {
            return getFilePath(ALL_FILENAME);
        }
        logger.error("Hmmer3DatabaseNode: Do not recognize tag type " + tag + " returning null");
        return null;
    }

    public Integer getNumberOfHmms() {
        return numberOfHmms;
    }

    public void setNumberOfHmms(Integer numberOfHmms) {
        this.numberOfHmms = numberOfHmms;
    }
}