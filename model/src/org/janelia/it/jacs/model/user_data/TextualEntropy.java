/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.model.user_data;

import java.util.regex.Pattern;

/**
 * Cleans up file names and node names so that no semantic confusion results.
 *
 * @author fosterl
 */
public class TextualEntropy {
    public static final String PUNCTUATION = "[\\[\\]{}().,;:\\-=+|*&^%$#@!~`'?<>@\t\n \\\\/]";
    private static final Pattern PUNCT_PATTERN = Pattern.compile(PUNCTUATION);

    public static final String NON_FILE_SEP_PUNCT = "[\\[\\]{}().,;:\\-=+|*&^%$#@!~`'?<>@\t\n ]";
    private static final Pattern NON_FILE_SEP_PUNCT_PATTERN = Pattern.compile(NON_FILE_SEP_PUNCT);

    /**
     * This makes a node name out of a possibly punctuation-ridden input string.
     */
    public static String sanitizeNodeName(String nodeName) {
        String noPunct = PUNCT_PATTERN.matcher(nodeName).replaceAll("_").toLowerCase();
        int firstNonUnder = 0;
        while (noPunct.charAt(firstNonUnder) == '_') {
            firstNonUnder++;
        }
        int lastNonUnder = noPunct.length();
        while (noPunct.charAt(lastNonUnder - 1) == '_') {
            lastNonUnder--;
        }
        String trimEnd = noPunct.substring(firstNonUnder, lastNonUnder);
        return trimEnd.replaceAll("_+", "_");
    }

    /**
     * This makes a possibly hierarchical directory name out of a possibly
     * punctuation-ridden input string.
     */
    public static String sanitizeDirName(String nodeName) {
        String noPunct = NON_FILE_SEP_PUNCT_PATTERN.matcher(nodeName).replaceAll("_").toLowerCase();
        int firstNonUnder = 0;
        while (noPunct.charAt(firstNonUnder) == '_') {
            firstNonUnder++;
        }
        int lastNonUnder = noPunct.length();
        while (noPunct.charAt(lastNonUnder - 1) == '_') {
            lastNonUnder--;
        }
        String trimEnd = noPunct.substring(firstNonUnder, lastNonUnder);
        return trimEnd.replaceAll("_+", "_");
    }


}
