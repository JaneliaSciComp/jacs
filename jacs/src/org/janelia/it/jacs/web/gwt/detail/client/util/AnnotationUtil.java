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

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.genomics.AnnotationDescription;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupAboveRightAlignedLauncher;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.EnzymeCommissionExternalLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.GeneOntologyExternalLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.Link;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

import java.util.List;

/**
 * @author Michael Press
 */
public class AnnotationUtil {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.common.client.util.AnnotationUtil");

    public static String createAnnotationStringFromAnnotationList(List<AnnotationDescription> annotationList) {
        StringBuffer text = new StringBuffer();
        for (AnnotationDescription annot : annotationList) {
            if (text.length() > 0)
                text.append(",");
            text.append(annot.getId());
        }
        return text.toString();
    }

    /**
     * Creates a panel with 3 x N annotation link widgets (3 per line, however many lines necessary).  Returns null
     * if the annotationList is empty so that the blank cell displays correctly in FF.
     */
    public static Panel createAnnotationLinksWidget(List<AnnotationDescription> annotationList) {
        if (annotationList == null || annotationList.size() == 0)
            return null;
        final int MAX_COLS = 5; // 3 values + 2 commas
        FlexTable table = new FlexTable();
        table.setCellPadding(0);
        table.setCellSpacing(0);

        int row = 0;
        int col = 0;
        for (AnnotationDescription annotation : annotationList) {
            if (col > 0) // add comma after the first annotation
                table.setWidget(row, col++, HtmlUtils.getHtml(",&nbsp;&nbsp;", "text"));
            table.setWidget(row, col++, createAnnotationExternalLink(annotation.getId().trim(), arrowizeAnnotationDescription(annotation.getDescription())));
            if (col == MAX_COLS) {
                row++;
                col = 0;
            }
        }

        return table;
    }

    public static String arrowizeAnnotationDescription(String description) {
        return description.replaceAll("\\|\\|", "&rarr;"); // replace "||" with "->" (right arrow)
    }

    public static Widget createAnnotationExternalLink(String id, String desc) {
        Link annotationLink;

        if (id.startsWith("EC:"))
            annotationLink = new EnzymeCommissionExternalLink(id, desc, new PopupAboveRightAlignedLauncher());
        else if (id.startsWith("GO"))
            annotationLink = new GeneOntologyExternalLink(id, desc, new PopupAboveRightAlignedLauncher());
        else
            throw new IllegalArgumentException("Invalid annotation " + id);

        return annotationLink;
    }
}
