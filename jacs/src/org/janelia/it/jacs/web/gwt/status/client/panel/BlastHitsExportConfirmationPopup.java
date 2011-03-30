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

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.popup.ModalPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.ui.ButtonSet;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

import java.util.List;

/**
 * @author Cristian Goina
 */
class BlastHitsExportConfirmationPopup extends ModalPopupPanel {

    private int selection;
    private List selectedItems;
    private BlastResultsExportCommand exportCommand;

    BlastHitsExportConfirmationPopup(List selectedItems, BlastResultsExportCommand exportCommand, boolean realizeNow) {
        super("Export", realizeNow);
        this.selectedItems = selectedItems;
        this.exportCommand = exportCommand;
    }

    protected ButtonSet createButtons() {
        final RoundedButton[] tmpButtons = new RoundedButton[3];
        tmpButtons[0] = new RoundedButton("Export Selected Items", new ClickListener() {
            public void onClick(Widget widget) {
                hide();
                exportCommand.exportResults(selectedItems);
            }
        });
        tmpButtons[1] = new RoundedButton("Export All Items", new ClickListener() {
            public void onClick(Widget widget) {
                hide();
                exportCommand.exportResults(null);
            }
        });
        tmpButtons[2] = new RoundedButton("Cancel", new ClickListener() {
            public void onClick(Widget widget) {
                hide();
            }
        });
        return new ButtonSet(tmpButtons);
    }

    /**
     * For subclasses to supply dialog content
     */
    protected void populateContent() {
        add(HtmlUtils.getHtml("There are currently " + String.valueOf(selectedItems.size()) + " items selected", "text"));
        add(HtmlUtils.getHtml("&nbsp;", "text"));
        add(HtmlUtils.getHtml("Do you want to export only the selected items, or all items? ", "text"));
        add(HtmlUtils.getHtml("&nbsp;", "text"));
    }

    int getSelection() {
        return selection;
    }

}
