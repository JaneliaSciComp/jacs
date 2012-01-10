
package org.janelia.it.jacs.web.gwt.common.client.ui.table;

import org.janelia.it.jacs.model.common.SortArgument;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 * Date: May 25, 2007
 * Time: 4:29:33 PM
 */
public class SortableColumn extends SortArgument {

    private int columnPosition;
    private String columnHeading;

    public SortableColumn() {
        super();
        columnPosition = -1;
    }

    public SortableColumn(int columnPosition, String columnHeading, int sortDirection) {
        super();
        this.columnPosition = columnPosition;
        this.columnHeading = columnHeading;
        setSortDirection(sortDirection);
    }

    public SortableColumn(int columnPosition, String columnHeading, String columnSortName) {
        super(columnSortName);
        this.columnPosition = columnPosition;
        this.columnHeading = columnHeading;
    }

    public SortableColumn(int columnPosition, String columnHeading, String columnSortName, int sortDirection) {
        super(columnSortName, sortDirection);
        this.columnPosition = columnPosition;
        this.columnHeading = columnHeading;
    }

    public String getColumnHeading() {
        return columnHeading;
    }

    public void setColumnHeading(String columnHeading) {
        this.columnHeading = columnHeading;
    }

    public int getColumnPosition() {
        return columnPosition;
    }

    public void setColumnPosition(int columnPosition) {
        this.columnPosition = columnPosition;
    }

}
