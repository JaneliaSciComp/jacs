/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

package org.janelia.it.jacs.server.utils;

import org.apache.log4j.Logger;
import org.hibernate.*;
import org.janelia.it.jacs.model.download.Author;
import org.janelia.it.jacs.model.download.Project;
import org.janelia.it.jacs.model.download.Publication;

import java.io.InputStream;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: Lfoster
 * Date: Oct 26, 2006
 * Time: 1:55:15 PM
 * <p/>
 * For the Download metadata, convert projects from XML to relational storage.
 */
public class DownloadXmlToDbConvertor {

    private static final String RELOAD_DATABASE = "reload";
    private static final String UPDATE_PROJECT = "update";
    private static Logger logger = Logger.getLogger(DownloadXmlToDbConvertor.class);

    private HibernateSessionSource _sessionSource = new HibernateSessionSource();
    private String _projectBaseLocation;
    private String _projectContents;
    private String _projectDescriptions;
    private String _projectLocationMap;
    private boolean _reload;           //Raw loads remove all old data from database.

    /**
     * Driving code: this is where the launch is done, to convert all the XML files in
     * the old-style props-and-XML area, to a database-schema home.
     *
     * @param args unused.
     */
    public static void main(String[] args) {
        try {
            // Reloads cause all old data in this section (download) of the schema to be
            // dropped, and new tables to be created.  If NOT a reload, then the intent
            // is the add something to the existing project.
            if (args.length > 0 && args[0].equalsIgnoreCase(RELOAD_DATABASE)) {
                new DownloadXmlToDbConvertor(true).convert();
            }
            else if (args.length > 1 && args[0].equalsIgnoreCase(UPDATE_PROJECT)) {
                String symbol = args[1];
                new DownloadXmlToDbConvertor(false).convert(symbol);
            }
            else {
                throw new IllegalArgumentException("Usage: java DownloadXmlToDbConvertor <reload|update> ['project symbol']"
                        + "\n     If update, then the project symbol should be given, and that project will be changed");
            }
        }
        catch (Exception ex) {
            logger.error(ex.getMessage());
            logger.error(ex);
            ex.printStackTrace();
        }
    }

    public DownloadXmlToDbConvertor(boolean reload) throws Exception {
        _reload = reload;

        // Discover all the background information.
        Properties props = new Properties();
        ClassLoader loader = this.getClass().getClassLoader();
        InputStream is = loader.getResourceAsStream("jacs.properties");
        if (is != null)
            props.load(is);
        /*
        * See also: jacsweb-servlet.xml
        */
        _projectBaseLocation = props.getProperty("download_delivery_controller.base_file_location");
        _projectContents = props.getProperty("xml_publication_helper.project_contents");
        _projectDescriptions = props.getProperty("xml_publication_helper.project_descriptions");
        _projectLocationMap = props.getProperty("xml_publication_helper.project_location_map");

    }

    public void convert() throws Exception {
        if (_reload)
            cleanupDatabase();

        XmlPublicationHelper helper = getHelper();

        // Make major-level calls to the helper.
        Map projectsMap = helper.getAllProjects();
        for (Iterator it = projectsMap.keySet().iterator(); it.hasNext();) {
            // Obtain the project from old sources.
            String nextProjectSymbol = it.next().toString();
            Project nextProject = helper.getProject(nextProjectSymbol);
            serializeProject(nextProjectSymbol, nextProject);
        }

        _sessionSource.closeSession();
    }

    /**
     * Make a change (create) a single project, with all associated publications.
     *
     * @param projectSymbol short name.
     * @throws Exception from any called methods.
     */
    public void convert(String projectSymbol) throws Exception {
        // The scenario covered here, is one in which new publications are to be added
        // to a project.  Otherwise, no change will be made.
        XmlPublicationHelper helper = getHelper();
        Project newProject = helper.getProject(projectSymbol);

        // Obtain the project from old sources.  Preserve its description.
        Session readSession = _sessionSource.getOrCreateSession();
        Transaction transaction = readSession.beginTransaction();
        Project dbLoadedProject = (Project) readSession.load(Project.class, projectSymbol);

        List publications = dbLoadedProject.getPublications();
        publications.addAll(newProject.getPublications());

        mergeInAuthorsFrom(dbLoadedProject, readSession);
        readSession.saveOrUpdate(dbLoadedProject);

        transaction.commit();
        if (readSession != null && readSession.isOpen()) {
            readSession.flush();
        }
        if (readSession != null && readSession.isOpen()) {
            readSession.close();
        }
        _sessionSource.closeSession();
    }

    /**
     * Populate a helper for subsequent use in conversions.
     *
     * @return populated helper.
     */
    private XmlPublicationHelper getHelper() {
        // Setup the class to grab and convert everything.
        XmlPublicationHelper helper = new XmlPublicationHelper();
        helper.setProjectBaseLocation(_projectBaseLocation);
        helper.setProjectContents(_projectContents);
        helper.setProjectDescriptions(_projectDescriptions);
        helper.setProjectLocationMap(_projectLocationMap);

        return helper;
    }

    /**
     * Wrapper around serializing method, to deal with errors.
     *
     * @param projectSymbol known as
     * @param project       fully loaded object.
     */
    private void serializeProject(String projectSymbol, Project project) {
        // Pump project into new location.
        try {
            if (project == null)
                logger.error("No project found for (possibly future project) symbol: " + projectSymbol);
            else {
                logger.info("Writing project: " + project.getDescription());
                serializeProject(project);
            }
        }
        catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }

    }

    /**
     * Just take an existing transient object and persist it to the database.
     *
     * @param project created elsewhere
     * @return same project, if it got persisted.
     * @throws org.springframework.dao.DataAccessException
     *          from method calls.
     */
    public Project serializeProject(Project project) throws Exception {
        try {
            Session session = _sessionSource.getOrCreateSession();
            Transaction transaction = session.beginTransaction();
            try {
                mergeInAuthorsFrom(project, session);
                session.saveOrUpdate(project);
                transaction.commit();
            }
            catch (HibernateException he) {
                // Ignore for now: must get all projects loaded into db,
                // and cannot drop old duplicates.
                he.printStackTrace();
            }
            finally {
                if (session != null && session.isOpen())
                    session.close();
            }
            return project;

        }
        catch (HibernateException e) {
            throw e;
        }
    }

    /**
     * remove old contents from database.  Some content will be save to leave along, owing to how its
     * keys are constructed.  Other content has sequence-generated keys, and hence cannot be left in.
     */
    private void cleanupDatabase() {
        if (!_reload)
            return;

        Session session = null;
        try {
            session = _sessionSource.getOrCreateSession();
            Transaction transaction = session.beginTransaction();

            // Knockout contents of all link tables.
            SQLQuery sqlQuery;
            sqlQuery = session.createSQLQuery("delete from camera.project_publication_link");
            sqlQuery.executeUpdate();
            sqlQuery = session.createSQLQuery("delete from camera.publication_author_link");
            sqlQuery.executeUpdate();
            sqlQuery = session.createSQLQuery("delete from camera.publication_hierarchy_node_link");
            sqlQuery.executeUpdate();
            sqlQuery = session.createSQLQuery("delete from camera.hierarchy_node_data_file_link");
            sqlQuery.executeUpdate();
            sqlQuery = session.createSQLQuery("delete from camera.hierarchy_node_to_children_link");
            sqlQuery.executeUpdate();
            sqlQuery = session.createSQLQuery("delete from camera.data_file_sample_link");
            sqlQuery.executeUpdate();

            // Knockout contents of vulnerable data tables.
            Query query;
            query = session.createQuery("delete org.janelia.it.jacs.model.download.Publication");
            logger.info("Results executing Publication update: " + query.executeUpdate());

            query = session.createQuery("delete org.janelia.it.jacs.model.download.HierarchyNode");
            logger.info("Results executing HierarchyNode update: " + query.executeUpdate());

            query = session.createQuery("delete org.janelia.it.jacs.model.download.DataFile");
            logger.info("Results executing DataFile update: " + query.executeUpdate());

            transaction.commit();
        }
        catch (Exception ex) {
            ex.printStackTrace();
            logger.error(ex);
        }
        finally {
            if (session != null && session.isOpen()) {
                session.flush();
                session.close();
            }
        }
    }

    /**
     * Traverse all authors in all publications in the given project: ensure they have been
     * merged into the session, to prevent problems with duplications.
     *
     * @param project to be serialized soon
     * @param session to get the authors straightened out.
     */
    private void mergeInAuthorsFrom(Project project, Session session) {
        try {
            Map authorNameVsAuthor = new HashMap();
            for (Publication pub : (List<Publication>) project.getPublications()) {
                List oldList = new ArrayList(pub.getAuthors());
                for (Author author : (List<Author>) oldList) {
                    // Theory: can set refs in author to same instance.
                    if (authorNameVsAuthor.containsKey(author.getName())) {
                        String name = author.getName();
                        int inx = pub.getAuthors().indexOf(author);
                        pub.getAuthors().remove(author);
                        pub.getAuthors().add(inx, (Author) authorNameVsAuthor.get(name));  // Same object.
                    }
                    else {
                        authorNameVsAuthor.put(author.getName(), author);
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
            logger.error(ex);
        }
    }

}
