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

package org.janelia.it.jacs.shared.dma;

import java.io.File;

/**
 * This class represents a file or directory used by DmaThreads and instances of DmaAction
 *
 * @author Tareq Nabeel
 */
public class DmaFile {

    private File file;

    private long javaFileSize;

    private String absolutePath;

    private String name;

    private long sequenceCount;

    public DmaFile(File file) {
        this.file = file;
        javaFileSize = file.length();
        absolutePath = file.getAbsolutePath();
        name = file.getName();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getTargetSeqCount() {
        return sequenceCount;
    }

    public void setTargetSeqCount(long sequenceCount) {
        this.sequenceCount = sequenceCount;
    }

    public String getAbsolutePath() {
        return absolutePath;
    }

    public long getJavaFileSize() {
        return javaFileSize;
    }

    public File getFile() {
        return file;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DmaFile dmaFile = (DmaFile) o;

        return absolutePath.equals(dmaFile.absolutePath);
    }

    public int hashCode() {
        return absolutePath.hashCode();
    }

    public String toString() {
        return getName();
    }
}
