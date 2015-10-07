
package org.janelia.it.jacs.web.gwt.detail.client.bse.xlink;

import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.genomics.BseEntityDetail;
import org.janelia.it.jacs.model.genomics.EntityTypeGenomic;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTable;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableCell;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableRow;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.NumericColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.TextColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.FulltextPopperUpperHTML;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.CachedPagedDataRetriever;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.DataRetrievedListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.PagedDataRetriever;
import org.janelia.it.jacs.web.gwt.detail.client.bse.BSEntityPanel;
import org.janelia.it.jacs.web.gwt.detail.client.util.AnnotationUtil;

/**
 * Responsible for populating the entity details table
 */
public class CorrelatedFeaturesPanelBuilder extends BaseCorrelatedEntitiesPanelBuilder {
    // constants for column positions
    private static final int ID_COLUMN = 0;
    private static final int TYPE_COLUMN = 1;
    private static final int LENGTH_COLUMN = 2;
    private static final int BEGIN_COLUMN = 3;
    private static final int END_COLUMN = 4;
    private static final int DIRECTION_COLUMN = 5;
    private static final int PROTEIN_FUNCTION_COLUMN = 6;
    private static final int GENE_SYMBOL_COLUMN = 7;
    private static final int GO_COLUMN = 8;
    private static final int EC_NUM_COLUMN = 9;

    // constants for column headings
    private static String ID_COLUMN_HEADING = "ID";
    private static String TYPE_COLUMN_HEADING = "Type";
    private static String LENGTH_COLUMN_HEADING = "Len.";
    private static String BEGIN_COLUMN_HEADING = "Beg.";
    private static String END_COLUMN_HEADING = "End";
    private static String DIRECTION_COLUMN_HEADING = "Dir.";

    private static final String PROTEIN_FUNCTION_HEADING = "Protein Function";
    private static final String GENE_SYMBOL_HEADING = "Gene Symbol";
    private static final String GO_HEADING = "Gene Ontology";
    private static final String EC_NUM_HEADING = "EC #";

    private PagedDataRetriever dataRetriever;

    /**
     * Go through DetailSubPanelBuilderFactory for getting instance
     */
    public CorrelatedFeaturesPanelBuilder(BSEntityPanel parentPanel) {
        super(parentPanel, "CorrelatedFeatures");
    }

    public Widget createContent() {
        return createContent("Features", false);
    }

    private class RelatedFeaturesRetriever implements PagedDataRetriever {

        private RelatedFeaturesRetriever() {
        }

        public void retrieveTotalNumberOfDataRows(final DataRetrievedListener listener) {
            cRefService.getNumFeatures(getEntityAccessionNo(), createDataCountCallback(listener));
        }

        public void retrieveDataRows(int startIndex,
                                     int numRows,
                                     SortArgument[] sortArgument,
                                     final DataRetrievedListener listener) {
            cRefService.getRelatedORFsAndRNAs(getEntityAccessionNo(), startIndex, numRows, sortArgument,
                    createDataListCallback(listener));
        }

    }

    protected PagedDataRetriever createDataRetriever() {
        if (dataRetriever == null) {
            dataRetriever = new CachedPagedDataRetriever(new RelatedFeaturesRetriever(), 20);
        }
        return dataRetriever;
    }

    protected SortableTable createDataTable() {
        SortableTable dataTable = new SortableTable();
        // add table columns
        dataTable.addColumn(new TextColumn(ID_COLUMN_HEADING));
        dataTable.addColumn(new TextColumn(TYPE_COLUMN_HEADING));
        dataTable.addColumn(new NumericColumn(LENGTH_COLUMN_HEADING));
        dataTable.addColumn(new NumericColumn(BEGIN_COLUMN_HEADING));
        dataTable.addColumn(new NumericColumn(END_COLUMN_HEADING));
        dataTable.addColumn(new TextColumn(DIRECTION_COLUMN_HEADING));
        dataTable.addColumn(new TextColumn(PROTEIN_FUNCTION_HEADING));
        dataTable.addColumn(new TextColumn(GENE_SYMBOL_HEADING));
        dataTable.addColumn(new TextColumn(GO_HEADING));
        dataTable.addColumn(new TextColumn(EC_NUM_HEADING));
        return dataTable;
    }

    protected String[][] getSortOptions() {
        return new String[][]{
                {"accession", ID_COLUMN_HEADING},
                {"entityType", TYPE_COLUMN_HEADING},
                {"sequenceLength", LENGTH_COLUMN_HEADING},
                {"dnaBegin", BEGIN_COLUMN_HEADING},
                {"dnaEnd", END_COLUMN_HEADING},
                {"dnaOrientation", DIRECTION_COLUMN_HEADING},
                {"protein_function ", PROTEIN_FUNCTION_HEADING},
                {"gene_symbol", GENE_SYMBOL_HEADING},
                {"gene_ontology", GO_HEADING},
                {"enzyme_commission", EC_NUM_HEADING}
        };
    }

    protected TableRow formatDataAsTableRow(Object data) {

        BseEntityDetail bseDetail = (BseEntityDetail) data;
        TableRow row = new TableRow();
        row.setValue(ID_COLUMN, new TableCell(bseDetail.getAcc(), createEntityLinkColumnValue(bseDetail.getAcc())));
        if (EntityTypeGenomic.ENTITY_CODE_NON_CODING_RNA == bseDetail.getEntityTypeCode()) {
            if (bseDetail.getType() == null) row.setValue(TYPE_COLUMN, new TableCell("RNA"));
            else row.setValue(TYPE_COLUMN, new TableCell(bseDetail.getType()));
        }
        else {
            row.setValue(TYPE_COLUMN, new TableCell(bseDetail.getType()));
        }
        row.setValue(LENGTH_COLUMN, new TableCell(bseDetail.getLength()));
        switch (bseDetail.getEntityTypeCode().intValue()) {
            case EntityTypeGenomic.ENTITY_CODE_PROTEIN:
            case EntityTypeGenomic.ENTITY_CODE_ORF:
            case EntityTypeGenomic.ENTITY_CODE_NON_CODING_RNA:
                setBseEntityDetailFields(bseDetail, row);
        }
        return row;
    }

    private void setBseEntityDetailFields(BseEntityDetail bseEntityDetail, TableRow row) {
        row.setValue(BEGIN_COLUMN, new TableCell(bseEntityDetail.getDnaBegin()));
        row.setValue(END_COLUMN, new TableCell(bseEntityDetail.getDnaEnd()));
        int orientation = 1;
        if (bseEntityDetail.getDnaOrientation() != null) {
            orientation = bseEntityDetail.getDnaOrientation().intValue();
        }
        row.setValue(DIRECTION_COLUMN, new TableCell(bseEntityDetail.getDnaOrientation(),
                createDNAOrientationColumnValue(orientation)));

        String pepFunc = bseEntityDetail.getProteinFunction();
        if (pepFunc != null && pepFunc.length() > 0) {
            row.setValue(PROTEIN_FUNCTION_COLUMN, new TableCell(bseEntityDetail.getProteinFunction(), new FulltextPopperUpperHTML(bseEntityDetail.getProteinFunction(), 40)));
        }
        else {
            row.setValue(PROTEIN_FUNCTION_COLUMN, new TableCell(pepFunc));
        }

        row.setValue(GENE_SYMBOL_COLUMN, new TableCell(bseEntityDetail.getGeneSymbol()));
        row.setValue(GO_COLUMN, new TableCell(
                AnnotationUtil.createAnnotationStringFromAnnotationList(bseEntityDetail.getGoAnnotationDescription()),
                AnnotationUtil.createAnnotationLinksWidget(bseEntityDetail.getGoAnnotationDescription())));
        row.setValue(EC_NUM_COLUMN, new TableCell(
                AnnotationUtil.createAnnotationStringFromAnnotationList(bseEntityDetail.getEcAnnotationDescription()),
                AnnotationUtil.createAnnotationLinksWidget(bseEntityDetail.getEcAnnotationDescription())));

    }


}
