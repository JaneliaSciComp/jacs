
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