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

package org.janelia.it.jacs.shared.hibernate;

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
    private static Logger logger = Logger.getLogger(HibernateSessionSource.class);

    private static Configuration _cfg;
    private static SessionFactory _factory;

    //------------------------------------------------------PUBLIC INTERFACE METHODS
    /**
     * Wrapper around cached session object, to allow it to be created prior to first use.  The session will be
     * cached in the session source object, so it should not be cached outside!
     *
     * @return existing cached session or newly created one.
     */
    public synchronized static Session getOrCreateSession() {
        Session session = getOrCreateSessionFactory().getCurrentSession();
        if (session == null) {
            session = getOrCreateSessionFactory().openSession();
        }
        return session;
    }

    /**
     * Dispose of any existing session object.
     */
    public synchronized static void closeSession() {
        Session session = getOrCreateSessionFactory().getCurrentSession();
        if (session != null) {
            if (session.isOpen())
                session.close();
            logger.info("Closed session");
        }
    }

    public synchronized static Configuration getOrCreateConfiguration() {
        if (_cfg == null) {
            Configuration cfg = new Configuration();
            URL hibernateConfigURL = HibernateSessionSource.class.getClassLoader().getResource(RESOURCE_LOCATION);
            String configResource = hibernateConfigURL.toString();
            cfg = cfg.configure(hibernateConfigURL);
            _cfg = cfg;

            logger.info("Created a configuration from " + configResource);
        }
        return _cfg;
    }

    public synchronized static SessionFactory getOrCreateSessionFactory() {
        Configuration cfg = getOrCreateConfiguration();
        if (_factory == null) {
            _factory = cfg.buildSessionFactory();
        }
        return _factory;
    }

}
