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

package org.janelia.it.jacs.web.gwt.common.client.core;

import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;

/**
 * User Agent strings for reference:<ul>
 * <li>win/FF1: mozilla/5.0 (windows; u; windows nt 5.1; en-us; rv:1.8.0.10) gecko/20070216 firefox/1.5.0.10</li>
 * <li>win/FF2: mozilla/5.0 (windows; u; windows nt 5.1; en-us; rv:1.8.1.14) gecko/20080404 firefox/2.0.0.14</li>
 * <li>win/FF3: mozilla/5.0 (windows; u; windows nt 5.1; en-us; rv:1.9) gecko/2008052906 firefox/3.0</li>
 * <li>win/IE6: mozilla/4.0 (compatible; msie 6.0; windows nt 5.1; sv1; .net clr 1.1.4322; .net clr 2.0.50727; .net clr 3.0.04506.30; .net clr 3.0.0450</li>
 * <li>win/IE7: mozilla/4.0 (compatible; msie 7.0; windows nt 5.1; .net clr 1.1.4322; .net clr 2.0.50727; .net clr 3.0.04506.30; infopath.1; .net clr</li>
 * <li>win/saf: mozilla/5.0 (windows; u; windows nt 5.1; en-us) applewebkit/525.13 (khtml, like gecko) version/3.1 safari/525.13<li>
 * <p/>
 * <li>mac/FF2: mozilla/5.0 (macintosh; u; ppc mac os x mach-o; en-us; rv:1.8.1.4) gecko/20070515 firefox/2.0.0.4</li>
 * <li>mac/FF3: mozilla/5.0 (macintosh; u; ppc mac os x 10.4; en-us; rv:1.9) gecko/2008053008 firefox/3.0</li>
 * <p/>
 * <li>lin/FF2: mozilla/5.0 (x11; u; linux i686; en-us; rv:1.8.1.2) gecko/20070220 firefox/2.0.0.2</li>
 * <li>lin/FF3: ...</li>
 * </ul>
 *
 * @author Michael Press
 */
public class BrowserDetector {
    private static Logger _logger = Logger.getLogger("");

    public static final String IE = "msie";
    public static final String IE6 = IE + " 6.0";
    public static final String IE7 = IE + " 7.0";

    public static final String FF = "firefox";
    public static final String FF2 = "rv:1.8";
    public static final String FF3 = "rv:1.9";

    public static final String SAFARI = "safari";
    public static final String UNKNOWN = "unknown";

    /**
     * Returns true if browser is Firefox (any version)
     */
    public static boolean isFF() {
        return isBrowser(FF);
    }

    public static boolean isFF2() {
        return isBrowser(FF) && isBrowser(FF2);
    }

    public static boolean isFF3() {
        return isBrowser(FF) && isBrowser(FF3);
    }

    /**
     * Returns true if browser is IE (any version)
     */
    public static boolean isIE() {
        return isBrowser(IE);
    }

    /**
     * Returns true if browser is IE 6
     */
    public static boolean isIE6() {
        return isBrowser(IE6);
    }

    /**
     * Returns true if browser is IE 6
     */
    public static boolean isIE7() {
        return isBrowser(IE7);
    }

    /**
     * Returns true if browser is Safari
     */
    public static boolean isSafari() {
        return isBrowser(SAFARI);
    }

    public static native String getUserAgent() /*-{
        return navigator.userAgent.toLowerCase();
    }-*/;

    /**
     * Returns true if the given browser string is contained in the user agent
     */
    protected static boolean isBrowser(String browser) {
        return (getUserAgent().indexOf(browser) != -1);
    }
}
