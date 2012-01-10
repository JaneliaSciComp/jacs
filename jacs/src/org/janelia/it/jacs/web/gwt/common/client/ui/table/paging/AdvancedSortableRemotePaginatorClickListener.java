
package org.janelia.it.jacs.web.gwt.common.client.ui.table.paging;

import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupBelowLauncher;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTable;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.advancedsort.AdvancedSortClickListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.advancedsort.AdvancedSortPopup;

/**
 * @author Cristian Goina
 */
public class AdvancedSortableRemotePaginatorClickListener implements AdvancedSortClickListener {
    private SortableTable table;
    private RemotingPaginator paginator;
    private RemotePagingPanel pagingPanel;

    public AdvancedSortableRemotePaginatorClickListener(SortableTable table,
                                                        RemotingPaginator paginator,
                                                        RemotePagingPanel pagingPanel) {
        this.table = table;
        this.paginator = paginator;
        this.pagingPanel = pagingPanel;
    }

    public void onClick(Widget widget) {
        new PopupBelowLauncher(new AdvancedSortPopup(paginator.getAllSortableColumns(),
                table.getSortColumns(),
                this,
                false)).showPopup(widget);
    }

    public void sortBy(SortableColumn[] sortColumns) {
        paginator.clearData();
        // only set the sort columns in the table since the paginator will get them from there
        paginator.setSortColumns(sortColumns);
        pagingPanel.first();   // handles loading label visiblility
    }

    protected SortableTable getTable() {
        return table;
    }

    protected RemotingPaginator getPaginator() {
        return paginator;
    }

    protected RemotePagingPanel getPagingPanel() {
        return pagingPanel;
    }

}
