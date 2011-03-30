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

package org.janelia.it.jacs.web.gwt.detail.client.util;

import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

/**
 * This class is used to post messages to the title boxes on ReadDetail page
 *
 * @author Tareq Nabeel
 */
public class MessageUtil {

    /**
     * This method posts a "Not Found" message to a panel
     *
     * @param panel        The panel to post the message to
     * @param entityName   The entity name that the message is for e.g. "Sample"
     * @param searchString e.g. Read Id: 12122 or LibraryId: 324234
     */
    public static void addNotFoundErrorMessage(TitledBox panel, String entityName, String searchString) {
        panel.add(HtmlUtils.getHtml(entityName + " unavailable for " + searchString,
                "detailMessageError"));
    }

    /**
     * This method posts a message to a panel after clearing the panel
     *
     * @param panel        The panel to post the message to
     * @param entityName   The entity name that the message is for e.g. "Sample"
     * @param searchString e.g. Read Id: 12122 or LibraryId: 324234
     */
    public static void setNotFoundErrorMessage(TitledBox panel, String entityName, String searchString) {
        panel.clearContent();
        addNotFoundErrorMessage(panel, entityName, searchString);
    }

    /**
     * This method posts a data retrieval error message to a panel after clearing the panel
     *
     * @param panel        The panel to post the message to
     * @param entityName   The entity name that the message is for e.g. "Sample"
     * @param searchString e.g. Read Id: 12122 or LibraryId: 324234
     */
    public static void setServiceErrorMessage(TitledBox panel, String entityName, String searchString) {
        panel.clearContent();
        addServiceErrorMessage(panel, entityName, searchString);
    }

    /**
     * This method posts a data retrieval error message to a panel
     *
     * @param panel        The panel to post the message to
     * @param entityName   The entity name that the message is for e.g. "Sample"
     * @param searchString e.g. Read Id: 12122 or LibraryId: 324234
     */
    public static void addServiceErrorMessage(TitledBox panel, String entityName, String searchString) {
        panel.add(HtmlUtils.getHtml("Error retrieving " + entityName + " information for " + searchString,
                "detailMessageError"));
    }
}
