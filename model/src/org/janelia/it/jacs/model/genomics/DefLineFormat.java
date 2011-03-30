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

package org.janelia.it.jacs.model.genomics;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * DefLineFormat
 */
public class DefLineFormat {
    private static final String DEFLINE_ACC = "defline_accession";

    public DefLineFormat() {
    }

    public String formatDefline(Map deflineMap) {
        if (deflineMap == null) {
            return null;
        }
        StringBuffer deflineBuffer = new StringBuffer();
        formatDefline(deflineMap, deflineBuffer);
        return deflineBuffer.toString();
    }

    public StringBuffer formatDefline(Map deflineMap, StringBuffer deflineBuffer) {
        String accession = (String) deflineMap.get(DEFLINE_ACC);
        if (accession != null && accession.length() > 0) {
            deflineBuffer.append(accession);
            deflineBuffer.append(' ');
        }
        int entryIndex = 0;
        for (Iterator itr = deflineMap.keySet().iterator(); itr.hasNext(); entryIndex++) {
            String deflineEntryProperty = (String) itr.next();
            if (deflineEntryProperty.equals(DEFLINE_ACC)) {
                continue;
            }
            if (entryIndex != 0) {
                deflineBuffer.append(' ');
            }
            Object deflineEntryValue = deflineMap.get(deflineEntryProperty);
            deflineBuffer.append('/');
            deflineBuffer.append(deflineEntryProperty);
            deflineBuffer.append('=');
            if (deflineEntryValue instanceof Map) {
                deflineBuffer.append('"');
                formatDefline((Map) deflineEntryValue, deflineBuffer);
                deflineBuffer.append('"');
            }
            else if (deflineEntryValue instanceof String) {
                deflineBuffer.append((String) deflineEntryValue);
            }
        }
        return deflineBuffer;
    }

    public Map parseDefline(String deflineDescription) {
        Map deflineMap = null;
        if (deflineDescription != null) {
            deflineMap = new HashMap();
            parseDefline(deflineDescription, deflineMap, true);
        }
        return deflineMap;
    }

    /**
     * Parses the <code>deflineDescription</code> and initializes the
     * <code>deflineMap</code> object
     *
     * @param deflineDescription
     */
    public static void parseDefline(String deflineDescription, Map deflineMap, boolean includeBracketInAccession) {
        if (deflineDescription == null) {
            return;
        }
        String propertyName = null;
        String propertyValue;
        StringBuffer buffer = new StringBuffer();
        char[] deflineChars = deflineDescription.toCharArray();
        int deflineCharsPos = 0;
        char c;
        // skip the first accession
        for (; deflineCharsPos < deflineChars.length; deflineCharsPos++) {
            c = deflineChars[deflineCharsPos];
            if (c == '/') {
                break;
            }
            else if (c == ' ') {
                deflineCharsPos++;
                break;
            }
            else {
                if (includeBracketInAccession || (c != '>')) {
                    buffer.append(c);
                }
            }
        }
        // the buffer contains the accession number
        String accession = buffer.toString();
        buffer.setLength(0);
        if (accession.length() > 0) {
            deflineMap.put(DEFLINE_ACC, accession);
        }
        for (; deflineCharsPos < deflineChars.length; deflineCharsPos++) {
            c = deflineChars[deflineCharsPos];
            if (c == '/') {
                // property name start
                propertyValue = buffer.toString().trim();
                if (propertyName != null) {
                    deflineMap.put(propertyName, propertyValue);
                }
                // reset everything for a new name-value pair
                propertyName = null;
                propertyValue = null;
                buffer.setLength(0);
            }
            else if (c == '=') {
                // name - value delimiter
                propertyName = buffer.toString().trim();
                if (propertyName.length() == 0) {
                    propertyName = null;
                }
                // reset the buffer
                buffer.setLength(0);
            }
            // todo How can this be right?  string, map?  I thought it should be string, string?
            else if (c == '"') {
                deflineCharsPos = readValueEnclosedWithinQuotes(deflineChars, deflineCharsPos, buffer);
                Map enclosedDefline = new HashMap();
                parseDefline(buffer.toString(), enclosedDefline, true);
                deflineMap.put(propertyName, enclosedDefline);
            }
            else {
                buffer.append(c);
            }
        }
        // last property value
        propertyValue = buffer.toString().trim();
        if (propertyName != null) {
            deflineMap.put(propertyName, propertyValue);
        }
    }

    /**
     * Reads a defline's value enclosed within double quotes
     *
     * @param deflineChars
     * @param startDeflineCharIndex
     * @param buffer
     * @return updated position in the deflineChars buffer
     */
    private static int readValueEnclosedWithinQuotes(char[] deflineChars, int startDeflineCharIndex, StringBuffer buffer) {
        int deflineCharIndex = startDeflineCharIndex + 1;
        for (; deflineCharIndex < deflineChars.length; deflineCharIndex++) {
            char c = deflineChars[deflineCharIndex];
            if (c == '"') {
                break;
            }
            else {
                buffer.append(c);
            }
        }
        return deflineCharIndex;
    }

}
