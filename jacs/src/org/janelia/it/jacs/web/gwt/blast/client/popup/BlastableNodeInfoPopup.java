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

package org.janelia.it.jacs.web.gwt.blast.client.popup;

import com.google.gwt.user.client.ui.FlexTable;
import org.janelia.it.jacs.model.common.BlastableNodeVO;
import org.janelia.it.jacs.web.gwt.common.client.util.NumberUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.StringUtils;

/**
 * @author Michael Press
 */
public class BlastableNodeInfoPopup extends org.janelia.it.jacs.web.gwt.common.client.popup.BaseInfoPopup {
    private BlastableNodeVO _node;

    /**
     * Won't show a title
     */
    public BlastableNodeInfoPopup(BlastableNodeVO node, boolean realizeNow) {
        this(null, node, realizeNow);
    }

    public BlastableNodeInfoPopup(String title, BlastableNodeVO node, boolean realizeNow) {
        super(title, /*realize now*/ false); // skip init until node is set
        _node = node;

        if (realizeNow)
            realize();
    }

    protected void populateContent() {
        if (_node == null)
            return;

        clear();

        FlexTable grid = new FlexTable();
        grid.setStyleName("BlastInfoPopup");
        grid.setCellPadding(0);
        grid.setCellSpacing(0);

        addRow(0, grid, "Dataset", _node.getNodeName());
        addRow(1, grid, "Description", StringUtils.wrapTextAsHTML(cleanupDescription(_node), 50));
        addRow(2, grid, "Length", NumberUtils.formatInteger(_node.getLength()));
        addRow(3, grid, "Sequences", NumberUtils.formatInteger(_node.getSequenceCount()));
        addRow(4, grid, "Type", _node.getSequenceType());

        add(grid);
    }

    private String cleanupDescription(BlastableNodeVO node) {
        if (node.getDescription() == null)
            return "None";
        return node.getDescription().trim();
    }
}