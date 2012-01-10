
package org.janelia.it.jacs.server.access;

import org.hibernate.SessionFactory;

/**
 * Created by IntelliJ IDEA.
 * User: tnabeel
 * Date: Mar 12, 2007
 * Time: 10:42:02 AM
 */
public interface DAO {

    SessionFactory getSessionFactory();
}
