
package org.janelia.it.jacs.web.gwt.search.client.model;

import org.janelia.it.jacs.model.tasks.search.SearchTask;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Sep 21, 2007
 * Time: 10:26:18 AM
 */
public class GenomeResult extends CategoryResult {
    String speciesName;
    String taxonId;
    String locusTag;
    Long genomeLength;
    String description;

    public String getResultType() {
        return SearchTask.TOPIC_GENOME;
    }

    public String getSpeciesName() {
        return speciesName;
    }

    public void setSpeciesName(String speciesName) {
        this.speciesName = speciesName;
    }

    public String getTaxonId() {
        return taxonId;
    }

    public void setTaxonId(String taxonId) {
        this.taxonId = taxonId;
    }

    public String getLocusTag() {
        return locusTag;
    }

    public void setLocusTag(String locusTag) {
        this.locusTag = locusTag;
    }

    public Long getGenomeLength() {
        return genomeLength;
    }

    public void setGenomeLength(Long genomeLength) {
        this.genomeLength = genomeLength;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
