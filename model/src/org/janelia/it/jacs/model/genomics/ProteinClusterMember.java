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
 * User: cgoina
 * Date: Nov 28, 2007
 * Time: 10:14:14 PM
 */
public class ProteinClusterMember implements Serializable, IsSerializable {
    private String proteinAcc;
    private String coreClusterAcc;
    private String finalClusterAcc;
    private String clusterQuality;
    private String nonRedundantParentAcc;
    private String defline;
    private String proteinFunction;
    private String geneSymbol;
    private Integer length;
    private List<AnnotationDescription> goAnnotationDescription;
    private List<AnnotationDescription> ecAnnotationDescription;

    public ProteinClusterMember() {
    }

    public String getProteinAcc() {
        return proteinAcc;
    }

    public void setProteinAcc(String proteinAcc) {
        this.proteinAcc = proteinAcc;
    }

    public String getCoreClusterAcc() {
        return coreClusterAcc;
    }

    public void setCoreClusterAcc(String coreClusterAcc) {
        this.coreClusterAcc = coreClusterAcc;
    }

    public String getFinalClusterAcc() {
        return finalClusterAcc;
    }

    public void setFinalClusterAcc(String finalClusterAcc) {
        this.finalClusterAcc = finalClusterAcc;
    }

    public String getDefline() {
        return defline;
    }

    public void setDefline(String defline) {
        this.defline = defline;
    }

    public String getNonRedundantParentAcc() {
        return nonRedundantParentAcc;
    }

    public void setNonRedundantParentAcc(String nonRedundantParentAcc) {
        this.nonRedundantParentAcc = nonRedundantParentAcc;
    }

    public String getClusterQuality() {
        return clusterQuality;
    }

    public void setClusterQuality(String clusterQuality) {
        this.clusterQuality = clusterQuality;
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

    public String getProteinFunction() {
        return proteinFunction;
    }

    public void setProteinFunction(String proteinFunction) {
        this.proteinFunction = proteinFunction;
    }

    public List<AnnotationDescription> getEcAnnotationDescription() {
        return ecAnnotationDescription;
    }

    public void setEcAnnotationDescription(List<AnnotationDescription> ecAnnotationDescription) {
        this.ecAnnotationDescription = ecAnnotationDescription;
    }

    public List<AnnotationDescription> getGoAnnotationDescription() {
        return goAnnotationDescription;
    }

    public void setGoAnnotationDescription(List<AnnotationDescription> goAnnotationDescription) {
        this.goAnnotationDescription = goAnnotationDescription;
    }

}
