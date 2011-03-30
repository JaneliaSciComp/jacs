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

package org.janelia.it.jacs.web;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Michael Press
 */
public class JspPlatformDetector {
    public static final String USER_AGENT = "USER-AGENT";
    private static final String WINDOWS = "windows";
    private static final String MACINTOSH = "macintosh";
    private static final String MAC_OS = "mac os";

    protected static String userAgent(HttpServletRequest req) {
        if (req == null)
            return null;

        String userAgent = req.getHeader(USER_AGENT);
        if (userAgent == null)
            return "unknown";
        else
            return userAgent.toLowerCase();
    }

    public static boolean isWindows(HttpServletRequest req) {
        return req != null && userAgent(req).indexOf(WINDOWS) != -1;
    }

    public static boolean isMac(HttpServletRequest req) {
        return req != null && (userAgent(req).indexOf(MAC_OS) != -1 || userAgent(req).indexOf(MACINTOSH) != -1);
    }
}
