
package org.janelia.it.jacs.web.gwt.common.client.util;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Jul 26, 2007
 * Time: 2:14:33 PM
 */
public class StringUtils {
    /**
     * True if the string is not null and length > 0
     */
    public static boolean hasValue(String str) {
        return str != null && str.trim().length() > 0;
    }

    public static String wrapTextAsText(String defline, int targetWidth) {
        return wrapText(defline, targetWidth, "\n");
    }

    public static String wrapTextAsHTML(String defline, int targetWidth) {
        return wrapText(defline, targetWidth, "<br>");
    }

    public static String wrapText(String str, int targetWidth, String lineBreak) {
        if (str == null)
            return ("");

        String[] tokens = str.split("\\s+");
        if (tokens.length == 0)
            return ("");

        int lineWidth = 0;
        StringBuffer out = new StringBuffer();

        for (String token : tokens) {
            if (lineWidth > targetWidth) {
                out.append(lineBreak).append(token);
                lineWidth = token.length();
            }
            else {
                out.append(" ").append(token);
                lineWidth += token.length();
            }
        }

        return out.toString();
    }
}
