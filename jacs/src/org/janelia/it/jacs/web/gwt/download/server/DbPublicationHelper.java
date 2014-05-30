
package org.janelia.it.jacs.web.gwt.download.server;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.download.Author;
import org.janelia.it.jacs.model.download.DataFile;
import org.janelia.it.jacs.model.download.HierarchyNode;
import org.janelia.it.jacs.model.metadata.BioMaterial;
import org.janelia.it.jacs.model.metadata.GeoPoint;
import org.janelia.it.jacs.model.metadata.Sample;
import org.janelia.it.jacs.server.access.DownloadDAO;
import org.janelia.it.jacs.server.access.hibernate.DaoException;
import org.janelia.it.jacs.web.gwt.common.client.model.download.DownloadableDataNode;
import org.janelia.it.jacs.web.gwt.common.client.model.download.DownloadableDataNodeImpl;
import org.janelia.it.jacs.web.gwt.common.client.model.metadata.Site;
import org.janelia.it.jacs.web.gwt.download.client.model.Project;
import org.janelia.it.jacs.web.gwt.download.client.model.ProjectImpl;
import org.janelia.it.jacs.web.gwt.download.client.model.Publication;
import org.janelia.it.jacs.web.gwt.download.client.model.PublicationImpl;

import java.io.File;
import java.util.*;

/**
 * User: lfoster
 * Date: Oct 23, 2006
 * Time: 5:45:56 PM
 * <p/>
 * Database Publication Helper: uses information about publications, that is
 * stored in the JaCS database.
 */
public class DbPublicationHelper implements PublicationHelper {

    // Setup default-configured log.
    private Logger logger = Logger.getLogger(DbPublicationHelper.class);

    //TODO consider: non-cached.
    private Map<String, Project> _projectSymbolVsProject;
    private Map<String, Publication> _publicationAccessionVsPublication;
    private String _projectBaseLocation;

    private DownloadDAO _downloadDAO;
    //private HashMap<String, HashSet<org.janelia.it.jacs.web.gwt.download.client.model.Sample>> _publicationSamples =
    //    new HashMap<String, HashSet<org.janelia.it.jacs.web.gwt.download.client.model.Sample>>(); // projectName->Set of samples mapping

    public DbPublicationHelper() {
        _publicationAccessionVsPublication = new HashMap<String, Publication>();
    }

    public DbPublicationHelper(String baseFileLocation) {
        _projectBaseLocation = baseFileLocation;
    }

    //--------------------------------------DEPENDENCIES
    /**
     * Place to inject the DAO that this class depends ;-) on.
     *
     * @param downloadDAO used to fetch from database.
     */
    public void setDownloadDAO(DownloadDAO downloadDAO) {
        _downloadDAO = downloadDAO;

    }

    /**
     * Place to inject the base location for projects.
     *
     * @param baseFileLocation where it is.
     */
    public void setProjectBaseLocation(String baseFileLocation) {
        _projectBaseLocation = baseFileLocation;
    }

    //--------------------------------------IMPLEMENTS PublicationHelper
    /**
     * Gets project info from db.
     *
     * @return projectname vs project object.
     */
    public Map<String, Project> getSymbolToProjectMapping() {
        if (_projectSymbolVsProject == null) {
            populateProjects();
        }
        return _projectSymbolVsProject;
    }

    /**
     * Get all publications from db (using accession-to-publication mapping).
     *
     * @return publication accession vs publication
     */
    public Map<String, Publication> getAccessionToPublicationMapping() {
        if (_publicationAccessionVsPublication.size() == 0) {
            populatePublications();
        }
        return _publicationAccessionVsPublication;
    }

    public void saveOrUpdateProject(ProjectImpl gwtProject) {

        org.janelia.it.jacs.model.download.Project modelProject;

        try {
            modelProject = _downloadDAO.findReleasedProjectBySymbol(gwtProject.getProjectSymbol());
        }
        catch (DaoException ex) {
            logger.error("Failed to get project map. " + ex.getMessage(), ex);
            return;
        }

        // make updates/changes

        // project details
        modelProject.setName(gwtProject.getProjectName());
        modelProject.setSymbol(gwtProject.getProjectSymbol());
        modelProject.setPrincipal_investigators(gwtProject.getPrincipalInvestigators());
        modelProject.setInstitutional_affiliation(gwtProject.getInstitutionalAffiliation());
        modelProject.setOrganization(gwtProject.getOrganization());
        modelProject.setFunded_by(gwtProject.getFundedBy());
        modelProject.setWebsite_url(gwtProject.getWebsite());
        modelProject.setEmail(gwtProject.getEmail());

        // project description
        modelProject.setDescription(gwtProject.getDescription());

        // project publications
        // TODO: make all necessary updates on a project's publications also. Right now, kinda messy.


        _downloadDAO.saveOrUpdateProject(modelProject);

        // clear the "cache" on server side
        _projectSymbolVsProject = null;


    }

    /**
     * Get a project, from the DB, by name.
     *
     * @param projectName which to return?
     * @return the project.
     */
    public Project getProjectByName(String projectName) {
        if (_projectSymbolVsProject == null) {
            populateProjects();
        }
        Project result = null;
        for (Object o : _projectSymbolVsProject.values()) {
            Project project =
                    (Project) o;
            if (projectName.equals(project.getProjectName())) {
                result = project;
                break;
            }
        }
        return result;
    }

    /**
     * Get a project, from the DB, by name.
     *
     * @param projectSymbol which to return?
     * @return the project.
     */
    public Project getProjectBySymbol(String projectSymbol) {
        if (_projectSymbolVsProject == null) {
            populateProjects();
        }
        return _projectSymbolVsProject.get(projectSymbol);
    }

    public Publication getPublicationByAccession(String publicationAccession) {
        Publication gwtPub = null;
        if (_publicationAccessionVsPublication.size() > 0) {
            gwtPub = _publicationAccessionVsPublication.get(publicationAccession);
        }
        if (gwtPub == null) {
            org.janelia.it.jacs.model.download.Publication modelPub;
            try {
                modelPub = _downloadDAO.findPublicationByAccessionNo(publicationAccession);
                gwtPub = createPublicationFromModel(modelPub);
                _publicationAccessionVsPublication.put(modelPub.getPublicationAccession(), gwtPub);
            }
            catch (Exception e) {
                logger.error("Failed to get publication " + publicationAccession, e);
            }
        }
        return gwtPub;
    }

    /**
     * Get list of new data files from the database.  NYI.
     *
     * @return list.
     */
    public List<String> getNewFiles() {
        return null;
    }

    /**
     * Answer the question: does this file exist?
     *
     * @param fileLocation what to look for.
     * @return true: found/false: not found or other IO,etc. failure.
     */
    public Boolean checkFileLocation(String fileLocation) {
        try {
            File finder = new File(_projectBaseLocation + fileLocation);
            logger.info("Checking location: " + _projectBaseLocation + fileLocation);
            return finder.exists();
        }
        catch (Exception ex) {
            logger.error(ex.getMessage());  // Let the developers know...
            logger.error(ex);
            return Boolean.FALSE;
        }

    }

    public List<org.janelia.it.jacs.web.gwt.common.client.model.metadata.Sample> getProjectSamples(String projectSymbol) {
        logger.debug("getProjectSamples()");
        List<org.janelia.it.jacs.web.gwt.common.client.model.metadata.Sample> samples = null;
        try {
            samples = remarshallSamples(_downloadDAO.findProjectSamples(projectSymbol));
        }
        catch (DaoException ex) {
            logger.error("DaoException: " + ex.getMessage(), ex);
        }
        catch (Throwable e) {
            logger.error("Throwable: " + e.getMessage(), e);
        }

        return samples;
    }

    public Map<String, List<org.janelia.it.jacs.web.gwt.common.client.model.metadata.Sample>> getProjectSamplesByProject() {
        logger.debug("getProjectSamples()");
        Map<String, List<org.janelia.it.jacs.web.gwt.common.client.model.metadata.Sample>> clientSamples = new HashMap<String, List<org.janelia.it.jacs.web.gwt.common.client.model.metadata.Sample>>();
        try {
            List<Object[]> results = _downloadDAO.findProjectSamplesByProject();
            for (Object[] row : results) {
                Sample modelSamples = (Sample) row[0];
                String projectName = (String) row[1];
                addToList(clientSamples, remarshallSample(modelSamples), projectName);
            }
        }
        catch (DaoException ex) {
            logger.error("DaoException: " + ex.getMessage(), ex);
        }
        catch (Throwable e) {
            logger.error("Throwable: " + e.getMessage(), e);
        }
        return clientSamples;
    }

    private void addToList(
            Map<String, List<org.janelia.it.jacs.web.gwt.common.client.model.metadata.Sample>> projectMap,
            org.janelia.it.jacs.web.gwt.common.client.model.metadata.Sample sample,
            String projectName) {
        List<org.janelia.it.jacs.web.gwt.common.client.model.metadata.Sample> sampleList = projectMap.get(projectName);
        if (sampleList == null) {
            sampleList = new ArrayList<org.janelia.it.jacs.web.gwt.common.client.model.metadata.Sample>();
            projectMap.put(projectName, sampleList);
        }
        sampleList.add(sample);
    }

    public List<DownloadableDataNode> getDownloadableFilesBySampleAcc(String sampleAcc) {
        logger.debug("getDownloadableFilesBySampleAcc()");
        List<DownloadableDataNode> gwtDataFiles = new ArrayList<DownloadableDataNode>();
        try {
            List<DataFile> modelDataFiles = _downloadDAO.findDataFilesBySampleAcc(sampleAcc);
            for (DataFile modelDataFile : modelDataFiles) {
                DownloadableDataNodeImpl gwtDataFile = convertDataFileFromModel(modelDataFile);
                gwtDataFiles.add(gwtDataFile);
            }
        }
        catch (DaoException ex) {
            logger.error("DaoException: " + ex.getMessage(), ex);
        }
        catch (Throwable e) {
            logger.error("Throwable: " + e.getMessage(), e);
        }
        return gwtDataFiles;
    }

    //---------------------------------------HELPERS
    /**
     * Populates the cache for project information.
     */
    private void populateProjects() {
        Map<String, Project> symbolVsProject = new HashMap<String, Project>();
        List<org.janelia.it.jacs.model.download.Project> projects;

        try {
            projects = _downloadDAO.findAllProjects();
        }
        catch (DaoException ex) {
            logger.error("Failed to get project map. " + ex.getMessage(), ex);
            return;
        }

        for (int i = 0; projects != null && i < projects.size(); i++) {
            org.janelia.it.jacs.model.download.Project nextModel = projects.get(i);
            String projectSymbol = nextModel.getSymbol();
            ProjectImpl nextProject =
                    new ProjectImpl();
            nextProject.setProjectSymbol(projectSymbol);
            nextProject.setProjectName(nextModel.getName());
            nextProject.setDescription(nextModel.getDescription());
            nextProject.setPrincipalInvestigators(nextModel.getPrincipal_investigators());
            nextProject.setOrganization(nextModel.getOrganization());
            nextProject.setEmail(nextModel.getEmail());
            nextProject.setWebsite(nextModel.getWebsite_url());
            nextProject.setFundedBy(nextModel.getFunded_by());
            nextProject.setInstitutionalAffiliation(nextModel.getInstitutional_affiliation());
            // create and attach the publication list to the current project
            List<Publication> pubList = createPublicationList(nextModel.getPublications());
            nextProject.setPublications(pubList);
            symbolVsProject.put(projectSymbol, nextProject);
        }

        _projectSymbolVsProject = symbolVsProject;

    }

    /**
     * Populates the cache for publication information.
     */
    private void populatePublications() {
        Map<String, Publication> accessionVsPublication = new HashMap<String, Publication>();
        List<org.janelia.it.jacs.model.download.Publication> publications;

        try {
            publications = _downloadDAO.findAllPublications();
        }
        catch (DaoException ex) {
            logger.error("Failed to get publication map. " + ex.getMessage(), ex);
            return;
        }

        for (org.janelia.it.jacs.model.download.Publication modelPub : publications) {
            String publicationAccession = modelPub.getPublicationAccession();
            Publication gwtPub =
                    createPublicationFromModel(modelPub);
            accessionVsPublication.put(publicationAccession, gwtPub);
        }

        _publicationAccessionVsPublication = accessionVsPublication;

    }

    /**
     * Given the input set of publications (model), produce an output list
     * of publications (GWT-exchange).
     *
     * @param publicationSet model set
     * @return GWT-compatible list.
     */
    private List<Publication> createPublicationList(List<org.janelia.it.jacs.model.download.Publication> publicationSet) {
        ArrayList<Publication> returnList = new ArrayList<Publication>();

        //TODO consider abandoning the GWT-exchange models and using internal model objs.
        for (org.janelia.it.jacs.model.download.Publication modelPub : publicationSet) {
            // Apparently, according to debug-step-through, it is possible for a null to appear here. --LLF, 12/8/6
            if (modelPub == null) {
                continue;
            }
            Publication gwtPub = createPublicationFromModel(modelPub);
            returnList.add(gwtPub);
        }
        return returnList;
    }

    /**
     * Given the a model publication return a GWT publication
     *
     * @param modelPub
     * @return GWT-compatible publication.
     */
    private Publication createPublicationFromModel(org.janelia.it.jacs.model.download.Publication modelPub) {
        if (modelPub == null) {
            return null;
        }
        PublicationImpl gwtPub =
                new PublicationImpl();
        gwtPub.setAccessionNumber(modelPub.getPublicationAccession());
        gwtPub.setAbstract(modelPub.getAbstractOfPublication());
        gwtPub.setTitle(modelPub.getTitle());
        gwtPub.setSummary(modelPub.getSummary());
        gwtPub.setDescription(modelPub.getDescription());

        List<Author> authors = createAuthorList(modelPub.getAuthors());
        List<DownloadableDataNodeImpl> dataFiles = createDataFiles(modelPub.getHierarchyRootNodes());

        gwtPub.setAuthors(authors);
        gwtPub.setDataFiles(dataFiles);
        // TODO Breaking intentionally.  We probably won't ever use this.
        List<DataFile> rolledUpArchives = new ArrayList<DataFile>();//createRolledUpArchives(modelPub.getRolledUpArchives());
        gwtPub.setRolledUpDataArchives(rolledUpArchives);
        DownloadableDataNode subjectDocument = createSubjectDocument(modelPub.getSubjectDocument());
        gwtPub.setSubjectDocument(subjectDocument);

        return gwtPub;
    }

    /**
     * Remarshal set of authors from database, into list of authors suitable for
     * exchange with GWT.
     *
     * @param authorSet Author objects.
     * @return list of strings.
     */
    private List<Author> createAuthorList(List<Author> authorSet) {
        ArrayList<Author> returnList = new ArrayList<Author>();

        //TODO consider abandoning the GWT-exchange models and using internal model objs.
        for (Author modelAuthor : authorSet) {
            returnList.add(modelAuthor);
        }

        return returnList;
    }

    /**
     * Recursive remarshal method, to convert internal db models into GWT-compatible
     * exchange models.
     *
     * @param hierarchyNodeSet from db.
     * @return for GWT.
     */
    private List<DownloadableDataNodeImpl> createDataFiles(List<HierarchyNode> hierarchyNodeSet) {
        ArrayList<DownloadableDataNodeImpl> returnList = new ArrayList<DownloadableDataNodeImpl>();

        // First treat the sub-hierarchy-nodes, which represent directories.
        for (HierarchyNode modelNode :
                hierarchyNodeSet) {

            DownloadableDataNodeImpl gwtParentLevel = convertHierarchyNodeFromModel(modelNode);

            // Children of the parent-level GWT-compatible node, include all the hierarchy nodes below
            // the next incoming hierarchy node, as well as any data files under the incoming node.
            //
            List<DownloadableDataNode> children = new ArrayList<DownloadableDataNode>();

            //       Handle all the hierarchy node children from the parent hierarchy node.
            children.addAll(createDataFiles(modelNode.getChildren()));

            //       Handle all the datafiles from the parent hierarchy node.
            List<DownloadableDataNode> dataFileChildren = new ArrayList<DownloadableDataNode>();
            for (Object o : modelNode.getDataFiles()) {

                DataFile modelDataFile =
                        (DataFile) o;

                DownloadableDataNodeImpl gwtDataFile = convertDataFileFromModel(modelDataFile);

                dataFileChildren.add(gwtDataFile);
            }

            children.addAll(dataFileChildren);
            gwtParentLevel.setChildren(children);

            returnList.add(gwtParentLevel);

        }

        return returnList;
    }

    /**
     * Givne a db-model of a hierarchy node, map that into a downloadable node (for GWT).  No children info
     * handled here--just node itself.
     *
     * @param modelNode whence info comes.
     * @return where it goes.
     */
    private DownloadableDataNodeImpl convertHierarchyNodeFromModel(HierarchyNode modelNode) {
        DownloadableDataNodeImpl gwtParentLevel = new DownloadableDataNodeImpl();
        gwtParentLevel.setText(modelNode.getName());
        String[] pAttributeNames = new String[]{DESCRIPTIVE_TEXT};
        String[] pAttributeValues = new String[]{modelNode.getDescription()};
        gwtParentLevel.setAttributes(pAttributeNames, pAttributeValues);
        gwtParentLevel.setText(modelNode.getName());

        return gwtParentLevel;
    }

    /**
     * Given a db-model data file, convert it to a downloadable node (for GWT).
     *
     * @param modelDataFile where info comes from
     * @return where info goes
     */
    private DownloadableDataNodeImpl convertDataFileFromModel(DataFile modelDataFile) {

        DownloadableDataNodeImpl gwtDataFile = new DownloadableDataNodeImpl();
        gwtDataFile.setInfoLocation(modelDataFile.getInfoLocation());
        gwtDataFile.setLocation(modelDataFile.getPath());

        String[] attributeNames = new String[]{DESCRIPTIVE_TEXT};
        String[] attributeValues = new String[]{modelDataFile.getDescription()};
        gwtDataFile.setAttributes(attributeNames, attributeValues);

        gwtDataFile.setSite(createSite(modelDataFile.getSamples()));
        gwtDataFile.setSize(modelDataFile.getSize());
        gwtDataFile.setMultifileArchive(modelDataFile.isMultifileArchive());
        gwtDataFile.setText(getFileName(modelDataFile.getPath()));

        return gwtDataFile;
    }

    /**
     * Given a List of db-model Samples, return a List of client-model Samples.
     */
    private List<org.janelia.it.jacs.web.gwt.common.client.model.metadata.Sample> remarshallSamples(Collection<Sample> modelSamples) {
        if (modelSamples == null)
            return null;
        List<org.janelia.it.jacs.web.gwt.common.client.model.metadata.Sample> clientSamples = new ArrayList<org.janelia.it.jacs.web.gwt.common.client.model.metadata.Sample>();
        for (Sample modelSample : modelSamples)
            clientSamples.add(remarshallSample(modelSample));
        return clientSamples;
    }

    private org.janelia.it.jacs.web.gwt.common.client.model.metadata.Sample remarshallSample(Sample modelSample) {
        Iterator siteIter = modelSample.getBioMaterials().iterator();
        // TODO: support multiple sites per sample
        Set<Site> sites = new HashSet<Site>();
        if (siteIter.hasNext()) {
            while (siteIter.hasNext())
                sites.add(getSiteFromModelSite((BioMaterial) siteIter.next()));
        }
        else {
            sites.add(getSiteFromModelSite(null));
        }

        // Create the Sample and add to the List
        return new org.janelia.it.jacs.web.gwt.common.client.model.metadata.Sample(
                modelSample.getSampleId(), modelSample.getSampleAcc(), modelSample.getSampleName(), modelSample.getTitle(),
                sites, getDataFilesFromSample(modelSample), modelSample.getFilterMin(), modelSample.getFilterMax());
    }

    private HashSet<DownloadableDataNode> getDataFilesFromSample(Sample modelSample) {
        HashSet<DownloadableDataNode> dataNodes = new HashSet<DownloadableDataNode>();
        Set dataFiles = modelSample.getDataFiles();
        if (dataFiles != null) {
            for (DataFile dataFile1 : modelSample.getDataFiles()) {
                // Tareq: Changed call to use dataFile.getPath() instead of dataFile.getInfoLocation() // which was null
                dataNodes.add(convertDataFileFromModel(dataFile1));
            }
        }

        return dataNodes;
    }

    /**
     * Retrieves the first site in the first sample
     */
    //TODO: support multiple samples/multiple sites
    private Site createSite(Set samples) {
        Site site = null;

        if (samples != null && samples.size() > 0) {
            Iterator iter = samples.iterator();
            Set sites = ((Sample) iter.next()).getBioMaterials();
            if (sites != null && sites.size() > 0) {
                Iterator siteIter = sites.iterator();
                site = getSiteFromModelSite((BioMaterial) siteIter.next());
            }
        }

        return site;
    }

    private Site getSiteFromModelSite(BioMaterial modelSite) {
        Site site = new Site();
        site.setSiteId(modelSite.getMaterialAcc());
        site.setBiomass(modelSite.getObservationAsString("biomass"));
//        site.setChlorophyllDensity(modelSite.getObservationAsString("chlorophyll density"));
        if (modelSite.getObservationAsString("chlorophyll density").length() > 0)
            site.setChlorophyllDensity(modelSite.getObservationAsString("chlorophyll density"));
        else if (modelSite.getObservationAsString("chlorophyll density/sample month").length() > 0)
            site.setChlorophyllDensity(modelSite.getObservationAsString("chlorophyll density/sample month"));
        else if (modelSite.getObservationAsString("chlorophyll density/annual").length() > 0)
            site.setChlorophyllDensity(modelSite.getObservationAsString("chlorophyll density/annual"));
        else site.setChlorophyllDensity("");
        if (modelSite.getCollectionSite() != null) {
            site.setCountry(((GeoPoint) modelSite.getCollectionSite()).getCountry());
            site.setGeographicLocation(modelSite.getCollectionSite().getRegion());
            site.setGeographicLocationDetail(modelSite.getCollectionSite().getComment());
            site.setHabitatType(modelSite.getCollectionSite().getSiteDescription());
            site.setLatitude(((GeoPoint) modelSite.getCollectionSite()).getLatitude());
            site.setLatitudeDouble(((GeoPoint) modelSite.getCollectionSite()).getLatitudeAsDouble());
            //site.setLeg(modelSite.getLeg());
            site.setLongitude(((GeoPoint) modelSite.getCollectionSite()).getLongitude());
            site.setLongitudeDouble(((GeoPoint) modelSite.getCollectionSite()).getLongitudeAsDouble());
            site.setSampleDepth(((GeoPoint) modelSite.getCollectionSite()).getDepth());
            site.setSampleLocation(modelSite.getCollectionSite().getLocation());
        }
        if (modelSite.getCollectionHost() != null) {
            site.setHostOrganism(modelSite.getCollectionHost().getOrganism());
            site.setHostDetails(modelSite.getCollectionHost().getHostDetails());
        }
        //site.setDataTimestamp(new Date(modelSite.getDataTimestamp().getTime()));
        site.setDissolvedInorganicCarbon(modelSite.getObservationAsString("dissolved inorganic carbon"));
        site.setDissolvedInorganicPhospate(modelSite.getObservationAsString("dissolved inorganic phosphate"));
        site.setDissolvedOrganicCarbon(modelSite.getObservationAsString("dissolved organic carbon"));
        site.setDissolvedOxygen(modelSite.getObservationAsString("dissolved oxygen"));
        site.setFluorescence(modelSite.getObservationAsString("fluorescence"));
        site.setNitrate_plus_nitrite(modelSite.getObservationAsString("nitrate+nitrite"));
        site.setNumberOfSamplesPooled(modelSite.getObservationAsString("number of samples pooled"));
        site.setNumberOfStationsSampled(modelSite.getObservationAsString("number of stations sampled"));
        site.setProject(modelSite.getProject());
        site.setSalinity(modelSite.getObservationAsString("salinity"));
        if (modelSite.getCollectionStartTime() != null)
            site.setStartTime(new Date(modelSite.getCollectionStartTime().getTime()));
        if (modelSite.getCollectionStopTime() != null)
            site.setStopTime(new Date(modelSite.getCollectionStopTime().getTime()));
        site.setTemperature(modelSite.getObservationAsString("temperature"));
        site.setTransmission(modelSite.getObservationAsString("transmission"));
        site.setVolume_filtered(modelSite.getObservationAsString("volume filtered"));
        site.setWaterDepth(modelSite.getObservationAsString("water depth"));

        return site;
    }

    /**
     * Remarshall a list of rolled-up (combined) archives.
     *
     * @param rolledUpArchiveSet from db
     * @return for gwt
     */
//    private List<DownloadableDataNode> createRolledUpArchives(List<org.janelia.it.jacs.model.download.DataFile> rolledUpArchiveSet) {
//        List<DownloadableDataNode> returnList = new ArrayList<DownloadableDataNode>();
//        for (org.janelia.it.jacs.model.download.DataFile modelDataFile: rolledUpArchiveSet) {
//            DownloadableDataNodeImpl gwtDataFile =
//                    new DownloadableDataNodeImpl();
//            gwtDataFile.setLocation(modelDataFile.getPath());
//            gwtDataFile.setInfoLocation(modelDataFile.getInfoLocation());
//            gwtDataFile.setMultifileArchive(false);
//            String[] attributeNames = new String[] { DESCRIPTIVE_TEXT };
//            String[] attributeValues = new String[] { modelDataFile.getDescription() };
//            gwtDataFile.setAttributes(attributeNames, attributeValues);
//            gwtDataFile.setText(getFileName(modelDataFile.getPath()));
//
//            returnList.add(gwtDataFile);
//        }
//        return returnList;
//    }

    /**
     * Just get the file name part from a path.
     *
     * @param path partially or fully-qualified, from some point.
     * @return just the name.
     */
    private String getFileName(String path) {
        int pos = path.lastIndexOf("/");
        if (pos == -1)
            pos = path.lastIndexOf("\\");
        if (pos == -1)
            return path;
        else
            return path.substring(pos + 1);
    }

    /**
     * Make a gwt-compatible node out of the subj-doc string.
     *
     * @param subjectDocument incoming string from db.
     * @return node for gwt.
     */
    private DownloadableDataNode createSubjectDocument(String subjectDocument) {
        DownloadableDataNodeImpl node = new DownloadableDataNodeImpl();
        node.setMultifileArchive(false);
        node.setText("PDF");

        String[] attributeNames = new String[]{PublicationHelper.DESCRIPTIVE_TEXT};
        String[] attributeValues = new String[]{"PDF"};

        node.setAttributes(attributeNames, attributeValues);
        node.setLocation(subjectDocument);
        return node;
    }
}
