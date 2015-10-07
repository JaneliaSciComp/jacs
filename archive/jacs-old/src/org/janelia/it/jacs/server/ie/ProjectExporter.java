
package org.janelia.it.jacs.server.ie;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.download.*;
import org.janelia.it.jacs.model.metadata.Sample;
import org.janelia.it.jacs.server.ie.jaxb.*;
import org.janelia.it.jacs.server.utils.HibernateSessionSource;
import org.janelia.it.jacs.shared.utils.FileUtil;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Set;

/**
 * This class is responsible for loading all the projects in the database and writing
 * them out to the directroy (xml_publication.iedir) specified in jacs.properties
 *
 * @author Tareq Nabeel
 */
public class ProjectExporter {

    private ObjectFactory jbObjectFactory = new ObjectFactory();
    JAXBContext jaxbContext;
    private HibernateSessionSource sessionSource = new HibernateSessionSource();
    private String projectBaseDir = SystemConfigurationProperties.getString("xml_project.ie.dir");
    private String publicationBaseDir = SystemConfigurationProperties.getString("xml_publication.ie.dir");

    public ProjectExporter() {
        try {
            jaxbContext = JAXBContext.newInstance("org.janelia.it.jacs.server.ie.jaxb");
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static final Logger logger = Logger.getLogger(FileUtil.class.getName());

    public static void main(String[] args) {
        ProjectExporter projectExporter = new ProjectExporter();
        projectExporter.exportProjects();
        projectExporter.exportPublications();
    }

    /**
     * This method is responsible for loading all the projects in the database and writing
     * them out to the filesystem
     */
    private void exportProjects() {
        Session session = sessionSource.getOrCreateSession();
        Transaction transaction = session.beginTransaction();
        try {
            File projectsDir = FileUtil.ensureDirExists(projectBaseDir);

            // Get the list of projects in the database to export
            List<Project> projects = session.createCriteria(Project.class).list();
            for (Project project : projects) {
                ProjectType jbProject = jbObjectFactory.createProjectType();
                setProjectAttributes(jbProject, project);
                writeProject(projectsDir, jbProject);
            }
            transaction.commit();
        }
        catch (HibernateException e) {
            handleException(transaction, e);
        }
        catch (JAXBException e) {
            handleException(transaction, e);
        }
        catch (IOException e) {
            handleException(transaction, e);
        }
    }

    /**
     * This method sets the jaxb project attributes based on the loaded project from the database
     *
     * @param jbProject
     * @param project
     */
    private void setProjectAttributes(ProjectType jbProject, Project project) {
        jbProject.setDescription(project.getDescription());
        jbProject.setEmail(project.getEmail());
        jbProject.setFundedBy(project.getFunded_by());
        jbProject.setInstitutionalAffiliation(project.getInstitutional_affiliation());
        jbProject.setName(project.getName());
        jbProject.setOrganization(project.getOrganization());
        jbProject.setPrincipalInvestigators(project.getPrincipal_investigators());
        //jbProject.setPublications(); // no need to do this ... publications are not written to project.xml
        // but are written individually for easier maintenance
        jbProject.setReleased(project.getReleased());
        jbProject.setSymbol(project.getSymbol());
        jbProject.setWebsiteUrl(project.getWebsite_url());
    }


    /**
     * This method writes out an individual project the project directory that's passed in
     *
     * @param projectsDir
     * @param jbProject
     * @throws IOException
     * @throws JAXBException
     */
    private void writeProject(File projectsDir, ProjectType jbProject) throws IOException, JAXBException {
        File projectFile = FileUtil.createNewFile(projectsDir.getAbsolutePath() + FileUtil.FILE_SEPARATOR + jbProject.getSymbol().replace(" ", "") + ".xml");
        writeJaxbObject(jbProject, projectFile);
    }

    /**
     * This method is responsible for creating all the jaxb objects assoicated to a publication and writing
     * out each publication to the filesystem
     */
    private void exportPublications() {
        Session session = sessionSource.getOrCreateSession();
        Transaction transaction = session.beginTransaction();
        try {
            File publicationDir = FileUtil.ensureDirExists(publicationBaseDir);
            List<Publication> publications = session.createCriteria(Publication.class).list();
            for (Publication publication : publications) {
                PublicationType jbPublication = jbObjectFactory.createPublicationType();
                // Set the state of the jaxb publication based on the hib-aware publication
                setPublicationAttributes(publication, jbPublication);
                setProjects(publication, jbPublication);
                setAuthors(publication, jbPublication);
                setDataFiles(publication, jbPublication);
                setHierarchyNodes(publication, jbPublication);

                // Write out the publication to the filesystem
                writePublication(publicationDir, publication, jbPublication);
            }
            transaction.commit();
        }
        catch (HibernateException e) {
            handleException(transaction, e);
        }
        catch (JAXBException e) {
            handleException(transaction, e);
        }
        catch (IOException e) {
            handleException(transaction, e);
        }
    }

    /**
     * This method is responsible for writing out a publication to the filesystem
     *
     * @param publicationDir
     * @param publication
     * @param jbPublication
     * @throws JAXBException
     * @throws IOException
     */
    private void writePublication(File publicationDir, Publication publication, PublicationType jbPublication) throws JAXBException, IOException {
        String pubFileName = createPublicationFileName(publication);
        File publicationFile = FileUtil.ensureFileExists(publicationDir.getAbsolutePath() + FileUtil.FILE_SEPARATOR + pubFileName);
        writeJaxbObject(jbPublication, publicationFile);
    }

    /**
     * Publication file name is now created based on primary author's last name, pubDate and publication
     * identifier
     *
     * @param publication
     * @return the created publication file name
     */
    private String createPublicationFileName(Publication publication) {
        //pub file name starts with primary author's last name
        StringBuffer fileName = null;
        List authors = publication.getAuthors();
        if (authors != null && authors.size() > 0) {
            String[] nameNameTokens = ((Author) authors.get(0)).getName().split("\\s");
            String firstAuthorLastName = nameNameTokens[nameNameTokens.length - 1];
            fileName = new StringBuffer(firstAuthorLastName);
        }
        else {
            fileName = new StringBuffer("anonymous");
        }
        fileName.append("-");
        //If pubDate exists append it
        if (publication.getPubDate() != null) {
            String pubDate = new SimpleDateFormat("yyyymmdd").format(publication.getPubDate());
            fileName.append(pubDate);
            fileName.append("-");
        }
        //Append the primary key of publication to make it unique
        fileName.append(publication.getObjectId());
        fileName.append(".xml");

        return fileName.toString();
    }

    /**
     * This method sets the jaxb publication attributes based on the loaded publication from the database
     *
     * @param publication
     * @param jbPublication
     * @throws JAXBException
     */
    private void setPublicationAttributes(Publication publication, PublicationType jbPublication) throws JAXBException {
        jbPublication.setAbstractOfPublication(publication.getAbstractOfPublication());
        jbPublication.setDescription(publication.getDescription());
        jbPublication.setObjectId(publication.getObjectId());
        jbPublication.setPublicationAccession(publication.getPublicationAccession());
        jbPublication.setSummary(publication.getSummary());
        jbPublication.setTitle(publication.getTitle());
        jbPublication.setPubDate(publication.getPubDate());
        setJournalEntry(publication, jbPublication);
        setSubjectDocument(publication, jbPublication);
        setSupplementalText(publication, jbPublication);
    }

    /**
     * This method sets the jaxb publication fulltext attribute based on the
     * db-loaded publication SubjectDocument attribute.
     * The name "FullText" is preferred in XML over "subjectdocument"
     *
     * @param publication
     * @param jbPublication
     * @throws JAXBException
     */
    private void setSubjectDocument(Publication publication, PublicationType jbPublication) throws JAXBException {
        LocationType jbLocation = jbObjectFactory.createLocationType();
        jbLocation.setLocation(publication.getSubjectDocument());
        jbPublication.setFullText(jbLocation);
    }

    /**
     * This method sets the jaxb publication supplementalText attribute based on the
     * db-loaded publication supplementalText attribute
     *
     * @param publication
     * @param jbPublication
     * @throws JAXBException
     */
    private void setSupplementalText(Publication publication, PublicationType jbPublication) throws JAXBException {
        LocationType jbLocation = jbObjectFactory.createLocationType();
        jbLocation.setLocation(publication.getSupplementalText());
        jbPublication.setSupplementalText(jbLocation);
    }

    /**
     * This method sets the jaxb publication journal based on the db-loaded publication journal entry.
     * We'd have to change this method when we decide to normalize the various journal attributes into a
     * separate table
     *
     * @param publication
     * @param jbPublication
     * @throws JAXBException
     */
    private void setJournalEntry(Publication publication, PublicationType jbPublication) throws JAXBException {
        if (publication.getJournalEntry() == null)
            return;

        // Set publication journal properties: <journal name="PLoS" volume="54" issue="3" page="30-33"/>
        String[] nameVolumeIssuePage = publication.getJournalEntry().split("/");
        JournalEntryType jbJournalEntry = jbObjectFactory.createJournalEntryType();
        jbJournalEntry.setName(nameVolumeIssuePage[0]);
        if (nameVolumeIssuePage.length > 1)
            jbJournalEntry.setVolume(nameVolumeIssuePage[1]);
        if (nameVolumeIssuePage.length > 2)
            jbJournalEntry.setIssue(nameVolumeIssuePage[2]);
        if (nameVolumeIssuePage.length > 3)
            jbJournalEntry.setPage(nameVolumeIssuePage[3]);
        jbPublication.setJournal(jbJournalEntry);
    }

    /**
     * This method sets the projects  for a publication.  Only the id of the project gets exported (so associations
     * can be changed) as the project contents get exported to project.xml file under projectsBaseDir.
     *
     * @param publication
     * @param jbPublication
     * @throws JAXBException
     */
    private void setProjects(Publication publication, PublicationType jbPublication) throws JAXBException {
        ProjectsType jbPubProjects = jbObjectFactory.createProjectsType();
        List jbProjects = jbPubProjects.getProject();
        for (Project project : (List<Project>) publication.getProjects()) {
            PubProjectType jbPubProject = jbObjectFactory.createPubProjectType();
            jbPubProject.setSymbol(project.getSymbol());
            jbProjects.add(jbPubProject);
        }
        jbPublication.setProjects(jbPubProjects);
    }

    /**
     * This method sets the jaxb publication authors based on the db-loaded publication authors.
     *
     * @param publication
     * @param jbPublication
     * @throws JAXBException
     */
    private void setAuthors(Publication publication, PublicationType jbPublication) throws JAXBException {
        AuthorsType jbAuthorsType = jbObjectFactory.createAuthorsType();
        List jbAuthors = jbAuthorsType.getAuthor();  // JAXB compiler should have named the method getAuthors()
        for (Author author : (List<Author>) publication.getAuthors()) {
            AuthorType jbAuthor = jbObjectFactory.createAuthorType();
            jbAuthor.setName(author.getName());
            jbAuthors.add(jbAuthor);
        }
        jbPublication.setAuthors(jbAuthorsType);
    }

    /**
     * This method sets the jaxb publication datafiles based on the db-loaded publication datafiles.
     *
     * @param publication
     * @param jbPublication
     * @throws JAXBException
     */
    private void setDataFiles(Publication publication, PublicationType jbPublication) throws JAXBException {
        // Set the data files for the jaxb publication
        DataFilesType jbDataFiles = createDataFiles(publication.getRolledUpArchives());
        //jbPublication.setDataFiles(jbDataFiles);
        jbPublication.getDataFile().addAll(jbDataFiles.getDataFile());
    }

    /**
     * This method is used to create a list of jaxb datafiles based on the db-loaded dataFileList.
     * It is used to create the datafiles for a publication as well as a hierarchy node
     *
     * @param dataFileList
     * @return
     * @throws JAXBException
     */
    private DataFilesType createDataFiles(List<DataFile> dataFileList) throws JAXBException {
        DataFilesType jbDataFiles = jbObjectFactory.createDataFilesType();
        for (DataFile dataFile : dataFileList) {
            DataFileType jbDataFile = jbObjectFactory.createDataFileType();
            jbDataFile.setDescription(dataFile.getDescription());
            jbDataFile.setInfoLocation(dataFile.getInfoLocation());
            jbDataFile.setMultifileArchive(dataFile.isMultifileArchive());
            jbDataFile.setObjectId(dataFile.getObjectId());
            jbDataFile.setPath(dataFile.getPath());
            jbDataFile.setSize(dataFile.getSize());
            addSamples(dataFile, jbDataFile);
            jbDataFiles.getDataFile().add(jbDataFile);
        }
        return jbDataFiles;
    }

    /**
     * This method creates a jaxb sample based on the db-loaded sample
     *
     * @param dataFile
     * @param jbDataFile
     * @throws JAXBException
     */
    private void addSamples(DataFile dataFile, DataFileType jbDataFile) throws JAXBException {
        Set<Sample> samples = dataFile.getSamples();
        for (Sample sample : samples) {
            SampleType jbSample = jbObjectFactory.createSampleType();
            jbSample.setId(sample.getSampleId());
            jbDataFile.getSample().add(jbSample);
        }
    }

    /**
     * This method is used to create and set the list of jaxb hierarchy nodes for a publication
     * based on the db-loaded publication hierarchy nodes.
     *
     * @param publication
     * @param jbPublication
     * @throws JAXBException
     */
    private void setHierarchyNodes(Publication publication, PublicationType jbPublication) throws JAXBException {
        // Create a list of jaxb hierarchy child nodes
        HierarchyNodesType jbPubHierarchyChildrenNodes = jbObjectFactory.createHierarchyNodesType();

        // The publication hierarchy nodes can in turn contain other hierarchy nodes.
        // We need to add them recursively
        addHierarchyChildNodes(publication.getHierarchyRootNodes(), jbPubHierarchyChildrenNodes);

        // Add the list of jaxb hierarchy nodes to the parent jaxb hierarchy node
        //jbPublication.setHierarchyNodes(jbPubHierarchyChildrenNodes);
        jbPublication.getHierarchyNode().addAll(jbPubHierarchyChildrenNodes.getHierarchyNode());
    }


    /**
     * This method is responsible for creating and setting the jaxb hierarchy nodes based on the db-loaded
     * hierarchy nodes.  Since hierarchy node can contain other hierarchy nodes, it calls itself recursively
     *
     * @param hierarchyNodeList
     * @param jbHierarchyChildrenNodes
     * @throws JAXBException
     */
    private void addHierarchyChildNodes(List<HierarchyNode> hierarchyNodeList, HierarchyNodesType jbHierarchyChildrenNodes) throws JAXBException {
        for (HierarchyNode hierarchyChildNode : hierarchyNodeList) {
            // Create a jaxb Hierarchy node based on the hbm hierarchy node supplied to this method
            HierarchyNodeType jbHierarchyChildNode = createJBHierarchyChildNode(hierarchyChildNode);

            // Add the jaxb Hierarchy node to the list of jaxb hierarchy nodes
            jbHierarchyChildrenNodes.getHierarchyNode().add(jbHierarchyChildNode);

            // Create the tree recursively
            setHierarchyNodes(hierarchyChildNode.getChildren(), jbHierarchyChildNode);
        }
    }

    /**
     * This method is responsible for setting the jaxb-parent-hierarchy-node's hierarchy children nodes
     *
     * @param hierarchyNodeList
     * @param jbHierarchyParentNode
     * @throws JAXBException
     */
    private void setHierarchyNodes(List hierarchyNodeList, HierarchyNodeType jbHierarchyParentNode) throws JAXBException {
        // Create a list of jaxb hierarchy child nodes
        HierarchyNodesType jbHierarchyChildrenNodes = jbObjectFactory.createHierarchyNodesType();

        addHierarchyChildNodes(hierarchyNodeList, jbHierarchyChildrenNodes);

        // Add the list of jaxb hierarchy nodes to the parent jaxb hierarchy node
        //jbHierarchyParentNode.setHierarchyNodes(jbHierarchyChildrenNodes);
        jbHierarchyParentNode.getHierarchyNode().addAll(jbHierarchyChildrenNodes.getHierarchyNode());
    }


    /**
     * This method creates a jaxb Hierarchy Node object and sets it's state based on the state
     * of the supplied hibernate Hierarchy Node object
     *
     * @param hierarchyNode
     * @return
     * @throws JAXBException
     */
    private HierarchyNodeType createJBHierarchyChildNode(HierarchyNode hierarchyNode) throws JAXBException {
        // Create the jaxb hierarchy node and set it's attributes
        HierarchyNodeType jbHierarchyNode = jbObjectFactory.createHierarchyNodeType();
        jbHierarchyNode.setName(hierarchyNode.getName());
        jbHierarchyNode.setObjectId(hierarchyNode.getObjectId());
        //jbHierarchyNode.setDescription(hierarchyNode.getDescription());

        // Set the data files for the newly created jaxb hierarchy node
        DataFilesType jbDataFiles = createDataFiles(hierarchyNode.getDataFiles());
        //jbHierarchyNode.setDataFiles(jbDataFiles);
        jbHierarchyNode.getDataFile().addAll(jbDataFiles.getDataFile());

        return jbHierarchyNode;
    }

    /**
     * Rolls back the transaction and logs the exception
     *
     * @param transaction
     * @param e
     */
    private void handleException(Transaction transaction, Exception e) {
        logger.error(getClass().getName(), e);
        e.printStackTrace();
        transaction.rollback();
    }

    private void writeJaxbObject(Object jbObject, File destination) throws JAXBException, IOException {
        Marshaller jbMarshaller = createMarshaller();
        jbMarshaller.marshal(jbObject, new FileOutputStream(destination));
    }

    private Marshaller createMarshaller() throws JAXBException {
        Marshaller jbMarshaller = jaxbContext.createMarshaller();
        jbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        jbMarshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");


        return jbMarshaller;
    }

}




