package org.janelia.it.jacs.compute.util;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.InitialContext;

/**
 * Created by goinac on 9/15/15.
 */
public class HibernateSessionUtils {

    private static final Logger LOG = LoggerFactory.getLogger(HibernateSessionUtils.class);
    private static SessionFactory sessionFactory;

    private static SessionFactory getSessionFactory() {
        try {
            if (sessionFactory == null) {
                InitialContext namingContext = new InitialContext();

                sessionFactory = (SessionFactory) namingContext.lookup("java:/hibernate/ComputeSessionFactory");
            }
            return sessionFactory;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static Session getSession() {
        SessionFactory sessionFactory = getSessionFactory();
        Session session = null;
        try {
            session = sessionFactory.getCurrentSession();
        } catch (HibernateException ignore) {
            LOG.debug("Error while reading current hibernate session (ignored)", ignore);
        }
        if (session == null) {
            session = sessionFactory.openSession();
        }
        return session;
    }

    public static void closeSession(Session session) {
        if (session != null && session.isOpen()) session.close();
    }


}
