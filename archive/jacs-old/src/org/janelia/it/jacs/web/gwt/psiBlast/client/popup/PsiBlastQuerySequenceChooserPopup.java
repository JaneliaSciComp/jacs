
package org.janelia.it.jacs.web.gwt.psiBlast.client.popup;

import org.janelia.it.jacs.model.genomics.SequenceType;
import org.janelia.it.jacs.web.gwt.common.client.panel.user.UploadUserSequencePanel;
import org.janelia.it.jacs.web.gwt.common.client.ui.SelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.util.BlastData;

/**
 * @author Michael Press
 */
public class PsiBlastQuerySequenceChooserPopup extends BasePsiBlastPopup {
    private UploadUserSequencePanel _sequencePanel;

    public PsiBlastQuerySequenceChooserPopup(SelectionListener selectionListener, BlastData blastData) {
        super("Select a Query Sequence", selectionListener, blastData);
    }

    protected void populateContent() {
        // propagate the selection listener to the popup creator
        _sequencePanel = new UploadUserSequencePanel(new SequenceSelectionListener(), new SequenceDoubleClickSelectedListener(),
                new String[]{"5", "10", "20"}, getBlastData(), SequenceType.PEPTIDE, 5);
        add(_sequencePanel);
    }

    public void setBlastData(BlastData blastData) {
        super.setBlastData(blastData);
        if (_sequencePanel != null)
            _sequencePanel.setBlastData(blastData);
    }

    /**
     * Have to override the Apply button action to first check if any pasted FASTA sequence is valid
     */
    protected void onApply() {
        if (!_sequencePanel.isPastedSequence())
            super.onApply();
        else if (_sequencePanel.validateAndPersistSequenceSelection()) // shows error message if not valid
            super.onApply();
    }

    public void clear() {
        if (_sequencePanel != null) // may not have been created yet
            _sequencePanel.clear();
    }
}