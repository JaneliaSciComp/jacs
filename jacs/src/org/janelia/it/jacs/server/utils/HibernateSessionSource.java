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
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.net.URL;

/**
 * Created by IntelliJ IDEA.
 * User: Lfoster
 * Date: Oct 26, 2006
 * Time: 1:55:15 PM
 * <p/>
 * For the Download metadata, convert projects from XML to relational storage.
 */
public class HibernateSessionSource {

    private static final String RESOURCE_LOCATION = "hibernate.cfg.xml";
    private static Logger _log = Logger.getLogger(HibernateSessionSource.class);

    private Session _session;
    private Configuration _cfg;
    private SessionFactory _factory;

    //------------------------------------------------------PUBLIC INTERFACE METHODS
    /**
     * Wrapper around cached session object, to allow it to be created prior to first use.  The session will be
     * cached in the session source object, so it should not be cached outside!
     *
     * @return existing cached session or newly created one.
     */
    public synchronized Session getOrCreateSession() {
        if (_session == null || !_session.isOpen()) {
            _session = createSession();
        }
        return _session;
    }

    /**
     * Dispose of any existing session object.
     */
    public synchronized void closeSession() {
        if (_session != null) {
            if (_session.isOpen())
                _session.flush();
            if (_session.isOpen())
                _session.close();
            _session = null;

            _log.info("Closed session");
        }
    }

    //------------------------------------------------------HELPER METHODS
    /**
     * Obtain session to use in populating the database.
     *
     * @return Hibernate Session that can contact our database.
     */
    private Session createSession() {
        SessionFactory factory = getOrCreateSessionFactory();
        Session returnSession = factory.getCurrentSession();
        _log.info("Got session from factory of type: " + factory.getClass().getName());
        return returnSession;
    }

    public Configuration getOrCreateConfiguration() {
        if (_cfg == null) {
            Configuration cfg = new Configuration();
            URL hibernateConfigURL = this.getClass().getClassLoader().getResource(RESOURCE_LOCATION);
            String configResource = hibernateConfigURL.toString();
            cfg = cfg.configure(hibernateConfigURL);
            _cfg = cfg;

            _log.info("Created a configuration from " + configResource);
        }
        return _cfg;
    }

    public SessionFactory getOrCreateSessionFactory() {
        Configuration cfg = getOrCreateConfiguration();
        if (_factory == null) {
            _factory = cfg.buildSessionFactory();
        }
        return _factory;
    }

}
