
package org.janelia.it.jacs.web.gwt.download.client.model;

import org.janelia.it.jacs.model.download.DataFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Lfoster
 * Date: Sep 25, 2006
 * Time: 12:58:48 PM
 * <p/>
 * Implementation is a simple java bean: set things and get things.
 */
public class ProjectImpl implements Project {
    private List<Publication> _publications;
    private List<DataFile> _rolledUpArchives;
    private String _projectSymbol;
    private String _projectName;
    private String _principalInvestigators;
    private String _description;
    private String _organization;
    private String _email;
    private String _website;
    private String _fundedBy;
    private String _institutionalAffiliation;

    /**
     * Convenience constructor takes all params up front.
     *
     * @param projectName      what to call this--for display purposes, and identification.
     * @param publications     all meta data on publications under the project
     * @param rolledUpArchives combined archives for all papers under the project.
     */
    public ProjectImpl(String projectName, List<Publication> publications, List<DataFile> rolledUpArchives) {
        setPublications(publications);
        setRolledUpArchivesOfPublications(rolledUpArchives);
        setProjectName(projectName);
    }

    //public ProjectImpl(String projectName, List publications, List rolledUpArchives, String description, String
    //    organization, String contact, String website)
    //{
    //    setPublications(publications);
    //    setRolledUpArchivesOfPublications(rolledUpArchives);
    //    setProjectName(projectName);
    //    setDescription(description);
    //    setOrganization(organization);
    //    setContact(contact);
    //    setWebsite(website);
    //}

//    public String toString() {
//        return _projectName + ", " + _publications == null ? " no publications " : _publications.size() + " publications";
//    }

    /**
     * No-args makes external management possible if needed.
     */
    public ProjectImpl() {
    }

    public void setPublications(List<Publication> publications) {
        this._publications = new ArrayList<Publication>();
        this._publications.addAll(publications);
    }

    public List<Publication> getPublications() {
        return _publications;
    }

    public String getProjectName() {
        return _projectName;
    }

    public void setProjectName(String projectName) {
        this._projectName = projectName;
    }

    public String getProjectSymbol() {
        return _projectSymbol;
    }

    public void setProjectSymbol(String projectSymbol) {
        this._projectSymbol = projectSymbol;
    }

    public List<DataFile> getRolledUpArchivesOfPublications() {
        return _rolledUpArchives;
    }

    public void setRolledUpArchivesOfPublications(List<DataFile> rolledUpArchives) {
        _rolledUpArchives = rolledUpArchives;
    }

    public String getDescription() {
        return _description;
    }

    public String getPrincipalInvestigators() {
        return _principalInvestigators;
    }

    public void setEmail(String email) {
        _email = email;
    }

    public void setPrincipalInvestigators(String principalInvestigators) {
        _principalInvestigators = principalInvestigators;
    }

    public String getOrganization() {
        return _organization;
    }

    public String getEmail() {
        return _email;
    }

    public String getWebsite() {
        return _website;
    }

    public void setDescription(String description) {
        _description = description;
    }

    public void setContact(String email) {
        _email = email;
    }

    public void setOrganization(String organization) {
        _organization = organization;
    }

    public void setWebsite(String website) {
        _website = website;
    }

    public String getFundedBy()                   // comma-separated list
    {
        return _fundedBy;
    }

    public String getInstitutionalAffiliation()   // comma-separated list
    {
        return _institutionalAffiliation;
    }

    public void setFundedBy(String fundedBy) {
        _fundedBy = fundedBy;
    }

    public void setInstitutionalAffiliation(String institutionalAffiliation) {
        _institutionalAffiliation = institutionalAffiliation;
    }

    public int compareTo(Object o) {
        if (o == null)
            return 1;

        ProjectImpl other = (ProjectImpl) o;
        return this._projectName.compareTo(other._projectName);
    }

    public boolean equals(Object obj) {
        return (obj instanceof Project) && ((Project) obj).getProjectSymbol().equals(getProjectSymbol());
    }
}
