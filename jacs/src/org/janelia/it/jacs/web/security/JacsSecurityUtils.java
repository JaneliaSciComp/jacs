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

package org.janelia.it.jacs.web.security;

import org.janelia.it.jacs.model.user_data.User;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by IntelliJ IDEA.
 * User: lkagan
 * Date: Nov 6, 2007
 * Time: 4:39:56 PM
 */
public class JacsSecurityUtils {
    public final static String USER_OBJECT_ATTR_NAME = "USER_OBJECT";

    public static boolean isAuthenticated(HttpServletRequest request) {
        User u = getSessionUser(request);
        return (u != null && u.getUserLogin() != null && !u.getUserLogin().equals("__CAMERA__ANONYMOUS__"));
    }

    public static boolean isAdmin(HttpServletRequest request) {
        User user = getSessionUser(request);
        return user != null && user.isAdministrator();
    }

    public static User getSessionUser(HttpServletRequest request) {
        return (User) request.getSession().getAttribute(USER_OBJECT_ATTR_NAME);
    }
}
