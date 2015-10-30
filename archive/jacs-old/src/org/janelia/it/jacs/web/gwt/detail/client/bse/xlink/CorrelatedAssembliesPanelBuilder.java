
package org.janelia.it.jacs.web.gwt.detail.client.bse.xlink;

import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.genomics.ScaffoldReadAlignment;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTable;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableCell;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableRow;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.NumericColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.TextColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.DataRetrievedListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.PagedDataRetriever;
import org.janelia.it.jacs.web.gwt.detail.client.bse.BSEntityPanel;

/**
 * Responsible for populating the entity details table
 */
public class CorrelatedAssembliesPanelBuilder extends BaseCorrelatedEntitiesPanelBuilder {
    // constants for column positions
    private static int ID_COLUMN = 0;
    private static int LENGTH_COLUMN = 1;
    private static int BEGIN_COLUMN = 2;
    private static int END_COLUMN = 3;
    private static int DIRECTION_COLUMN = 4;
    private static int DESCRIPTION_COLUMN = 5;

    private static int DESCRIPTION_COLUMN_LENGTH = 40;

    // constants for column headings
    private static String ID_COLUMN_HEADING = "ID";
    private static String LENGTH_COLUMN_HEADING = "Len.";
    private static String BEGIN_COLUMN_HEADING = "Beg.";
    private static String END_COLUMN_HEADING = "End";
    private static String DIRECTION_COLUMN_HEADING = "Dir.";
    private static String DESCRIPTION_COLUMN_HEADING = "Description";

    /**
     * Go through DetailSubPanelBuilderFactory for getting instance
     */
    public CorrelatedAssembliesPanelBuilder(BSEntityPanel parentPanel) {
        super(parentPanel, "CorrelatedAssemblies");
    }

    public Widget createContent() {
        return createContent("Assemblies", false);
    }

    private class RelatedAssembliesRetriever implements PagedDataRetriever {

        private RelatedAssembliesRetriever() {
        }

        public void retrieveTotalNumberOfDataRows(final DataRetrievedListener listener) {
            cRefService.getNumAssembledScaffoldForReadByAccNo(getEntityAccessionNo(), createDataCountCallback(listener));
        }

        public void retrieveDataRows(int startIndex,
                                     int numRows,
                                     SortArgument[] sortArgument,
                                     final DataRetrievedListener listener) {
            cRefService.getAssembledScaffoldForReadByAccNo(getEntityAccessionNo(), startIndex, numRows, sortArgument,
                    createDataListCallback(listener));
        }

    }

    protected PagedDataRetriever createDataRetriever() {
        return new RelatedAssembliesRetriever();
    }

    protected SortableTable createDataTable() {
        SortableTable dataTable = new SortableTable();
        // add table columns
        dataTable.addColumn(new TextColumn(ID_COLUMN_HEADING));
        dataTable.addColumn(new NumericColumn(LENGTH_COLUMN_HEADING));
        dataTable.addColumn(new NumericColumn(BEGIN_COLUMN_HEADING));
        dataTable.addColumn(new NumericColumn(END_COLUMN_HEADING));
        dataTable.addColumn(new TextColumn(DIRECTION_COLUMN_HEADING));
        dataTable.addColumn(new TextColumn(DESCRIPTION_COLUMN_HEADING));
        return dataTable;
    }

    protected String[][] getSortOptions() {
        return new String[][]{
                {"scaffoldAcc", ID_COLUMN_HEADING},
                {"scaffoldLength", LENGTH_COLUMN_HEADING},
                {"scaffoldBegin", BEGIN_COLUMN_HEADING},
                {"scaffoldEnd", END_COLUMN_HEADING},
                {"scaffoldLength", DIRECTION_COLUMN_HEADING},
                {"assemblyDescription", DESCRIPTION_COLUMN_HEADING}
        };
    }

    protected TableRow formatDataAsTableRow(Object data) {
        ScaffoldReadAlignment assembledRead = (ScaffoldReadAlignment) data;
        TableRow row = new TableRow();
        row.setValue(ID_COLUMN, new TableCell(assembledRead.getScaffoldAcc(),
                createEntityLinkColumnValue(assembledRead.getScaffoldAcc())));
        row.setValue(LENGTH_COLUMN, new TableCell(assembledRead.getScaffoldLength()));
        row.setValue(BEGIN_COLUMN, new TableCell(assembledRead.getScaffoldBegin()));
        row.setValue(END_COLUMN, new TableCell(assembledRead.getScaffoldEnd()));
        int orientation = 1;
        if (assembledRead.getScaffoldOrientation() != null) {
            orientation = assembledRead.getScaffoldOrientation().intValue();
        }
        row.setValue(DIRECTION_COLUMN, new TableCell(assembledRead.getScaffoldOrientation(),
                createDNAOrientationColumnValue(orientation)));
        row.setValue(DESCRIPTION_COLUMN, new TableCell(assembledRead.getAssemblyDescription(),
                createTextColumnValue(assembledRead.getAssemblyDescription(), DESCRIPTION_COLUMN_LENGTH)));
        return row;
    }

}
