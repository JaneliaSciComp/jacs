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

package org.janelia.it.jacs.compute.service.common.file;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tnabeel
 * Date: Sep 25, 2007
 * Time: 11:04:38 AM
 */
public class PartitionList implements Serializable {

    private List<File> fileList;
    private long databaseLength; /* NOTE: the meaning of this attribute depends on the context of the node type */

    public PartitionList() {
        fileList = new ArrayList<File>();
    }

    public List<File> getFileList() {
        return fileList;
    }

    public void setFileList(List<File> fileList) {
        this.fileList = fileList;
    }

    public long getDatabaseLength() {
        return databaseLength;
    }

    public void setDatabaseLength(long databaseLength) {
        this.databaseLength = databaseLength;
    }

    public void add(File partitionFile) {
        fileList.add(partitionFile);
    }

    public int size() {
        return fileList.size();
    }

    public Iterator<File> iterator() {
        return fileList.iterator();
    }
}
