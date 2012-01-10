
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