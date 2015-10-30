
package org.janelia.it.jacs.web.gwt.search.client.model;

import org.janelia.it.jacs.model.tasks.search.SearchTask;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Sep 21, 2007
 * Time: 9:29:10 AM
 */
public class ProjectResult extends CategoryResult {
    private String name;
    private String description;
    private String email;
    private String websiteUrl;
    private Boolean released;
    private String investigators;
    private String organization;
    private String fundingSource;
    private String institution;
    private Float rank;

    public String getResultType() {
        return SearchTask.TOPIC_PROJECT;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInvestigators() {
        return investigators;
    }

    public void setInvestigators(String investigators) {
        this.investigators = investigators;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getFundingSource() {
        return fundingSource;
    }

    public void setFundingSource(String fundingSource) {
        this.fundingSource = fundingSource;
    }

    public String getInstitution() {
        return institution;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public Boolean getReleased() {
        return released;
    }

    public void setReleased(Boolean released) {
        this.released = released;
    }

    public Float getRank() {
        return rank;
    }

    public void setRank(Float rank) {
        this.rank = (float) ((int) (100 * rank.floatValue())) / (float) 100.;
    }
}
