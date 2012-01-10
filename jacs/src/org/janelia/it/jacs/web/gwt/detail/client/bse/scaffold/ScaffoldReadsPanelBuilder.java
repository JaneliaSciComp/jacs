
package org.janelia.it.jacs.web.gwt.detail.client.bse.scaffold;

import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.genomics.Read;
import org.janelia.it.jacs.model.genomics.ScaffoldReadAlignment;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.SelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTable;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableCell;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableRow;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.NumericColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.TextColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.IntegerString;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.DataRetrievedListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.PagedDataRetriever;
import org.janelia.it.jacs.web.gwt.common.shared.data.EntityListener;
import org.janelia.it.jacs.web.gwt.detail.client.bse.BSEntityPanel;

/**
 * @author Cristian Goina
 */
public class ScaffoldReadsPanelBuilder extends ScaffoldFeaturesPanelBuilder {
    private static Logger logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.detail.client.bse.scaffold.ScaffoldReadsPanelBuilder");
    // constants for column positions
    private static int ID_COLUMN = 0;
    private static int LENGTH_COLUMN = 1;
    private static int BEGIN_COLUMN = 2;
    private static int END_COLUMN = 3;
    private static int TEMPLATE_COLUMN = 4;
    private static int DIRECTION_COLUMN = 5;

    // constants for column headings
    private static String ID_COLUMN_HEADING = "ID";
    private static String LENGTH_COLUMN_HEADING = "Len.";
    private static String BEGIN_COLUMN_HEADING = "Beg.";
    private static String END_COLUMN_HEADING = "End";
    private static String TEMPLATE_COLUMN_HEADING = "Read Template ID";
    private static String DIRECTION_COLUMN_HEADING = "Dir.";


    /**
     * Go through DetailSubPanelBuilderFactory for getting instance
     */
    protected ScaffoldReadsPanelBuilder(BSEntityPanel parentPanel, EntityListener entitySelectedListener) {
        super(parentPanel, entitySelectedListener);
    }


    private class ScaffoldReadSelectedListener implements SelectionListener {
        public void onSelect(String value) {
            int row = Integer.parseInt(value);
            ScaffoldReadAlignment scaffoldReadAlignment = (ScaffoldReadAlignment) getDataTable().getRowData(row);
            TableCell readAccCell = getDataTable().getValue(row, ID_COLUMN);
            String readAcc = (String) readAccCell.getValue();
            getEntitySelectedListener().onEntitySelected(readAcc, scaffoldReadAlignment);
        }

        public void onUnSelect(String value) {
        }

    }

    private class ScaffoldReadsRetriever implements PagedDataRetriever {
        public void retrieveTotalNumberOfDataRows(final DataRetrievedListener listener) {
            cRefService.getNumOfReadsForScaffoldByAccNo(getEntityAccessionNo(), createDataCountCallback(listener));
        }

        public void retrieveDataRows(int startIndex,
                                     int numRows,
                                     SortArgument[] sortArgument,
                                     final DataRetrievedListener listener) {
            cRefService.getReadsForScaffoldByAccNo(getEntityAccessionNo(), startIndex, numRows, sortArgument,
                    createDataListCallback(listener));
        }
    }

    protected PagedDataRetriever createDataRetriever() {
        return new ScaffoldReadsRetriever();
    }

    protected SelectionListener createSelectionListener() {
        return new ScaffoldReadSelectedListener();
    }

    protected SortableTable createDataTable() {
        SortableTable dataTable = new SortableTable();
        // add table columns
        dataTable.addColumn(new TextColumn(ID_COLUMN_HEADING));
        dataTable.addColumn(new NumericColumn(LENGTH_COLUMN_HEADING));
        dataTable.addColumn(new NumericColumn(BEGIN_COLUMN_HEADING));
        dataTable.addColumn(new NumericColumn(END_COLUMN_HEADING));
        dataTable.addColumn(new TextColumn(TEMPLATE_COLUMN_HEADING));
        dataTable.addColumn(new TextColumn(DIRECTION_COLUMN_HEADING));
        return dataTable;
    }

    protected String[][] getSortOptions() {
        return new String[][]{
                {"readAcc", ID_COLUMN_HEADING},
                {"readLength", LENGTH_COLUMN_HEADING},
                {"scaffoldBegin", BEGIN_COLUMN_HEADING},
                {"scaffoldEnd", END_COLUMN_HEADING},
                {"readTemplate", TEMPLATE_COLUMN_HEADING},
                {"scaffoldLength", DIRECTION_COLUMN_HEADING}
        };
    }

    protected Widget createDNAOrientationColumnValue(int direction) {
        if (direction == -1)
            return ImageBundleFactory.getControlImageBundle().getArrowReverseImage().createImage();
        else
            return ImageBundleFactory.getControlImageBundle().getArrowForwardImage().createImage();
    }

    protected TableRow formatDataAsTableRow(Object data) {
        ScaffoldReadAlignment scaffoldReadAlignment = (ScaffoldReadAlignment) data;
        TableRow row = new TableRow();
        row.setRowObject(scaffoldReadAlignment);
        row.setValue(ID_COLUMN, new TableCell(scaffoldReadAlignment.getReadAcc(),
                createEntityLinkColumnValue(scaffoldReadAlignment.getReadAcc())));
        Read scaffoldRead = scaffoldReadAlignment.getRead();
        if (scaffoldRead != null) {
            row.setValue(LENGTH_COLUMN, new TableCell(scaffoldRead.getSequenceLength()));
            row.setValue(TEMPLATE_COLUMN, new TableCell(scaffoldRead.getTemplateAcc()));
        }
        else {
            row.setValue(LENGTH_COLUMN, new TableCell(new IntegerString(-1, "Unknown")));
            row.setValue(TEMPLATE_COLUMN, new TableCell("unknown"));
        }
        row.setValue(BEGIN_COLUMN, new TableCell(scaffoldReadAlignment.getScaffoldBegin()));
        row.setValue(END_COLUMN, new TableCell(scaffoldReadAlignment.getScaffoldEnd()));
        int orientation = 1;
        if (scaffoldReadAlignment.getScaffoldOrientation() != null) {
            orientation = scaffoldReadAlignment.getScaffoldOrientation().intValue();
        }
        row.setValue(DIRECTION_COLUMN, new TableCell(scaffoldReadAlignment.getScaffoldOrientation(),
                createDNAOrientationColumnValue(orientation)));
        return row;
    }

}
