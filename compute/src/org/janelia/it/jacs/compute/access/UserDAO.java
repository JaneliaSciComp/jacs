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
