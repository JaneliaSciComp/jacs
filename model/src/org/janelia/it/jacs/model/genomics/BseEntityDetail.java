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
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tnabeel
 * Date: Mar 25, 2008
 * Time: 2:10:49 PM
 */
public class BseEntityDetail implements IsSerializable, Serializable {

    private String acc;
    private Integer dnaBegin;
    private Integer dnaEnd;
    private Integer dnaOrientation;
    private String translationTable;
    private String stop5Prime;
    private String stop3Prime;
    private String proteinFunction;
    private String geneSymbol;
    private Integer length;
    private String type;
    private Integer entityTypeCode;

    private List<AnnotationDescription> goAnnotationDescription;
    private List<AnnotationDescription> ecAnnotationDescription;

    public String getAcc() {
        return acc;
    }

    public void setAcc(String acc) {
        this.acc = acc;
    }

    public Integer getDnaBegin() {
        return dnaBegin;
    }

    public void setDnaBegin(Integer dnaBegin) {
        this.dnaBegin = dnaBegin;
    }

    public Integer getDnaEnd() {
        return dnaEnd;
    }

    public void setDnaEnd(Integer dnaEnd) {
        this.dnaEnd = dnaEnd;
    }

    public Integer getDnaOrientation() {
        return dnaOrientation;
    }

    public void setDnaOrientation(Integer dnaOrientation) {
        this.dnaOrientation = dnaOrientation;
    }

    public String getProteinFunction() {
        return proteinFunction;
    }

    public void setProteinFunction(String proteinFunction) {
        this.proteinFunction = proteinFunction;
    }

    public String getGeneSymbol() {
        return geneSymbol;
    }

    public void setGeneSymbol(String geneSymbol) {
        this.geneSymbol = geneSymbol;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    public List<AnnotationDescription> getGoAnnotationDescription() {
        return goAnnotationDescription;
    }

    public void setGoAnnotationDescription(List<AnnotationDescription> goAnnotationDescription) {
        this.goAnnotationDescription = goAnnotationDescription;
    }

    public List<AnnotationDescription> getEcAnnotationDescription() {
        return ecAnnotationDescription;
    }

    public void setEcAnnotationDescription(List<AnnotationDescription> ecAnnotationDescription) {
        this.ecAnnotationDescription = ecAnnotationDescription;
    }

    public String getTranslationTable() {
        return translationTable;
    }

    public void setTranslationTable(String translationTable) {
        this.translationTable = translationTable;
    }

    public String getStop5Prime() {
        return stop5Prime;
    }

    public void setStop5Prime(String stop5Prime) {
        this.stop5Prime = stop5Prime;
    }

    public String getStop3Prime() {
        return stop3Prime;
    }

    public void setStop3Prime(String stop3Prime) {
        this.stop3Prime = stop3Prime;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getEntityTypeCode() {
        return entityTypeCode;
    }

    public void setEntityTypeCode(Integer entityTypeCode) {
        this.entityTypeCode = entityTypeCode;
    }
}
