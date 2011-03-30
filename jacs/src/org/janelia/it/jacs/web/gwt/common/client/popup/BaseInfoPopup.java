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

package org.janelia.it.jacs.web.gwt.common.client.popup;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

/**
 * Simple grid-based informational popup.
 */
abstract public class BaseInfoPopup extends BasePopupPanel {
    /**
     * Won't show a title
     */
    public BaseInfoPopup(boolean realizeNow) {
        this(null, realizeNow);
    }

    public BaseInfoPopup(String title, boolean realizeNow) {
        super(title, /*realize now*/ false); // skip init until node is set
    }

    protected void addRow(int row, FlexTable grid, String prompt, String value) {
        grid.setWidget(row, 0, HtmlUtils.getHtml(prompt + ":", "prompt"));
        grid.setWidget(row, 1, HtmlUtils.getHtml(value, "text", "nowrap"));
        grid.getCellFormatter().setVerticalAlignment(row, 0, HasVerticalAlignment.ALIGN_TOP);
        grid.getCellFormatter().setVerticalAlignment(row, 1, HasVerticalAlignment.ALIGN_TOP);
        grid.getCellFormatter().addStyleName(row, 0, "gridCell");
        grid.getCellFormatter().addStyleName(row, 1, "gridCell");
    }
}