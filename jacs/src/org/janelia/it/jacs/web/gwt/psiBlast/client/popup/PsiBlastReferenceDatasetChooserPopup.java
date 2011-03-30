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

package org.janelia.it.jacs.web.gwt.psiBlast.client.popup;

import org.janelia.it.jacs.model.genomics.SequenceType;
import org.janelia.it.jacs.web.gwt.blast.client.panel.BlastableNodePanel;
import org.janelia.it.jacs.web.gwt.common.client.ui.SelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.util.BlastData;

/**
 * Popup to display and select blastable nodes
 *
 * @author Michael Press
 */
public class PsiBlastReferenceDatasetChooserPopup extends BasePsiBlastPopup {
    private BlastableNodePanel _blastableNodePanel;

    public PsiBlastReferenceDatasetChooserPopup(SelectionListener selectionListener, BlastData blastData) {
        super("Select a Reference Dataset", selectionListener, blastData);
    }

    protected void populateContent() {
        _blastableNodePanel = new BlastableNodePanel(SequenceType.PEPTIDE, new SequenceSelectionListener(), new SequenceDoubleClickSelectedListener(),
                getBlastData());
        _blastableNodePanel.realize();//TODO: when to call this?

        // propagate the selection listener to the popup creator
        add(_blastableNodePanel);
    }


    public void setBlastData(BlastData blastData) {
        super.setBlastData(blastData);
        if (_blastableNodePanel != null)
            _blastableNodePanel.setBlastData(blastData);
    }

    public void clear() {
        if (_blastableNodePanel != null) // may not have been created yet
            _blastableNodePanel.clear();
    }
}