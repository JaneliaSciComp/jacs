
package org.janelia.it.jacs.compute.access;

import org.apache.log4j.Logger;
import org.hibernate.Query;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: sreenath
 * Date: Sep 3, 2009
 * Time: 11:59:51 AM
 */
public class UserDAO extends ComputeBaseDAO {

    public UserDAO(Logger logger) {
        super(logger);
    }

    public String getEmailByUserName(String requestingUser) {
        String sql = "select email from user_accounts where user_login = '" + requestingUser + "'";
        Query query = getCurrentSession().createSQLQuery(sql);
        List<String> returnList = query.list();

        if (null == returnList || returnList.size() <= 0) {
            _logger.debug("No email found for user " + requestingUser + " - SQL: '" + sql + "'");
            return null; // empty list
        }

        return returnList.get(0);
    }

}
