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

/**
 * PlatformDetector strings for reference:<ul>
 * <li>windows: "windows"</li>
 * <li>mac: "macintosh"</li>
 * <li>linux (suse): "linux i686"</li>
 * </ul>
 *
 * @author Michael Press
 */
public class PlatformDetector {
    public static final String WINDOWS = "windows";
    public static final String MAC = "mac";
    public static final String LINUX = "linux";
    public static final String UNKNOWN = "unknown";

    public static boolean isWindows() {
        return platform().equals(WINDOWS);
    }

    public static boolean isMac() {
        return platform().equals(MAC);
    }

    public static boolean isLinux() {
        return platform().equals(LINUX);
    }

    public static native String platform() /*-{
        var platform = navigator.platform.toLowerCase();
        
        if (platform.indexOf("win") == 0)
            return "windows";
        else if (platform.indexOf("mac") == 0)
            return "mac";
        else if (platform.indexOf("linux") == 0)
            return "linux";

        return platform;
  }-*/;
}
