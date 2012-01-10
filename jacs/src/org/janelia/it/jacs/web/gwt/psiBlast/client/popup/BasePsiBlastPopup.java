
package org.janelia.it.jacs.web.gwt.psiBlast.client.popup;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.popup.BasePopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.ui.ButtonSet;
import org.janelia.it.jacs.web.gwt.common.client.ui.DoubleClickSelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.ui.SelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.util.BlastData;

/**
 * @author Michael Press
 */
abstract public class BasePsiBlastPopup extends BasePopupPanel {
    private SelectionListener _selectionListener;
    private RoundedButton _applyButton;
    private String _selectedValue;
    private BlastData _blastData;

    protected BasePsiBlastPopup(String title, SelectionListener selectionListener, BlastData blastData) {
        super(title, /*realize now*/false, /*autohide*/false, /*modal*/ false);
        _selectionListener = selectionListener;
        setBlastData(blastData);
    }


    private void setSelectedValue(String value) {
        _selectedValue = value;
        if (_selectedValue == null)
            _applyButton.setEnabled(false);
        else
            _applyButton.setEnabled(true);
    }

    public void setBlastData(BlastData blastData) {
        _blastData = blastData;
    }

    public class SequenceSelectionListener implements SelectionListener {

        public void onSelect(String value) {
            setSelectedValue(value);
        }

        public void onUnSelect(String value) {
            setSelectedValue(null);
        }
    }

    public class SequenceDoubleClickSelectedListener implements DoubleClickSelectionListener {
        public void onSelect(String value) {
            setSelectedValue(value);
            _applyButton.execute();
        }
    }

    protected ButtonSet createButtons() {
        // Apply button = when a new node is selected, close the popup and notify the listener
        _applyButton = new RoundedButton("Apply", new ClickListener() {

            public void onClick(Widget widget) {
                onApply();
            }
        });
        _applyButton.setEnabled(false);

        RoundedButton closeButton = new RoundedButton("Close", new ClickListener() {
            public void onClick(Widget widget) {
                hidePopup();
            }
        });

        return new ButtonSet(new RoundedButton[]{_applyButton, closeButton});
    }

    /**
     * Allows subclasses to override Apply button behavior
     */
    protected void onApply() {
        hidePopup();
        notifySelectionListener();
    }

    private void notifySelectionListener() {
        if (_selectionListener != null)
            _selectionListener.onSelect(_selectedValue);
    }

    /**
     * Hook for inner classes to hide the popup
     */
    protected void hidePopup() {
        hide();
    }

    protected BlastData getBlastData() {
        return _blastData;
    }

}