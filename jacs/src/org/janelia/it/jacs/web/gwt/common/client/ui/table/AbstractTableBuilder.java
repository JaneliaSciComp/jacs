
package org.janelia.it.jacs.web.gwt.common.client.ui.table;

import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.TableColumn;

import java.util.List;

/**
 * Base class for Builder pattern for building a SortableTable.  Supports caller adding/inserting custom columns
 * at any location.
 *
 * @author Michael Press
 */
abstract public class AbstractTableBuilder {
    private SortableTable _table;
    private List<TableColumn> _customCols;

    protected AbstractTableBuilder(List<TableColumn> customCols) {
        _customCols = customCols;
    }

    abstract protected SortableTable createTable();

    /**
     * Returns a TableRow for insert into a SortableTable.  Subclass is responsible for building each
     * TableRow from the Lists of TableCells for the standard columns and the custom columns.
     *
     * @param stockCellList standard cells defined by the table builder
     * @param customCells   custom cells supplied by the caller
     * @return a TableRow suitable for addition into the table or given to a PagingPanel
     */
    abstract public TableRow createTableRow(List<TableCell> stockCellList, List<TableCell> customCells);

    /**
     * Returns the sort options, which can include standard columns and/or custom columns.
     */
    abstract public String[][] getSortOptions(String[][] customSortOptions);

    public SortableTable getSortableTable() {
        if (_table == null)
            _table = createTable();
        return _table;
    }

    protected List<TableColumn> getCustomColumns() {
        return _customCols;
    }

    protected int getNumCustomColumns() {
        return _customCols.size();
    }
}
