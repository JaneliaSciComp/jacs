
package org.janelia.it.jacs.web.gwt.detail.client.bse.scaffold;

import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.genomics.PeptideDetail;
import org.janelia.it.jacs.web.gwt.common.client.security.ClientSecurityUtils;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.SelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.NotLoggedInLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTable;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableCell;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableRow;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.NumericColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.TableColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.TextColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.FulltextPopperUpperHTML;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.DataRetrievedListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.PagedDataRetriever;
import org.janelia.it.jacs.web.gwt.common.shared.data.EntityListener;
import org.janelia.it.jacs.web.gwt.detail.client.bse.BSEntityPanel;
import org.janelia.it.jacs.web.gwt.detail.client.peptide.PeptideTableBuilder;
import org.janelia.it.jacs.web.gwt.detail.client.util.AnnotationUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Tareq Nabeel
 *         This is a copy of Christian's ScaffoldReadsPanelBuilder.  It's involves in retrieval and building of peptides
 *         panel next to reads
 */
public class ScaffoldPeptidesPanelBuilder extends ScaffoldFeaturesPanelBuilder {
    private static Logger logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.detail.client.bse.scaffold.ScaffoldPeptidesPanelBuilder");
    // constants for column positions
    private static int ID_COLUMN = 0;

    // constants for column headings
    private static String BEGIN_COLUMN_HEADING = "Beg.";
    private static String END_COLUMN_HEADING = "End";
    private static String DIRECTION_COLUMN_HEADING = "Dir.";
    private PeptideTableBuilder peptideTableBuilder;

    /**
     * Go through DetailSubPanelBuilderFactory for getting instance
     */
    protected ScaffoldPeptidesPanelBuilder(BSEntityPanel parentPanel, EntityListener entitySelectedListener) {
        super(parentPanel, entitySelectedListener);
    }


    private class ScaffoldPeptideSelectedListener implements SelectionListener {
        public void onSelect(String value) {
            int row = Integer.parseInt(value);
            PeptideDetail peptideDetail = (PeptideDetail) getDataTable().getRowData(row);
            TableCell peptideAccCell = getDataTable().getValue(row, ID_COLUMN);
            String peptideAcc = (String) peptideAccCell.getValue();
            getEntitySelectedListener().onEntitySelected(peptideAcc, peptideDetail);
        }

        public void onUnSelect(String value) {
        }
    }

    private class ScaffoldPeptidesRetriever implements PagedDataRetriever {
        public void retrieveTotalNumberOfDataRows(final DataRetrievedListener listener) {
            cRefService.getNumOfPeptidesForScaffoldByAccNo(getEntityAccessionNo(), createDataCountCallback(listener));
        }

        public void retrieveDataRows(int startIndex,
                                     int numRows,
                                     SortArgument[] sortArgument,
                                     final DataRetrievedListener listener) {
            cRefService.getPeptidesForScaffoldByAccNo(getEntityAccessionNo(), startIndex, numRows, sortArgument,
                    createDataListCallback(listener));
        }
    }

    protected PagedDataRetriever createDataRetriever() {
        return new ScaffoldPeptidesRetriever();
    }

    protected SelectionListener createSelectionListener() {
        return new ScaffoldPeptideSelectedListener();
    }

    protected SortableTable createDataTable() {
        // Custom columns for the standard peptide table
        List<TableColumn> customColumns = new ArrayList();
        customColumns.add(new NumericColumn(BEGIN_COLUMN_HEADING));
        customColumns.add(new NumericColumn(END_COLUMN_HEADING));
        customColumns.add(new TextColumn(DIRECTION_COLUMN_HEADING));

        peptideTableBuilder = new PeptideTableBuilder(customColumns);
        return peptideTableBuilder.getSortableTable();
    }

    protected String[][] getSortOptions() {
        return peptideTableBuilder.getSortOptions(new String[][]{
                {"peptideBegin", BEGIN_COLUMN_HEADING},
                {"peptideEnd", END_COLUMN_HEADING},
                {"peptideDirection", DIRECTION_COLUMN_HEADING}
        });
    }


    protected TableRow formatDataAsTableRow(Object data) {
        // Stock peptide table columns
        PeptideDetail peptideDetail = (PeptideDetail) data;
        List<TableCell> cells = new ArrayList();
        if (ClientSecurityUtils.isAuthenticated())
            cells.add(new TableCell(peptideDetail.getAcc(), createEntityLinkColumnValue(peptideDetail.getAcc())));
        else
            cells.add(new TableCell(peptideDetail.getAcc(), new NotLoggedInLink(peptideDetail.getAcc())));
        String pepFunc = peptideDetail.getProteinFunction();
        if (pepFunc != null && pepFunc.length() > 0)
            cells.add(new TableCell(peptideDetail.getProteinFunction(), new FulltextPopperUpperHTML(peptideDetail.getProteinFunction(), 40)));
        else
            cells.add(new TableCell(pepFunc));

        cells.add(new TableCell(peptideDetail.getGeneSymbol()));
        cells.add(new TableCell(
                AnnotationUtil.createAnnotationStringFromAnnotationList(peptideDetail.getGoAnnotationDescription()),
                AnnotationUtil.createAnnotationLinksWidget(peptideDetail.getGoAnnotationDescription())));
        cells.add(new TableCell(
                AnnotationUtil.createAnnotationStringFromAnnotationList(peptideDetail.getEcAnnotationDescription()),
                AnnotationUtil.createAnnotationLinksWidget(peptideDetail.getEcAnnotationDescription())));
        cells.add(new TableCell(peptideDetail.getLength()));

        // Custom column values
        List<TableCell> customCells = new ArrayList();
        customCells.add(new TableCell(peptideDetail.getDnaBegin()));
        customCells.add(new TableCell(peptideDetail.getDnaEnd()));
        int orientation = 1;
        if (peptideDetail.getDnaOrientation() != null) {
            orientation = peptideDetail.getDnaOrientation().intValue();
        }
        customCells.add(new TableCell(peptideDetail.getDnaOrientation(),
                createDNAOrientationColumnValue(orientation)));

        TableRow row = peptideTableBuilder.createTableRow(cells, customCells);
        row.setRowObject(peptideDetail);

        return row;
    }

}