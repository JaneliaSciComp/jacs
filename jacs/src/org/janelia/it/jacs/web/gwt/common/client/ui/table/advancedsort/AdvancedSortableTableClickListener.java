
package org.janelia.it.jacs.web.gwt.common.client.ui.table.advancedsort;

import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupBelowLauncher;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTable;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.TableColumn;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Cristian Goina
 */
public class AdvancedSortableTableClickListener implements AdvancedSortClickListener {

    private SortableTable table;
    private SortableColumn[] sortableColumns;

    public AdvancedSortableTableClickListener(SortableTable table, SortableColumn[] sortableColumns) {
        this.table = table;
        this.sortableColumns = sortableColumns;
    }

    public void onClick(Widget widget) {
        // make only visible columns available for sorting
        List sortableColumnsList = new ArrayList();
        for (int i = 0; i < sortableColumns.length; i++) {
            TableColumn tCol = table.getCol(sortableColumns[i].getColumnPosition());
            if (tCol.isVisible()) {
                sortableColumnsList.add(sortableColumns[i]);
            }
        }
        SortableColumn[] currentSortableColumns = new SortableColumn[sortableColumnsList.size()];
        for (int i = 0; i < currentSortableColumns.length; i++) {
            currentSortableColumns[i] = (SortableColumn) sortableColumnsList.get(i);
        }
        new PopupBelowLauncher(new AdvancedSortPopup(currentSortableColumns,
                table.getSortColumns(),
                this,
                false)).showPopup(widget);
    }

    public void sortBy(SortableColumn[] sortColumns) {
        // only set the sort columns in the table since the paginator will get them from there
        table.setSortColumns(sortColumns);
        table.sort();
    }

}
