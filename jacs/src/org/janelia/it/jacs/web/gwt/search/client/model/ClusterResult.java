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

package org.janelia.it.jacs.web.gwt.search.client.model;

import org.janelia.it.jacs.model.genomics.AnnotationDescription;
import org.janelia.it.jacs.model.tasks.search.SearchTask;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Sep 20, 2007
 * Time: 4:35:55 PM
 */
public class ClusterResult extends CategoryResult {
    private String finalAccession;
    private Integer numCoreClusters;
    private Integer numProteins;
    private Integer numNRProteins;
    private String geneSymbols;
    private String proteinFunctions;
    private List<AnnotationDescription> ecAnnotationDescription;
    private List<org.janelia.it.jacs.model.genomics.AnnotationDescription> goAnnotationDescription;
    private Float rank;

    public Integer getNumProteins() {
        return numProteins;
    }

    public void setNumProteins(Integer numProteins) {
        this.numProteins = numProteins;
    }

    public String getResultType() {
        return SearchTask.TOPIC_CLUSTER;
    }

    public String getFinalAccession() {
        return finalAccession;
    }

    public void setFinalAccession(String finalAccession) {
        this.finalAccession = finalAccession;
    }

    public String getGeneSymbols() {
        return geneSymbols;
    }

    public void setGeneSymbols(String geneSymbols) {
        this.geneSymbols = geneSymbols;
    }

    public String getProteinFunctions() {
        return proteinFunctions;
    }

    public void setProteinFunctions(String proteinFunctions) {
        this.proteinFunctions = proteinFunctions;
    }

    public List getEcAnnotationDescription() {
        return ecAnnotationDescription;
    }

    public void setEcAnnotationDescription(List ecAnnotationDescription) {
        this.ecAnnotationDescription = ecAnnotationDescription;
    }

    public List getGoAnnotationDescription() {
        return goAnnotationDescription;
    }

    public void setGoAnnotationDescription(List goAnnotationDescription) {
        this.goAnnotationDescription = goAnnotationDescription;
    }

    public Integer getNumNRProteins() {
        return numNRProteins;
    }

    public void setNumNRProteins(Integer numNRProteins) {
        this.numNRProteins = numNRProteins;
    }

    public Integer getNumCoreClusters() {
        return numCoreClusters;
    }

    public void setNumCoreClusters(Integer numCoreClusters) {
        this.numCoreClusters = numCoreClusters;
    }

    public Float getRank() {
        return rank;
    }

    public void setRank(Float rank) {
        this.rank = (float) ((int) (100 * rank.floatValue())) / (float) 100.;
    }
}
