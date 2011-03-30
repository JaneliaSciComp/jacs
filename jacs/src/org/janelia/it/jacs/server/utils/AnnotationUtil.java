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

package org.janelia.it.jacs.server.utils;

import org.janelia.it.jacs.model.genomics.AnnotationDescription;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracted methods from SearchDAOImpl that are used by DAOs in other packages
 */
public class AnnotationUtil {

    private static final String ROW_SEP = "\n";
    private static final String LIST_SEP = "\t";

    public static List<AnnotationDescription> createAnnotationListFromString(String listString) {
        return createAnnotationListFromString(listString, ROW_SEP, LIST_SEP);
    }

    public static List<AnnotationDescription> createAnnotationListFromString(String listString, String rowSep, String listSep) {
        List<AnnotationDescription> annoList = new ArrayList<AnnotationDescription>();
        if (listString == null)
            return annoList;
        String[] rowArr = listString.split(rowSep);
        for (String r : rowArr) {
            String[] l = r.split(listSep);
            if (l.length >= 2) {
                AnnotationDescription ad = new AnnotationDescription();
                ad.setId(l[0]);
                StringBuffer dBuffer = new StringBuffer();
                for (int i = 1; i < l.length; i++) {
                    if (i == 1)
                        dBuffer.append(l[i]);
                    else
                        dBuffer.append(", " + l[i]);
                }
                ad.setDescription(dBuffer.toString());
                annoList.add(ad);
            }
        }
        return annoList;
    }
}
