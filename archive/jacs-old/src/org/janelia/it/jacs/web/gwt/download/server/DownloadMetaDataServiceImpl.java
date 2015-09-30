
package org.janelia.it.jacs.web.gwt.download.server;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.web.gwt.common.client.model.download.DownloadableDataNode;
import org.janelia.it.jacs.web.gwt.common.client.model.metadata.Sample;
import org.janelia.it.jacs.web.gwt.common.client.model.metadata.SampleItem;
import org.janelia.it.jacs.web.gwt.common.client.model.metadata.Site;
import org.janelia.it.jacs.web.gwt.common.client.security.NotLoggedInException;
import org.janelia.it.jacs.web.gwt.common.server.JcviGWTSpringController;
import org.janelia.it.jacs.web.gwt.download.client.DownloadMetaDataService;
import org.janelia.it.jacs.web.gwt.download.client.model.Project;
import org.janelia.it.jacs.web.gwt.download.client.model.ProjectImpl;
import org.janelia.it.jacs.web.gwt.download.client.model.Publication;
import org.janelia.it.jacs.web.gwt.download.client.samples.SampleItemComparator;
import org.janelia.it.jacs.web.security.JacsSecurityUtils;

import java.util.*;

/**
 * Server-side of the Download Page equation.  Called from client, and sends data back for
 * the callback.
 * <p/>
 * User: lfoster
 * Date: Aug 24, 2006
 * Time: 6:09:01 PM
 */
public class DownloadMetaDataServiceImpl extends JcviGWTSpringController implements DownloadMetaDataService {
    private static Logger logger = Logger.getLogger(DownloadMetaDataServiceImpl.class);

    transient private PublicationHelper publicationHelper;

    // Cached samples
    private Map<String, List<Sample>> projectSamples;
    private ArrayList<SampleItem> projectSampleItems;

    public void setPublicationHelper(PublicationHelper helper) {
        logger.debug("Helper Set");
        this.publicationHelper = helper;
    }

    /**
     * Get a mapping of all project symbol and Project objects
     */
    public Map<String, Project> getSymbolToProjectMapping() {
        Map<String, Project> returnMap = null;
        if (publicationHelper == null) {
            logger.error("No model helper.  Therefore, no projectSymbol-to-project map.");
        }
        else {
            returnMap = publicationHelper.getSymbolToProjectMapping();
        }
        return returnMap;
    }

    /**
     * Get a mapping of all publication accessions (String) and Publication objects
     */
    public Map<String, Publication> getAccessionToPublicationMapping() {
        Map<String, Publication> returnMap = null;
        if (publicationHelper == null) {
            logger.error("No model helper.  Therefore, no publicationAccession-to-publication map.");
        }
        else {
            returnMap = publicationHelper.getAccessionToPublicationMapping();
        }
        return returnMap;
    }

    public void saveOrUpdateProject(ProjectImpl project) {

        if (publicationHelper == null) {
            logger.error("No model helper: Cannot save or update Project.");
        }
        else {
            publicationHelper.saveOrUpdateProject(project);
        }
    }

    /**
     * Given name of project, return object for project.
     *
     * @param projectName what project
     * @return its object.
     */
    public Project getProjectByName(String projectName) {
        Project returnProject = null;
        if (publicationHelper == null)
            logger.error("No model helper.  Therefore, no project");
        else
            returnProject = publicationHelper.getProjectByName(projectName);

        return returnProject;
    }

    /**
     * Given the symbol of project, return object for project.
     *
     * @param projectSymbol what project
     * @return its object.
     */
    public Project getProjectBySymbol(String projectSymbol) {
        Project returnProject = null;
        if (publicationHelper == null)
            logger.error("No model helper.  Therefore, no project");
        else
            returnProject = publicationHelper.getProjectBySymbol(projectSymbol);

        return returnProject;
    }

    public Publication getPublicationByAccession(String publicationAccession) {
        Publication publication = null;
        if (publicationHelper == null) {
            logger.error("No model helper.  Therefore, no publicationAccession-to-publication map.");
        }
        else {
            publication = publicationHelper.getPublicationByAccession(publicationAccession);
        }
        return publication;
    }

    /**
     * Get all new files.  Simple list, with no hierarchy.
     *
     * @return list of new files.
     */
    public List<String> getNewFiles() {
        List<String> returnList = new ArrayList<String>();
        if (publicationHelper == null) {
            logger.error("No model helper.  Therefore, no new files for download");
        }
        else {
            try {
                returnList = publicationHelper.getNewFiles();
                logger.info("Got list of new fles of " + returnList);
            }
            catch (Exception ex) {
                logger.error("Failed to get list of new files: " + ex.getMessage());
            }
        }
        return returnList;
    }

    public Boolean checkFileLocation(String fileLocation) throws NotLoggedInException {
        if (!JacsSecurityUtils.isAuthenticated(getThreadLocalRequest())) {
            logger.debug("checkFileLocation() - denying un-authenticated user");
            throw new NotLoggedInException("You must be logged in to download this file.");
        }

        Boolean returnValue = Boolean.FALSE;
        if (publicationHelper == null) {
            logger.error("No model helper.  Therefore, no new files for download");
        }
        else {
            try {
                returnValue = publicationHelper.checkFileLocation(fileLocation);
            }
            catch (Exception ex) {
                logger.error("Failed to find file: " + fileLocation + " result: " + ex.getMessage());
            }
        }

        return returnValue;
    }

    public List<Sample> getProjectSamples(String projectSymbol) {
        List<Sample> samples = new ArrayList<Sample>();

        if (publicationHelper == null) {
            logger.error("No model helper.  Therefore, no new files for download");
        }
        else {
            try {
                samples = publicationHelper.getProjectSamples(projectSymbol);
                logger.debug("Got " + samples.size() + " samples from publicationHelper");
            }
            catch (Exception ex) {
                logger.error("Exception getting samples: " + ex.getMessage());
            }
        }

        return samples;
    }

    synchronized public Map<String, List<Sample>> getProjectSamplesByProject() {
        if (projectSamples == null) {
            projectSamples = new HashMap<String, List<Sample>>();
            if (publicationHelper == null) {
                logger.error("No model helper.  Therefore, no new files for download");
            }
            else {
                try {
                    projectSamples = publicationHelper.getProjectSamplesByProject();
                    logger.debug("Got samples for " + projectSamples.size() + " projects from publicationHelper");
                }
                catch (Exception ex) {
                    logger.error("Exception getting samples: " + ex.getMessage());
                }
            }
        }
        return projectSamples;
    }

    synchronized public Integer getNumProjectSampleInfo(List<String> selectedProjectNames) {
        if (projectSampleItems == null)
            getProjectSampleInfo(null);

        int numProjects;
        if (projectSampleItems == null)
            numProjects = 0;
        else if (selectedProjectNames == null)
            numProjects = projectSampleItems.size();
        else { // count the number of projects matching selected project names
            numProjects = 0;
            for (SampleItem sampleItem : projectSampleItems) {
                if (selectedProjectNames.contains(sampleItem.getProject()))
                    numProjects++;
            }
        }

        return numProjects;
    }

    synchronized public List<SampleItem> getProjectSampleInfo(int startIndex, int endIndex, SortArgument[] sortArgs, List<String> selectedProjectNames) {
        if (projectSampleItems == null)
            getProjectSampleInfo(null);
        if (projectSampleItems == null)
            return null;

        // Sort, using a local shallow copy of the list so the sorting is local and temporary (not retained for other users)
        ArrayList<SampleItem> tmplist;
        if (selectedProjectNames == null)
            tmplist = (ArrayList<SampleItem>) projectSampleItems.clone();
        else { // Copy just the selected items into the array that will be sorted
            tmplist = new ArrayList<SampleItem>();
            for (SampleItem sampleItem : projectSampleItems) {
                if (selectedProjectNames.contains(sampleItem.getProject()))
                    tmplist.add(sampleItem);
            }
        }
        if (sortArgs != null && sortArgs.length > 0)
            Collections.sort(tmplist, new SampleItemComparator(sortArgs[0]));

        // Extract the subset requested
        List<SampleItem> items = new ArrayList<SampleItem>(endIndex - startIndex + 1);
        if (tmplist != null) {
            for (int i = startIndex; i < endIndex && i < tmplist.size(); i++)
                items.add(tmplist.get(i));
        }

        return items;
    }

    synchronized public List<SampleItem> getProjectSampleInfo(SortArgument[] sortArgs) {
        if (projectSampleItems == null) {
            if (projectSamples == null)
                getProjectSamplesByProject();
            if (projectSamples == null)  // validate that there are some samples
                return null;

            projectSampleItems = new ArrayList<SampleItem>();
            for (Map.Entry<String, List<Sample>> entry : projectSamples.entrySet()) {
                String project = entry.getKey();
                List<Sample> samples = entry.getValue();
                for (Sample sample : samples) {
                    for (Site site : sample.getSites()) {
                        Set<DownloadableDataNode> dataFiles = sample.getDataNode();
                        if (dataFiles == null || dataFiles.size() == 0) {
                            projectSampleItems.add(new SampleItem(project, sample, null, site));
                        }
                        else {
                            for (DownloadableDataNode dataFile : dataFiles)
                                projectSampleItems.add(new SampleItem(project, sample, dataFile, site));
                        }
                    }
                }
            }
        }

        return projectSampleItems;
    }

    public List<DownloadableDataNode> getDownloadableFilesBySampleAcc(String sampleAcc) {
        List<DownloadableDataNode> samples = new ArrayList<DownloadableDataNode>();
        if (publicationHelper == null) {
            logger.error("No model helper.  Therefore, no new files for download");
        }
        else {
            try {
                samples = publicationHelper.getDownloadableFilesBySampleAcc(sampleAcc);
                logger.debug("Got " + samples.size() + " samples from publicationHelper");
            }
            catch (Exception ex) {
                logger.error("Exception getting downloadable files for " + sampleAcc, ex);
            }
        }
        return samples;
    }

}
