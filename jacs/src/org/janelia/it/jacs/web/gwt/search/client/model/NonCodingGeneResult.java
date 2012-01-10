
package org.janelia.it.jacs.web.gwt.search.client.model;

import org.janelia.it.jacs.model.tasks.search.SearchTask;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Sep 21, 2007
 * Time: 10:37:08 AM
 */
public class NonCodingGeneResult extends CategoryResult {
    String type;
    String organism;
    String sampleAccession;
    Integer sequenceLength;

    public String getResultType() {
        return SearchTask.TOPIC_NCGENE;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOrganism() {
        return organism;
    }

    public void setOrganism(String organism) {
        this.organism = organism;
    }

    public String getSampleAccession() {
        return sampleAccession;
    }

    public void setSampleAccession(String sampleAccession) {
        this.sampleAccession = sampleAccession;
    }

    public Integer getSequenceLength() {
        return sequenceLength;
    }

    public void setSequenceLength(Integer sequenceLength) {
        this.sequenceLength = sequenceLength;
    }
}
