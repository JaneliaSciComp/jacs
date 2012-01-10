
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
