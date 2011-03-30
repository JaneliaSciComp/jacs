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

package org.janelia.it.jacs.model.genomics;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

public class ScaffoldReadAlignment implements IsSerializable, Serializable {

    private String scaffoldAcc;
    private Scaffold scaffold;
    private String readAcc;
    private Read read;
    private Integer scaffoldBegin;
    private Integer scaffoldEnd;
    private Integer scaffoldOrientation;
    private Integer scaffoldLength;
    private String assemblyDescription;

    public ScaffoldReadAlignment() {
    }

    public String getScaffoldAcc() {
        return scaffoldAcc;
    }

    public void setScaffoldAcc(String scaffoldAcc) {
        this.scaffoldAcc = scaffoldAcc;
    }

    public Scaffold getScaffold() {
        return scaffold;
    }

    public void setScaffold(Scaffold scaffold) {
        this.scaffold = scaffold;
    }

    public String getReadAcc() {
        return readAcc;
    }

    public void setReadAcc(String readAcc) {
        this.readAcc = readAcc;
    }

    public Read getRead() {
        return read;
    }

    public void setRead(Read read) {
        this.read = read;
    }

    public Integer getScaffoldBegin() {
        return scaffoldBegin;
    }

    public Integer getAlignmentBegin_oneResCoords() {
        return scaffoldBegin + 1;
    }

    public void setScaffoldBegin(Integer scaffoldBegin) {
        this.scaffoldBegin = scaffoldBegin;
    }

    public Integer getScaffoldEnd() {
        return scaffoldEnd;
    }

    public Integer getAlignmentEnd_oneResCoords() {
        return scaffoldEnd;
    }

    public void setScaffoldEnd(Integer scaffoldEnd) {
        this.scaffoldEnd = scaffoldEnd;
    }

    public Integer getScaffoldOrientation() {
        return scaffoldOrientation;
    }

    public void setScaffoldOrientation(Integer scaffoldOrientation) {
        this.scaffoldOrientation = scaffoldOrientation;
    }

    public Integer getScaffoldLength() {
        return scaffoldLength;
    }

    public void setScaffoldLength(Integer scaffoldLength) {
        this.scaffoldLength = scaffoldLength;
    }

    public String getAssemblyDescription() {
        return assemblyDescription;
    }

    public void setAssemblyDescription(String assemblyDescription) {
        this.assemblyDescription = assemblyDescription;
    }

}
