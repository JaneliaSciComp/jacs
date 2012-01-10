
package org.janelia.it.jacs.web.gwt.common.client.model.metadata;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.web.gwt.common.client.model.download.DownloadableDataNode;

import java.util.Set;

public class Sample implements IsSerializable, Comparable {

    private Long objectId;
    private String sampleName;

    private String sampleTitle;
    private String experimentId;
    private String[] comments;
    private Double filterMin;
    private Double filterMax;

    private Set<DownloadableDataNode> dataNodes;
    private Set<Site> sites;
    private String sampleAcc;

    public Sample() {
    }

    public Sample(Long objectId, String sampleAcc, String sampleName, String sampleTitle, Set sites, Set dataNodes, Double filterMin, Double filterMax) {
        this.objectId = objectId;
        this.sampleAcc = sampleAcc;
        this.sampleName = sampleName;
        this.sampleTitle = sampleTitle;
        this.sites = sites;
        this.dataNodes = dataNodes;
        this.filterMin = filterMin;
        this.filterMax = filterMax;
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Sample))
            return false;
        else if (objectId == null && ((Sample) obj).getObjectId() == null)
            return true;
        else if (objectId == null || ((Sample) obj).getObjectId() == null)
            return false;
        else
            return objectId.equals(((Sample) obj).getObjectId());
    }

    public int hashCode() {
        return objectId.intValue();
    }

    public Long getObjectId() {
        return objectId;
    }

    public void setObjectId(Long objectId) {
        this.objectId = objectId;
    }

    public String[] getComments() {
        return comments;
    }

    public void setComments(String[] comments) {
        this.comments = comments;
    }

    public String getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(String experimentId) {
        this.experimentId = experimentId;
    }

    public String getSampleName() {
        return sampleName;
    }

    public void setSampleName(String sampleName) {
        this.sampleName = sampleName;
    }

    public String getSampleTitle() {
        return sampleTitle;
    }

    public void setSampleTitle(String sampleTitle) {
        this.sampleTitle = sampleTitle;
    }


    public Set<Site> getSites() {
        return sites;
    }

    public void setSites(Set<Site> sites) {
        this.sites = sites;
    }

    public Set<DownloadableDataNode> getDataNode() {
        return dataNodes;
    }

    public void setDataNode(Set<DownloadableDataNode> dataNode) {
        this.dataNodes = dataNode;
    }

    public void setSampleAcc(String sampleAcc) {
        this.sampleAcc = sampleAcc;
    }

    public String getSampleAcc() {
        return sampleAcc;
    }

    /**
     * Sorts samples alphabetically by sample name
     */
    public int compareTo(Object other) {
        // If other is null, return 1
        Sample otherSample = (Sample) other;
        if (otherSample == null || otherSample.getSampleName() == null)
            return 1;

        // If this is null, return -1
        if (getSampleName() == null)
            return -1;

        // Else compare the sample names
        return getSampleName().compareTo(otherSample.getSampleName());
    }


    public Double getFilterMin() {
        return filterMin;
    }

    public void setFilterMin(Double filterMin) {
        this.filterMin = filterMin;
    }

    public Double getFilterMax() {
        return filterMax;
    }

    public void setFilterMax(Double filterMax) {
        this.filterMax = filterMax;
    }
}
