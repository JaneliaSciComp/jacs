
package org.janelia.it.jacs.web.gwt.search.client.panel.iconpanel;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.MouseListenerAdapter;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author Michael Press
 */
public class SearchIconMouseManager extends MouseListenerAdapter implements ClickListener {
    SearchIconPanel _iconPanel;

    public SearchIconMouseManager(SearchIconPanel iconPanel) {
        _iconPanel = iconPanel;
    }

    /**
     * add the hover style name on mouse enter (if not selected)
     */
    public void onMouseEnter(Widget widget) {
        if (!_iconPanel.isSelected()) {
            _iconPanel.removeStyleName(_iconPanel.getPanelUnselectedStyleName());
            _iconPanel.addStyleName(_iconPanel.getPanelHoverStyleName());
        }
    }

    /**
     * remove the hover style name on mouse enter (if not selected)
     */
    public void onMouseLeave(Widget widget) {
        if (!_iconPanel.isSelected()) {
            _iconPanel.removeStyleName(_iconPanel.getPanelHoverStyleName());
            _iconPanel.addStyleName(_iconPanel.getPanelUnselectedStyleName());
        }
    }

    public void onClick(Widget w) {
        // Toggle selection unless this is the last icon selected (can't unselect the last icon)
        if (!_iconPanel.isSelected() || _iconPanel.getSelectionCounter().getCount() > 1) {
            _iconPanel.toggleSelected();

            if (_iconPanel.isSelected()) { /* unselect */
                _iconPanel.removeStyleName(_iconPanel.getPanelUnselectedStyleName());
                _iconPanel.removeStyleName(_iconPanel.getPanelHoverStyleName());
                _iconPanel.addStyleName(_iconPanel.getPanelSelectedStyleName());
                _iconPanel.unselectOtherIcons();
                _iconPanel.getSelectionCounter().increment();
            }
            else { /* select */
                _iconPanel.removeStyleName(_iconPanel.getPanelSelectedStyleName());
                _iconPanel.addStyleName(_iconPanel.getPanelHoverStyleName());
                _iconPanel.getSelectionCounter().decrement();
            }
        }
    }
}

