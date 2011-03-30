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

package org.janelia.it.jacs.compute.access.search;

import org.janelia.it.jacs.model.genomics.AnnotationDescription;
import org.janelia.it.jacs.model.tasks.search.SearchTask;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Jul 14, 2008
 * Time: 12:21:33 PM
 */
public class ProteinResult extends CategoryResult {
    private String defline; // add back in
    private Integer sequenceLength;
    private String externalSource;
    private String externalAccession;
    private String ncbiGiNumber;
    private String coreCluster;
    private String finalCluster;
    private String geneNames;
    private String proteinFunction;
    private String taxonomy;
    private List<AnnotationDescription> goAnnotationDescription;
    private List<AnnotationDescription> ecAnnotationDescription;
    private Float rank;

    public ProteinResult() {
    }

    public String getResultType() {
        return SearchTask.TOPIC_PROTEIN;
    }

    public String getDefline() {
        return defline;
    }

    public void setDefline(String defline) {
        this.defline = defline;
    }

    public Integer getSequenceLength() {
        return sequenceLength;
    }

    public void setSequenceLength(Integer sequenceLength) {
        this.sequenceLength = sequenceLength;
    }

    public String getExternalSource() {
        return externalSource;
    }

    public void setExternalSource(String externalSource) {
        this.externalSource = externalSource;
    }

    public String getExternalAccession() {
        return externalAccession;
    }

    public void setExternalAccession(String externalAccession) {
        this.externalAccession = externalAccession;
    }

    public String getNcbiGiNumber() {
        return ncbiGiNumber;
    }

    public void setNcbiGiNumber(String ncbiGiNumber) {
        this.ncbiGiNumber = ncbiGiNumber;
    }

    public List<AnnotationDescription> getGoAnnotationDescription() {
        return goAnnotationDescription;
    }

    public void setGoAnnotationDescription(List<org.janelia.it.jacs.model.genomics.AnnotationDescription> goAnnotationDescription) {
        this.goAnnotationDescription = goAnnotationDescription;
    }

    public List<AnnotationDescription> getEcAnnotationDescription() {
        return ecAnnotationDescription;
    }

    public void setEcAnnotationDescription(List<AnnotationDescription> ecAnnotationDescription) {
        this.ecAnnotationDescription = ecAnnotationDescription;
    }

    public String getCoreCluster() {
        return coreCluster;
    }

    public void setCoreCluster(String coreCluster) {
        this.coreCluster = coreCluster;
    }

    public String getFinalCluster() {
        return finalCluster;
    }

    public void setFinalCluster(String finalCluster) {
        this.finalCluster = finalCluster;
    }

    public String getGeneNames() {
        return geneNames;
    }

    public void setGeneNames(String geneNames) {
        this.geneNames = geneNames;
    }

    public String getProteinFunction() {
        return proteinFunction;
    }

    public void setProteinFunction(String proteinFunction) {
        this.proteinFunction = proteinFunction;
    }

    public Float getRank() {
        return rank;
    }

    public void setRank(Float rank) {
        this.rank = (float) ((int) (100 * rank)) / (float) 100.;
    }

    public String getTaxonomy() {
        return taxonomy;
    }

    public void setTaxonomy(String taxonomy) {
        this.taxonomy = taxonomy;
    }
}
