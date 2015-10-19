
package org.janelia.it.jacs.web.gwt.download.server;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.web.gwt.common.client.model.metadata.Sample;
import org.janelia.it.jacs.web.gwt.download.client.model.Project;
import org.janelia.it.jacs.web.gwt.download.client.model.ProjectImpl;
import org.janelia.it.jacs.web.gwt.download.client.model.Publication;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: Lfoster
 * Date: Sep 29, 2006
 * Time: 9:26:24 AM
 * <p/>
 * Help get information about publications, by parsing an XML file.  Since this will reside
 * on the server, and the amount of information will generally not be that great, will try
 * and keep it all in memory.
 */
public class XmlPublicationHelper implements PublicationHelper {

    private static final String FILE_SEPARATOR = System.getProperty("file.separator");
    private static final Pattern NAME_VALUE_SEPARATION = Pattern.compile("[\\[\\]]");
    private static final Pattern INSTANCE_SEPARATION = Pattern.compile(";");
    private static final Pattern PROJECT_NAME_SEPARATION = Pattern.compile("=");

    private Map<String, String> _projectNameVsLocation;
    private Map<String, Project> _projectSymbolVsProject;
    private Map<String, String[]> _projectNameVsContents;
    private Map<String, String> _projectNameVsDescription;
    private String _projectBaseLocation;
    private List<String> _newFiles;

    private boolean _placeholdersHaveBeenMarked = false;

    // Setup default-configured log.
    private Logger log = Logger.getLogger(PublicationHelper.class);

    /**
     * Default constructor to aid in logging. Otherwise, implicit constructor okay.
     */
    public XmlPublicationHelper() {
        log.info("Publication Helper is " + this.getClass().getName());
    }

    /**
     * The base locations of all projects are the same.  Otherwise, the downloads cannot properly work.
     *
     * @param baseLocation where projects all reside. After setting, must end with separator.
     */
    public void setProjectBaseLocation(String baseLocation) {
        if (baseLocation.endsWith(FILE_SEPARATOR))
            _projectBaseLocation = baseLocation;
        else
            _projectBaseLocation = baseLocation + FILE_SEPARATOR;
    }

    /**
     * Location needs to be made available.  Here it is a "dependency" to be set.
     *
     * @param parsableNamesAndLocations where is/are the XML? Format: [name;name;name;...][path;path;path;...]
     */
    public void setProjectLocationMap(String parsableNamesAndLocations) {
        setProjectLocationMapping(parseNamesAndValues(parsableNamesAndLocations, true));
        log.info("Project name vs location set with this many values: " + _projectNameVsLocation.size());
    }

    public void setProjectContents(String parsableProjectContents) {
        setProjectContentMapping(this.parseProjectContents(parsableProjectContents));
    }

    public void setProjectDescriptions(String parsableDescriptions) {
        setProjectDescriptionMapping(parseNamesAndValues(parsableDescriptions, false));
        log.info("Project name vs description set with this many values: " + _projectNameVsDescription.size());
    }

    //--------------------------------IMPLEMENTS PublicationHelper

    public Map<String, Project> getSymbolToProjectMapping() {
        markPlaceholders();
        return _projectSymbolVsProject;
    }

    public Map<String, Publication> getAccessionToPublicationMapping() {
        System.out.print("");
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void saveOrUpdateProject(ProjectImpl project) {
        //To change body of implemented methods use File | Settings | File Templates.
        System.out.print("");
    }

    public Project getProjectByName(String projectName) {
        // Note that project data cannot be read one project at a time, until
        // this issue of what a project is, and how New Files relate to it, is
        // resolved.
        readProjectData();
        Project result = null;
        for (Object o : _projectSymbolVsProject.values()) {
            Project project = (Project) o;
            if (projectName.equals(project.getProjectName())) {
                result = project;
                break;
            }
        }
        return result;
    }

    public Project getProjectBySymbol(String projectSymbol) {
        // Note that project data cannot be read one project at a time, until
        // this issue of what a project is, and how New Files relate to it, is
        // resolved.
        readProjectData();
        return _projectSymbolVsProject.get(projectSymbol);
    }

    public Publication getPublicationByAccession(String publicationAccession) {
        throw new UnsupportedOperationException("Method not implemented");
    }

    public List<String> getNewFiles() {
        readProjectData();
        return _newFiles;
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
            return finder.exists();
        }
        catch (Exception ex) {
            log.error(ex.getMessage());  // Let the developers know...
            log.error(ex);
            return Boolean.FALSE;
        }

    }

    public List<Sample> getProjectSamples(String projectName) {
        throw new UnsupportedOperationException("Method not implemented");
    }

    /**
     * Not implemented.
     */
    public Map<String, List<Sample>> getProjectSamplesByProject() {
        return null;
    }

    public List getDownloadableFilesBySampleAcc(String sampleAcc) {
        throw new UnsupportedOperationException("Method not implemented");
    }

    //--------------------------------HELPERS
    /**
     * Breaks up names and locations from the raw setting.
     *
     * @param rawString Format: [name;name;name;...][value;value;value;...]
     */
    private Map<String, String> parseNamesAndValues(String rawString, boolean forPath) {
        Map<String, String> returnMap = new HashMap<String, String>();
        try {

            //  First, separate the names from the values.
            //
            String[] namesAndValues = NAME_VALUE_SEPARATION.split(rawString);
            String namesStr = namesAndValues[1];
            String valuesStr = namesAndValues[3];

            String[] names = INSTANCE_SEPARATION.split(namesStr);
            String[] values = INSTANCE_SEPARATION.split(valuesStr);

            if (names.length != values.length) {
                String message = "Number of names must equal number of values.  Not satisfied by: "
                        + rawString;

                log.error(message);

            }
            else {
                for (int i = 0; i < names.length; i++) {
                    if ((!forPath) || values[i].endsWith(FILE_SEPARATOR)) {
                        returnMap.put(names[i], values[i]);
                    }
                    else {
                        returnMap.put(names[i], values[i] + FILE_SEPARATOR);
                    }
                }

            }
        }
        catch (Exception ex) {
            log.error("Problem with parsing of (project name and location) " + rawString);
            log.error(ex);
        }

        return returnMap;
    }

    /**
     * Breaks up names and locations from the raw setting.
     *
     * @param rawString Format: [pnameA=location1;location2;...][pnameB=locationX;locationY;...][...
     */
    private Map<String, String[]> parseProjectContents(String rawString) {
        Map<String, String[]> returnMap = new HashMap<String, String[]>();
        try {

            //  First, separate the names from the values.
            //
            String[] projectContents = NAME_VALUE_SEPARATION.split(rawString);
            for (int i = 1; i < projectContents.length; i += 2) {
                String nextProjectsContents = projectContents[i];
                String[] projectNameVsContents = PROJECT_NAME_SEPARATION.split(nextProjectsContents);
                String projectName = projectNameVsContents[0];
                String nextContents = projectNameVsContents[1];
                String[] publications = INSTANCE_SEPARATION.split(nextContents);

                // Should end up with a project name vs its array of files, in a map.
                returnMap.put(projectName, publications);
            }

        }
        catch (Exception ex) {
            log.error("Problem with parsing of (project contents) " + rawString);
            log.error(ex);
        }

        return returnMap;
    }

    /**
     * High-level reader helper, to pull in all the projects listed in the
     * name vs locational mapping.
     */
    private void readProjectData() {
        if (_projectSymbolVsProject == null || _newFiles == null || _newFiles.size() == 0) {
            try {
                _projectSymbolVsProject = new HashMap<String, Project>();
                for (Object o : _projectNameVsContents.keySet()) {
                    // Setup for read of next project's XML data.
                    String nextName = (String) o;
                    //String location = (String)_projectNameVsLocation.get(nextName);

                    //location = buildupLocation(location);

                    // Given location, make the project, from the XML 'model', and add that
                    // to the mapping of proejct names vs the actual projects.
                    Project project = readProjectData(nextName);
                    _projectSymbolVsProject.put(project.getProjectSymbol(), project);
                }
            }
            catch (Exception ex) {
                log.error("Failed to read project data: " + ex.getMessage());
                log.error("See Below", ex);
            }
        }

        // Call this to establish which projects are place holders.
        markPlaceholders();

    }

    private String buildupLocation(String location) {

        // Given location, find absolute location of project.
        if (_projectBaseLocation != null) {
            location = _projectBaseLocation + location;
        }
        return location;
    }

    /**
     * Low-level helper, to build the project object given by injected path locations.
     *
     * @param projectName of project.
     * @return a fully-instantiated project.
     * @throws Exception thrown by called code.
     */
    private Project readProjectData(String projectName) throws Exception {

        //  Given we have the record, convert that into a suitable Project.
        ProjectImpl returnProject = new ProjectImpl();
        returnProject.setProjectName(projectName);
        List<Publication> publications = new ArrayList<Publication>();

        String[] members = _projectNameVsContents.get(projectName);

        PublicationSource source = new JAXBPublicationSource();
        for (String member : members) {
            String nextFileLocation = buildupLocation(member);
            log.info("Reading file in project " + projectName + ", " + nextFileLocation);
            File nextFile = new File(nextFileLocation);

            // Each member is a publication.
            if (nextFile.isFile()) {
                try {
                    Publication pub = source.readPublication(nextFile.getAbsolutePath());
                    publications.add(pub);
                }
                catch (Exception ex) {
                    log.error(ex.getMessage());
                    log.error(ex);
                }
            }
        }

        returnProject.setPublications(publications);

        return returnProject;
    }

    /**
     * Helpers to deal with fully-parsed external injections of values.
     */
    private void setProjectLocationMapping(Map<String, String> projectNameVsLocation) {
        _projectNameVsLocation = projectNameVsLocation;
        log.info("Project name vs location set with this many values: " + _projectNameVsLocation.size());
    }

    private void setProjectContentMapping(Map<String, String[]> projectContents) {
        _projectNameVsContents = projectContents;
        log.info("Project name vs contents set with this many values: " + _projectNameVsContents.size());
    }

    private void setProjectDescriptionMapping(Map<String, String> projectDescriptions) {
        _projectNameVsDescription = projectDescriptions;
        log.info("Project name vs description set with this many values: " + _projectNameVsDescription.size());
    }

    /**
     * This is to be called after dependencies of both the description and contents mappings have been
     * set (or injected).
     */
    private void markPlaceholders() {
        if (_placeholdersHaveBeenMarked)
            return;

        for (Object o : _projectNameVsDescription.keySet()) {
            String nextKey = (String) o;
            boolean isPlaceholder = _projectNameVsContents.get(nextKey) == null;
            if (isPlaceholder) {
                // Adjust the description to reflect its placeholder status.
                String description = _projectNameVsDescription.get(nextKey) + COMING_SOON;
                _projectNameVsDescription.remove(nextKey);
                _projectNameVsDescription.put(nextKey, description);
            }
        }
        _placeholdersHaveBeenMarked = true;

    }
}
