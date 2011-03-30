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

package org.janelia.it.jacs.compute.service.export.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Jul 2, 2008
 * Time: 4:37:20 PM
 */

public class CSVDataConversionHelper {
    static public final int DEFAULT_MAX_SEQ_LENGTH_IN_CSV = 2000;
    static public final String NULL_PLACEHOLDER = "n/a";
    static public final String SEPARATOR_CHARACTER = ",";

    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private static SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a");

    private CSVDataConversionHelper() {
    }

    static public String escapeSpecialExcelChars(String in) {
        if (in == null || in.trim().length() == 0 || in.trim().equals("null"))
            return NULL_PLACEHOLDER;
        String out = in.replaceAll("\"", "\"\"");
        boolean specChar = false;
        for (int i = 0; i < out.length(); i++) {
            if (out.charAt(i) == ',' || out.charAt(i) == '\n') {
                specChar = true;
            }
        }
        if (specChar) {
            out = "\"" + out + "\"";
        }

        return out;
    }

    static public String n2s(String s) {
        if (s == null) {
            return NULL_PLACEHOLDER;
        }
        else {
            return s;
        }
    }

    static public String n2s(Float f) {
        if (f == null) {
            return NULL_PLACEHOLDER;
        }
        else {
            return f.toString();
        }
    }

    static public String n2s(Integer i) {
        if (i == null) {
            return NULL_PLACEHOLDER;
        }
        else {
            return i.toString();
        }
    }

    static public String n2s(Double d) {
        if (d == null) {
            return NULL_PLACEHOLDER;
        }
        else {
            return d.toString();
        }
    }

    static public String n2sDate(Date d) {
        if (d == null) {
            return "";
        }
        else {
            return dateFormat.format(d);
        }
    }

    static public String n2sTime(Date t) {
        if (t == null) {
            return "";
        }
        else {
            return timeFormat.format(t);
        }
    }

}

