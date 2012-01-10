
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
