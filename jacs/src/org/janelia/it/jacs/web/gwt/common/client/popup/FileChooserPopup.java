
package org.janelia.it.jacs.web.gwt.common.client.popup;

import org.janelia.it.jacs.web.gwt.common.client.panel.FileChooserPanel;
import org.janelia.it.jacs.web.gwt.common.client.ui.SelectionListener;

import java.util.ArrayList;

/**
 * @author Michael Press
 */
public class FileChooserPopup extends BaseFileChooserPopup {
    private FileChooserPanel _sequencePanel;

    public FileChooserPopup(SelectionListener selectionListener) {
        super("Select a File", selectionListener);
    }

    protected void populateContent() {
        // propagate the selection listener to the popup creator
        ArrayList<FileChooserPanel.FILE_TYPE> fileTypes = new ArrayList<FileChooserPanel.FILE_TYPE>();
        fileTypes.add(FileChooserPanel.FILE_TYPE.fasta);
        fileTypes.add(FileChooserPanel.FILE_TYPE.frg);
        _sequencePanel = new FileChooserPanel(new SequenceSelectionListener(), fileTypes);
        add(_sequencePanel);
    }

    /**
     * Have to override the Apply button action to first check if any pasted FASTA sequence is valid
     */
    protected void onApply() {
        super.onApply();
    }

    public void clear() {
        if (_sequencePanel != null) // may not have been created yet
            _sequencePanel.clear();
    }
}