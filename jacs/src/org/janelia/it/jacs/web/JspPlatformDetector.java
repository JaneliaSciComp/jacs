
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
