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

package org.janelia.it.jacs.web.control;

import com.sun.security.auth.UserPrincipal;
import org.janelia.it.jacs.model.user_data.User;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;

/**
 * Created by IntelliJ IDEA.
 * User: lkagan
 * Date: Nov 6, 2007
 * Time: 2:45:29 PM
 */
public class OpenAccessUserVerificationInterceptor extends UserVerificationInterceptor {
    private static final Long ANONYMOUS_USER_DB_ID = new Long(100L);

    protected User obtainUser(String userLoginId, String userName) throws Exception {
        if ("__CAMERA__ANONYMOUS__".equals(userLoginId)) {
            return super.obtainUser(userLoginId, "Public Access User", ANONYMOUS_USER_DB_ID);
//            User u = new User();
//            u.setUserId(ANONYMOUS_USER_DB_ID);
//            return u;
        }
        else
            return super.obtainUser(userLoginId, userName);    //use real one
    }

    protected Principal getPrincipal(HttpServletRequest request) {
        Principal p = request.getUserPrincipal();
        if (request.getRequestURL().indexOf("healthMonitor.chk") >= 0) {
            // Specifies the time, in seconds, between client requests before the servlet container will invalidate
            // this session. A negative time indicates the session should never timeout.
            // We don't want healthCheck sessions around forever in the server.  It's bad.
            request.getSession().setMaxInactiveInterval(30);
        }
        if (p == null) {
            // generate one out of thin air to allow anonymous access
            p = new UserPrincipal("__CAMERA__ANONYMOUS__");
        }

        return p;
    }
}
