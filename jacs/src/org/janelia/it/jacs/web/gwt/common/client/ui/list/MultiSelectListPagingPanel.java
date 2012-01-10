
package org.janelia.it.jacs.web.gwt.common.client.ui.list;

import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.OptionItem;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.SelectOptionsLinks;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTable;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.PagingPanel;

import java.util.Arrays;

/**
 * Extends PagingPanel to make custom footer for multi-select lists.
 *
 * @author Michael Press
 */
public class MultiSelectListPagingPanel extends PagingPanel {
    public MultiSelectListPagingPanel(SortableTable sortableTable, String rowsPerPagePreferenceKey,
                                      ClickListener selectAllClickListener, ClickListener selectNoneClickListener) {
        super(sortableTable, rowsPerPagePreferenceKey);
        init(selectAllClickListener, selectNoneClickListener);
    }

    public void init(ClickListener selectAllClickListener, ClickListener selectNoneClickListener) {
        setStyleName("MultiSelectList");
        getPagingControlsPanelBottom().clear(); // turn off bottom controls
        createFooter(selectAllClickListener, selectNoneClickListener);
    }

    /**
     * Replace the standard footer with one suitable for a multi-select list
     */
    private void createFooter(ClickListener selectAllClickListener, ClickListener selectNoneClickListener) {
        DockPanel footer = getTableFooterPanel();
        footer.setVisible(false);
        footer.clear();
        footer.setWidth("100%");

        Widget left = getSelectPanel(selectAllClickListener, selectNoneClickListener);
        footer.add(left, DockPanel.WEST);
        footer.setCellHorizontalAlignment(left, HorizontalPanel.ALIGN_LEFT);
        footer.setCellVerticalAlignment(left, VerticalPanel.ALIGN_MIDDLE);

        Widget sizerPanel = getSizerLinkPanel();
        footer.add(sizerPanel, DockPanel.EAST);
        footer.setCellHorizontalAlignment(sizerPanel, HorizontalPanel.ALIGN_RIGHT);
        footer.setCellVerticalAlignment(sizerPanel, VerticalPanel.ALIGN_MIDDLE);

        footer.setVisible(true);
    }

    public Widget getSelectPanel(ClickListener selectAllClickListener, ClickListener selectNoneClickListener) {
        OptionItem selectAll = new OptionItem("all", selectAllClickListener);
        OptionItem selectNone = new OptionItem("none", selectNoneClickListener);
        return new SelectOptionsLinks("Select", Arrays.asList(selectAll, selectNone));
    }

    /**
     * Instead of the Goto menu, just put a separator
     */
    protected Widget createGoToMenu() {
        return createVerticalBarSeparator();
    }

    protected String getNextGoToPrevPanelStyleName() {
        return "MultiSelectListPagingSeparatorPanel"; // need more margin since the menu's gone
    }
}
