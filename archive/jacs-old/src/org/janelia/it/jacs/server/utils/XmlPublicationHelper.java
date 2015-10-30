
package org.janelia.it.jacs.server.utils;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.download.Project;
import org.janelia.it.jacs.model.download.Publication;

import java.io.File;
import java.util.*;
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
public class XmlPublicationHelper {

    public static final String DESCRIPTIVE_TEXT = "Description";
    public static final String COMING_SOON = "(Coming Soon)";

    private static final String FILE_SEPARATOR = "/"; // ASSUME: load against UNIX environment. System.getProperty("file.separator");
    private static final String DESCRIPTION_FILE = "description.txt";
    private static final String PLACEHOLDER_FILE = "placeholder.txt";

    private static final Pattern NAME_VALUE_SEPARATION = Pattern.compile("[\\[\\]]");
    private static final Pattern INSTANCE_SEPARATION = Pattern.compile(";");
    private static final Pattern PROJECT_NAME_SEPARATION = Pattern.compile("=");

    private Map _projectNameVsLocation;
    private Map _projectNameVsProject;
    private Map _projectNameVsContents;
    private Map _projectNameVsDescription;
    private String _projectBaseLocation;
    private List _newFiles;

    private boolean _futureProjectsHaveBeenDesignated = false;

    // Setup default-configured log.
    private Logger log = Logger.getLogger(XmlPublicationHelper.class);

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
        if (baseLocation.endsWith(XmlPublicationHelper.FILE_SEPARATOR))
            _projectBaseLocation = baseLocation;
        else
            _projectBaseLocation = baseLocation + XmlPublicationHelper.FILE_SEPARATOR;
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

    public Map getAllProjects() {
        readProjectData();
        return _projectNameVsDescription;
    }

    public Project getProject(String projectName) {
        // Note that project data cannot be read one project at a time, until
        // this issue of what a project is, and how New Files relate to it, is
        // resolved.
        readProjectData();
        return (Project) _projectNameVsProject.get(projectName);
    }

    public List getNewFiles() {
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

    //--------------------------------HELPERS
    /**
     * Breaks up names and locations from the raw setting.
     *
     * @param rawString Format: [name;name;name;...][value;value;value;...]
     */
    private Map parseNamesAndValues(String rawString, boolean forPath) {
        Map returnMap = new HashMap();
        try {

            //  First, separate the names from the values.
            //
            String[] namesAndValues = XmlPublicationHelper.NAME_VALUE_SEPARATION.split(rawString);
            String namesStr = namesAndValues[1];
            String valuesStr = namesAndValues[3];

            String[] names = XmlPublicationHelper.INSTANCE_SEPARATION.split(namesStr);
            String[] values = XmlPublicationHelper.INSTANCE_SEPARATION.split(valuesStr);

            if (names.length != values.length) {
                String message = "Number of names must equal number of values.  Not satisfied by: "
                        + rawString;

                log.error(message);

            }
            else {
                for (int i = 0; i < names.length; i++) {
                    if ((!forPath) || values[i].endsWith(XmlPublicationHelper.FILE_SEPARATOR)) {
                        returnMap.put(names[i], values[i]);
                    }
                    else {
                        returnMap.put(names[i], values[i] + XmlPublicationHelper.FILE_SEPARATOR);
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
    private Map parseProjectContents(String rawString) {
        Map returnMap = new HashMap();
        try {

            //  First, separate the names from the values.
            //
            String[] projectContents = XmlPublicationHelper.NAME_VALUE_SEPARATION.split(rawString);
            for (int i = 1; i < projectContents.length; i += 2) {
                String nextProjectsContents = projectContents[i];
                String[] projectNameVsContents = XmlPublicationHelper.PROJECT_NAME_SEPARATION.split(nextProjectsContents);
                String projectName = projectNameVsContents[0];
                String nextContents = projectNameVsContents[1];
                String[] publications = XmlPublicationHelper.INSTANCE_SEPARATION.split(nextContents);

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
        if (_projectNameVsProject == null) {
            try {
                _projectNameVsProject = new HashMap();
                for (Iterator it = _projectNameVsContents.keySet().iterator(); it.hasNext();) {
                    // Setup for read of next project's XML data.
                    String nextName = (String) it.next();
                    //String location = (String)_projectNameVsLocation.get(nextName);

                    //location = buildupLocation(location);

                    // Given location, make the project, from the XML 'model', and add that
                    // to the mapping of proejct names vs the actual projects.
                    _projectNameVsProject.put(nextName, readProjectData(nextName));
                }
            }
            catch (Exception ex) {
                log.error("Failed to read project data: " + ex.getMessage());
                log.error("See Below", ex);
            }
        }

        // Call this to establish which projects are place holders.
        createFutureProjects();

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
        Project returnProject = new Project();
        returnProject.setSymbol(projectName);
        List publications = new ArrayList();

        String[] members = (String[]) _projectNameVsContents.get(projectName);

        JAXBPublicationSource source = new JAXBPublicationSource();
        for (int i = 0; i < members.length; i++) {
            String nextFileLocation = buildupLocation(members[i]);
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
        returnProject.setDescription((String) _projectNameVsDescription.get(projectName));

        return returnProject;
    }

    /**
     * Helpers to deal with fully-parsed external injections of values.
     */
    private void setProjectLocationMapping(Map projectNameVsLocation) {
        _projectNameVsLocation = projectNameVsLocation;
        log.info("Project name vs location set with this many values: " + _projectNameVsLocation.size());
    }

    private void setProjectContentMapping(Map projectContents) {
        _projectNameVsContents = projectContents;
        log.info("Project name vs contents set with this many values: " + _projectNameVsContents.size());
    }

    private void setProjectDescriptionMapping(Map projectDescriptions) {
        _projectNameVsDescription = projectDescriptions;
        log.info("Project name vs description set with this many values: " + _projectNameVsDescription.size());
    }

    /**
     * This is to be called after dependencies of both the description and contents mappings have been
     * set (or injected).
     */
    private void createFutureProjects() {
        if (_futureProjectsHaveBeenDesignated)
            return;

        for (Iterator it = _projectNameVsDescription.keySet().iterator(); it.hasNext();) {
            String nextKey = (String) it.next();
            boolean isFuture = _projectNameVsContents.get(nextKey) == null;
            if (isFuture) {
                String description = (String) _projectNameVsDescription.get(nextKey);

                //  NOTE: future projects have no publications currently.
                Project futureProject = new Project();
                futureProject.setDescription(description);
                futureProject.setPublications(new ArrayList());
                futureProject.setSymbol(nextKey);
                _projectNameVsProject.put(nextKey, futureProject);
            }
        }
        _futureProjectsHaveBeenDesignated = true;

    }
}
