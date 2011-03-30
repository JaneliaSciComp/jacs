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
