
package org.janelia.it.jacs.web.gwt.search.client.model;

import org.janelia.it.jacs.model.tasks.search.SearchTask;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Sep 21, 2007
 * Time: 9:55:03 AM
 */
public class SampleResult extends CategoryResult {
    private String accession;
    private String name;
    private String project;
    private String region;
    private String location;
    private String country;
    private String habitat;
    private String depth;
    private String hostOrganism;
    private String filterSize;
    private Integer readCount;
    private Float rank;

    List<SiteDescription> sites = new ArrayList<SiteDescription>();

    public String getResultType() {
        return SearchTask.TOPIC_SAMPLE;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getReadCount() {
        return readCount;
    }

    public void setReadCount(Integer readCount) {
        this.readCount = readCount;
    }

    public List<SiteDescription> getSites() {
        return sites;
    }

    public void setSites(List<SiteDescription> sites) {
        this.sites = sites;
    }

    public String getAccession() {
        return accession;
    }

    public void setAccession(String accession) {
        this.accession = accession;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getFilterSize() {
        return filterSize;
    }

    public void setFilterSize(String filterSize) {
        this.filterSize = filterSize;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getHabitat() {
        return habitat;
    }

    public void setHabitat(String habitat) {
        this.habitat = habitat;
    }

    public String getDepth() {
        return depth;
    }

    public void setDepth(String depth) {
        this.depth = depth;
    }

    public String getHostOrganism() {
        return hostOrganism;
    }

    public void setHostOrganism(String hostOrganism) {
        this.hostOrganism = hostOrganism;
    }

    public Float getRank() {
        return rank;
    }

    public void setRank(Float rank) {
        this.rank = (float) ((int) (100 * rank.floatValue())) / (float) 100.;
    }
}
