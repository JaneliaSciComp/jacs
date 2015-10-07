
package org.janelia.it.jacs.web.gwt.admin.editproject.server;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.download.Project;
import org.janelia.it.jacs.server.access.DownloadDAO;
import org.janelia.it.jacs.server.access.hibernate.DaoException;
import org.janelia.it.jacs.server.access.hibernate.DownloadDAOImpl;
import org.janelia.it.jacs.web.gwt.admin.editproject.client.ProjectService;
import org.janelia.it.jacs.web.gwt.common.server.JcviGWTSpringController;
import org.janelia.it.jacs.web.gwt.download.server.DbPublicationHelper;
import org.janelia.it.jacs.web.gwt.download.server.PublicationHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ProjectServiceImpl extends JcviGWTSpringController implements ProjectService {   //RemoteServiceServlet

    // Setup default-configured log.
    private Logger logger = Logger.getLogger(DbPublicationHelper.class);

    private DownloadDAO _downloadDAO;

    private PublicationHelper publicationHelper;

    public void setDownloadDAO(DownloadDAOImpl downloadDAO) {
        this._downloadDAO = downloadDAO;
    }

    public void setPublicationHelper(PublicationHelper helper) {
        this.publicationHelper = helper;
    }


    public String getProjectName() {

        String projectName = null;
        List<Project> projects;

        try {

            projects = _downloadDAO.findAllProjects();

            projectName = projects.get(0).getName();


        }
        catch (DaoException ex) {
            logger.error("Failed to get project map. " + ex.getMessage(), ex);
            return null;
        }


        return projectName;
    }


    public Map getAllProjects() {

        // TODO 
        return new HashMap();


    }

    /* this DOESN'T WORK
    public Map getAllProjects() {

        // TODO limit the number of projects that can be retrieved (SEE TaskDAO)
        // TODO this will be necessary for when # of projects becomes too large

        Map symbolVsProject = new HashMap();

        List<Project> projects;

        ArrayList<org.janelia.it.jacs.web.gwt.download.client.model.ProjectImpl> marshalledProjects;

        try {
            projects = _downloadDAO.findAllProjects();
        } catch (DaoException ex) {
            logger.error("Failed to get project map. " + ex.getMessage(), ex);
            return null;
        }


        marshalledProjects = new ArrayList<ProjectImpl>(projects.size());


        for (int i = 0; projects != null && i < projects.size(); i++) {

            org.janelia.it.jacs.model.download.Project nextModel = projects.get(i);

            org.janelia.it.jacs.web.gwt.download.client.model.ProjectImpl nextProject =
                                       new org.janelia.it.jacs.web.gwt.download.client.model.ProjectImpl();

            // USED FOR 1-TO-1 MAPPING //
            String projectSymbol = nextModel.getSymbol();

            // NECESSARY //
            nextProject.setProjectSymbol(projectSymbol);
            nextProject.setProjectName(nextModel.getName());
            nextProject.setDescription(nextModel.getDescription());

            // AUXILIARY //
            nextProject.setPrincipalInvestigators(nextModel.getPrincipal_investigators());
            nextProject.setOrganization(nextModel.getOrganization());
            nextProject.setEmail(nextModel.getEmail());
            nextProject.setWebsite(nextModel.getWebsite_url());
            nextProject.setFundedBy(nextModel.getFunded_by());
            nextProject.setInstitutionalAffiliation(nextModel.getInstitutional_affiliation());

            // create and attach the publication list to the current project
//            List pubList = createPublicationList(nextModel.getPublications(),projectSymbol);
//            nextProject.setPublications(pubList);

            symbolVsProject.put(projectSymbol, nextProject); // symbol - project mapping

            marshalledProjects.add(nextProject);
        }

        // _projectSymbolVsProject = symbolVsProject; // copied from DbPublicationHelper.java

        return symbolVsProject;

    }
    */

    /*
    private List createPublicationList(List<org.janelia.it.jacs.model.download.Publication> publicationSet, String projectSymbol) {
        ArrayList returnList = new ArrayList();

        //TODO consider abandoning the GWT-exchange models and using internal model objs.
        for (org.janelia.it.jacs.model.download.Publication modelPub:
                publicationSet) {

            // Apparently, according to debug-step-through, it is possible for a null to appear here. --LLF, 12/8/6
            if (modelPub == null)
                continue;

            org.janelia.it.jacs.web.gwt.download.client.model.PublicationImpl gwtPub =
                    new org.janelia.it.jacs.web.gwt.download.client.model.PublicationImpl();

            gwtPub.setAccessionNumber(modelPub.getPublicationAccession());
            gwtPub.setAbstract(modelPub.getAbstractOfPublication());
            gwtPub.setTitle(modelPub.getTitle());
            gwtPub.setSummary(modelPub.getSummary());
            gwtPub.setDescription(modelPub.getDescription());

            List authors = createAuthorList(modelPub.getAuthors());
            List dataFiles = createDataFiles(modelPub.getHierarchyRootNodes(), projectSymbol);

            gwtPub.setAuthors(authors);
            gwtPub.setDataFiles(dataFiles);
            List rolledUpArchives = createRolledUpArchives(modelPub.getRolledUpArchives());
            gwtPub.setRolledUpDataArchives(rolledUpArchives);
            DownloadableDataNode subjectDocument = createSubjectDocument(modelPub.getSubjectDocument());
            gwtPub.setSubjectDocument(subjectDocument);

            returnList.add(gwtPub);
        }
        return returnList;
    }
    */


    /* this workded !!!
    public ArrayList getAllProjects() {

        String projectName = null;
        List<Project> projects;
        ArrayList<String> projectNames;

        try {

            projects = _downloadDAO.findAllProjects();

            projectNames = new ArrayList<String>(projects.size());

            for (int i=0; i < projects.size(); i++) {
                projectNames.add(i, projects.get(i).getName());
            }


            //projectName = projects.get(0).getName();

        } catch (DaoException ex) {
            logger.error("Failed to get project map. " + ex.getMessage(), ex);
            return null;
        }


        return projectNames;

    }
    */


}