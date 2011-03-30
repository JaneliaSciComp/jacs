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

import org.janelia.it.jacs.web.gwt.common.client.core.BrowserDetector;

import javax.servlet.http.HttpServletRequest;

/**
 * Browser detector for JSPs and Servlets.
 */
public class JspBrowserDetector {
    public static final String USER_AGENT = "USER-AGENT";

    public static String getBrowser(HttpServletRequest request) {
        if (request == null)
            return BrowserDetector.UNKNOWN;

        String agent = request.getHeader(USER_AGENT);
        if (agent == null)
            return BrowserDetector.UNKNOWN;
        else
            agent = agent.toLowerCase();

        if (agent.indexOf(BrowserDetector.IE) != -1)
            return BrowserDetector.IE;
        else if (agent.indexOf(BrowserDetector.FF) != -1)
            return BrowserDetector.FF;
        else
            return BrowserDetector.UNKNOWN;
    }

    public static boolean isIE(HttpServletRequest request) {
        return getBrowser(request).equals(BrowserDetector.IE);
    }

    public static boolean isFF(HttpServletRequest request) {
        return getBrowser(request).equals(BrowserDetector.FF);
    }
}
