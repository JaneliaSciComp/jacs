
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