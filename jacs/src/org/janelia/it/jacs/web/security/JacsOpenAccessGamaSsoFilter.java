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

import edu.sdsc.gama.services.SSO.GAMASSOUser;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by IntelliJ IDEA.
 * User: lkagan
 * Date: Nov 6, 2007
 * Time: 1:53:40 PM
 */
public class JacsOpenAccessGamaSsoFilter extends JacsGamaSsoFilter {
    public final static String CAMERA_ANONYMOUS_USER_ID = "__CAMERA__ANONYMOUS__";

    protected GAMASSOUser getUserFromSSO(HttpServletRequest httpServletRequest) {
        // try to retrieve GAMA user object. If it is not retrieved - timed out or
        // not logged in - will allow public access
        GAMASSOUser gamaUser = super.getUserFromSSO(httpServletRequest);
        if (gamaUser == null) {
            gamaUser = new GAMASSOUser(CAMERA_ANONYMOUS_USER_ID);
        }

        return gamaUser;
    }

}
