
package org.janelia.it.jacs.web.gwt.common.client.ui.link;

import com.google.gwt.user.client.ui.ClickListener;

/**
 * @author Michael Press
 */
public class OptionItem {
    private String _label;
    private ClickListener _clickListener;

    public OptionItem(String label, ClickListener clickListener) {
        _label = label;
        _clickListener = clickListener;
    }

    public ClickListener getClickListener() {
        return _clickListener;
    }

    public String getLabel() {
        return _label;
    }
}
