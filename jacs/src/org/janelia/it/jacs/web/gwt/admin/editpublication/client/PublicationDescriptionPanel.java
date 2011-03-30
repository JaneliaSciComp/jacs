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

package org.janelia.it.jacs.web.gwt.admin.editpublication.client;

import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.web.gwt.admin.editproject.client.EPTableUtilities;
import org.janelia.it.jacs.web.gwt.admin.editproject.client.RequiredPanel;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ExternalLink;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;


public class PublicationDescriptionPanel extends VerticalPanel {

    //private TextArea abstractTextArea = null;
    private TextArea textArea = null;

    public PublicationDescriptionPanel(PublicationTabPanel parent) {

        super();

        createDescriptionPanel();

        this.setStyleName("EPFullPanel");

        parent.add(this, "Description");
    }


    private void createDescriptionPanel() {

        Label abstractLabel = new Label("Publication Abstract (text only):");
        abstractLabel.setStyleName("prompt");

        /*
        HorizontalPanel abstractPanel = new HorizontalPanel();
            abstractPanel.setVerticalAlignment(HorizontalPanel.ALIGN_BOTTOM);
            abstractPanel.add(abstractLabel);
            abstractPanel.setHorizontalAlignment(HorizontalPanel.ALIGN_RIGHT);
            abstractPanel.setVerticalAlignment(HorizontalPanel.ALIGN_TOP);

            abstractPanel.setStyleName("EPDescriptionPrompt");
        */

        // "Project Description" Prompt //
        FlexTable descriptionGrid = new FlexTable();
        EPTableUtilities.addWidgetWidgetPair(
                descriptionGrid, 0, 0, new HTMLPanel("<span class='prompt'>Project Description (HTML):</span>" +
                        "<span class='requiredInformation'>&nbsp;*</span>"),
                EPTableUtilities.getStyleLinkCell(new ExternalLink("style tutorial", "/jacs/styleTutorial.htm")));

        HorizontalPanel descriptionTitlePanel = new HorizontalPanel();
        descriptionTitlePanel.setVerticalAlignment(HorizontalPanel.ALIGN_BOTTOM);
        descriptionTitlePanel.add(descriptionGrid);
        descriptionTitlePanel.setStyleName("EPDescriptionPrompt");
        descriptionTitlePanel.add(new RequiredPanel());

        // HTML Description TextArea //
        textArea = new TextArea();
        textArea.setStyleName("EPTextPanel");

        add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        add(descriptionTitlePanel);
        add(textArea);
    }


    public String getDescription() {
        return textArea.getText();
    }


    public void setSampleNewPublication() {

        textArea.setText("<html>\n" +
                "\n" +
                "<body></br></br></br></br>" +
                "in vitro motility assay  blah blah blah blah blah\n" +
                "in vitro motility assayin vitro motility assay\n" +
                "<br>" + "<br>" + "<br>" + "<br>" +
                "in vitro motility assay  blah blah blah blah blah blah\n" +
                "in vitro motility assayv vitro motilty a  blah blah blah blah\n" +
                //"<IMG SRC=\"/jacs/images/home/samples-thumb.jpg\" align=\"left\">\n" +
                "in vitro yvin vitro motility assayv  blah blah blah\n" +
                "in vo motil   itro motility assayvin vitro motility assayvin vitro motility assayv\n" +
                "o motiliity assayin vitro motility assay  blah blah blah\n" +
                "in vitr blah vitro motilty avitro motilty a blah blah blah blah\n" +
                "<br>" + "<br>" + "<br>" + "<br>" + "<br>" + "<br>" +
                "o motility assay  blah blah blah blah blah blah blah \n" +
                "in vitro motility assayin vitrsayin vitro motility assay\n" +
                "in vitro motilty assay blah blah blah blah blah blah blah\n" +
                "<br>" + "<br>" + "<br>" + "<br>" +
                "in vitro motility assay" +
                "ity assayin vitro motility assay\n" +
                " blah blah blah blah blah blah blah blah blah blah blah blah \n" +
                "</body>\n" +
                "\n" +
                "</html>");


    }

}
