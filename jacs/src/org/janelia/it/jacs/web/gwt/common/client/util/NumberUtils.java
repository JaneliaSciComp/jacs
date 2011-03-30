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

import com.google.gwt.i18n.client.NumberFormat;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;

/**
 * @author Michael Press
 */
public class NumberUtils {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.common.client.util.NumberUtils");

    public static final NumberFormat INTEGER_FORMAT = NumberFormat.getFormat("###,###");  // standard is ok
    public static final NumberFormat FLOAT_FORMAT = NumberFormat.getFormat("##.##");    // only want 2 significant digits

    /**
     * Formats a long for display (adds commas)
     */
    public static String formatLong(long value) {
        return INTEGER_FORMAT.format(value);
    }

    /**
     * Formats a float for display (adds commas, restricts to 2 significant digits, etc.)
     */
    public static String formatFloat(float value) {
        return FLOAT_FORMAT.format(value);
    }

    /**
     * Formats a double for display (adds commas, restricts to 2 significant digits, etc.)
     */
    public static String formatDouble(double value) {
        return FLOAT_FORMAT.format(value);
    }

    /**
     * True if the string is not null and length > 0
     */
    public static boolean hasValue(String str) {
        return str != null && str.length() > 0;
    }

    /**
     * Formats an Integer with commas
     */
    public static String formatInteger(Integer intNumber) {
        return (intNumber == null) ? "" : formatInteger(intNumber.toString());
    }

    /**
     * Formats an integer (held as a String) with commas
     */
    public static String formatInteger(String str) {
        try {
            return (str == null) ? "" : formatLong(Long.valueOf(str));
        }
        catch (NumberFormatException e) {
            _logger.error("Caught NumberFormatException parsing " + str);
            return "";
        }
    }
}