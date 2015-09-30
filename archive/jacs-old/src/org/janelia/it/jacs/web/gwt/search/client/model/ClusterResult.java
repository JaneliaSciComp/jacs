
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
    private List<AnnotationDescription> goAnnotationDescription;
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
