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

package org.janelia.it.jacs.server.access;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import javax.naming.InitialContext;

/**
 * Created by IntelliJ IDEA.
 * User: tdolafi
 * Date: Mar 30, 2006
 * Time: 11:00:27 AM
 */
public class HibernateSession {

    private static final String HIBERNATE_SESSION_FACTORY = "java:/hibernate/ComputeSessionFactory";
    private static Logger logger = Logger.getLogger(HibernateSession.class);

    public static Session getHibernateSession() {
        InitialContext ctx;
        SessionFactory sessionFactory;
        Session session = null;

        try {
            ctx = new InitialContext();
            sessionFactory = (SessionFactory) ctx.lookup(HIBERNATE_SESSION_FACTORY);
            session = sessionFactory.openSession();

        }
        catch (Exception e) {
            logger.debug(e);
        }
        return session;
    }
}