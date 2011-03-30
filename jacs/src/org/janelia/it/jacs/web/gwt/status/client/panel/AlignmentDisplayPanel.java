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

package org.janelia.it.jacs.web.gwt.status.client.panel;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.web.gwt.common.client.model.genomics.BlastHit;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

/**
 * @author Michael Press
 */
public class AlignmentDisplayPanel extends VerticalPanel {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.status.client.panle.AlignmentDisplayPanel");

    public void setHit(BlastHit hit) {
        add(queryToHtml(hit.getPairwiseAlignmentWide()));
        add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        add(getAlignmentLegend());
    }

    public static HTML queryToHtml(String pairwiseAlignment) {
        return HtmlUtils.getHtml(formatQuery(pairwiseAlignment), "fixedWidthText");
    }

    /**
     * Formats the (text) alignment string for HTML. Replaces:<ul>
     * <li>spaces with &nbsp's</li>
     * <li> dashes with spaces (they were inserted so they could be converted to spaces AFTER the real
     * spaces are converted to nbsp's</li>
     * <li>\n\n's with a span to separate groups of alignment lines</li>
     * <li>\n's with <br></li>
     * </ul>
     *
     * @param pairwiseAlignment
     * @return
     */
    public static String formatQuery(String pairwiseAlignment) {
        return pairwiseAlignment
                .replaceAll(" ", "&nbsp;")
                .replaceAll("%%", " ")
                .replaceAll("\n\n", "<br/><span class='blastHitAlignmentGroupSeparator'>&nbsp;</span><br/>")
                .replaceAll("\n", "<br/>")
                ;
    }

    private Widget getAlignmentLegend() {
        HorizontalPanel contents = new HorizontalPanel();
        contents.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);

        contents.add(HtmlUtils.getHtml("Key:", "infoPrompt"));
        contents.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        addLegendItem(contents, "alignmentMatch", "match");
        contents.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        addLegendItem(contents, "alignmentSimilar", "similar");
        contents.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        addLegendItem(contents, "alignmentStopCodon", "stop codon");

        //SimplePanel panel = new SimplePanel(); // wrapper for main contents, so padding doesn't apply to each item in HorizPanel
        //DOM.setStyleAttribute(panel.getElement(), "display", "inline");
        //panel.setStyleName("blastAlignmentLegendPanel");
        //panel.add(contents);

        //return panel;
        //CenteredWidgetHorizontalPanel wholePanel = new CenteredWidgetHorizontalPanel(); // full width of the parent
        //DOM.setStyleAttribute(wholePanel.getElement(), "display", "inline");
        //wholePanel.add(panel);

        return contents;
    }

    private void addLegendItem(Panel panel, String colorStyleName, String legendText) {
        HTML matchColor = HtmlUtils.getHtml("&nbsp;&nbsp;", colorStyleName);
        matchColor.addStyleName("blastAlignmentLegendColorBlock");

        HTML textHtml = HtmlUtils.getHtml("&nbsp;" + legendText, "infoText");
        DOM.setStyleAttribute(textHtml.getElement(), "display", "inline");

        panel.add(matchColor);
        panel.add(textHtml);
    }
}