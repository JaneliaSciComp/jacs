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

package org.janelia.it.jacs.web.gwt.download.client.formatter;

/**
 * Created by IntelliJ IDEA.
 * User: Lfoster
 * Date: Oct 6, 2006
 * Time: 1:00:27 PM
 * <p/>
 * Create links usable for download.
 */
public class DownloadLinkBuilder {

    /**
     * Static class, only.
     */
    private DownloadLinkBuilder() {
    }

    /**
     * return a link to handoff to a file delivery service.
     */
    public static String getDownloadLink(String dataFileLocation) {

        // NOTE: may not use the system setting for file sep, because that has not
        // been implemented in the GWT emulation.
        int lastPathLeg = dataFileLocation.lastIndexOf("/");
        if (lastPathLeg == -1)
            lastPathLeg = dataFileLocation.lastIndexOf("\\");
        String dataFileNameOnly = null;
        if (lastPathLeg > -1)
            dataFileNameOnly = dataFileLocation.substring(lastPathLeg + 1);
        else
            dataFileNameOnly = dataFileLocation;

        int lastDotPos = dataFileNameOnly.lastIndexOf(".");
        dataFileNameOnly = dataFileNameOnly.replace('.', '_');
        dataFileNameOnly = dataFileNameOnly.substring(0, lastDotPos) + "." +
                dataFileNameOnly.substring(lastDotPos + 1);

        return "/jacs/fileDelivery.htm?inputfilename=" +
                dataFileLocation +
                "&suggestedfilename=" +
                dataFileNameOnly;
    }
}
