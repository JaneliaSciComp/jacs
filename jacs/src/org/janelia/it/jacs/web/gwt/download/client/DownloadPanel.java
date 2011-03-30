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

package org.janelia.it.jacs.web.gwt.download.client;

import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import org.janelia.it.jacs.web.gwt.common.client.model.download.DownloadableDataNode;
import org.janelia.it.jacs.web.gwt.download.client.formatter.DataFileFormatter;
import org.janelia.it.jacs.web.gwt.download.client.formatter.DownloadLinkBuilder;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Lfoster
 * Date: Sep 8, 2006
 * Time: 1:33:45 PM
 * <p/>
 * Fixed panel (as opposed to a dialog) to allow users to download files.
 */
public class DownloadPanel extends VerticalPanel {

    VerticalPanel contentPanel;

    /**
     * Create a panel for downloading all relevant files.
     *
     * @param subjectDocument      PDF based on data.
     * @param rolledUpDataArchives data cited in the PDF.
     */
    public DownloadPanel(DownloadableDataNode subjectDocument, List rolledUpDataArchives) {

        DataFileFormatter formatter = new DataFileFormatter();

        //TODO consider removing these in favor of a single HTMLpanel with description and links.
        if (subjectDocument != null) {
            String descriptiveText = formatter.getDescriptiveText(subjectDocument);
            HTMLPanel pdfLinkPanel =
                    new HTMLPanel(
                            "<a href='" +
                                    subjectDocument.getLocation() +
                                    "'>" + descriptiveText + "</a>");

            pdfLinkPanel.setStyleName("DownloadDownloadWidgetText");
            add(pdfLinkPanel);
        }

        for (int i = 0; rolledUpDataArchives != null && i < rolledUpDataArchives.size(); i++) {
            DownloadableDataNode dataFile = (DownloadableDataNode) rolledUpDataArchives.get(i);
            String descriptiveText = formatter.getDescriptiveText(dataFile);
            HTMLPanel linkPanel =
                    new HTMLPanel(
                            "<a href='" +
                                    DownloadLinkBuilder.getDownloadLink(dataFile.getLocation()) +
                                    "'>" + descriptiveText + "</a>");

            linkPanel.setStyleName("DownloadDownloadWidgetText");
            add(linkPanel);
        }

    }

}
