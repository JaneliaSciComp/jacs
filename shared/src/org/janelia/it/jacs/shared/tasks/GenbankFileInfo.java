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

package org.janelia.it.jacs.shared.tasks;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.File;
import java.io.Serializable;

public class GenbankFileInfo implements Comparable, IsSerializable, Serializable {
    private Long genomeProjectNodeId;
    private File genbankFile;
    private Long length;
    private Long lengthWithoutGaps;

    public GenbankFileInfo(Long genomeProjectNodeId, File genbankFile, Long length, Long lengthMinusGaps) {
        this.genomeProjectNodeId = genomeProjectNodeId;
        this.genbankFile = genbankFile;
        this.length = length;
        this.lengthWithoutGaps = lengthMinusGaps;
    }

    public Long getGenomeProjectNodeId() {
        return genomeProjectNodeId;
    }

    public File getGenbankFile() {
        return genbankFile;
    }

    public Long getLength() {
        return length;
    }

    public Long getLengthWithoutGaps() {
        return lengthWithoutGaps;
    }

    public int compareTo(Object o) {
        GenbankFileInfo gfi2 = (GenbankFileInfo) o;
        return this.length.compareTo(gfi2.getLength());
    }
}

