package org.janelia.it.jacs.compute.service.tic;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is used to assist in the manipulation of TIC data
 * User: saffordt
 * Date: 9/10/12
 * Time: 1:40 PM
 */
public class TICHelper {

    public static String getTargetPrefix(String targetName) {
        // First, try to match against the new format
        Pattern newPattern = Pattern.compile("_t[0-9]*.tif");
        Matcher matcher = newPattern.matcher(targetName);
        if (matcher.find()) {
            return targetName.substring(0,matcher.start());
        }
        // If initial match not found then try the other match
        else {
            Pattern oldPattern = Pattern.compile("_[0-9]*_[a-zA-Z]*.tif");
            Matcher oldMatcher = oldPattern.matcher(targetName);
            if (oldMatcher.find()) {
                return targetName.substring(0,oldMatcher.start());
            }
        }
        // If we can't match then there is no unique prefix to suggest
        return null;
    }
}
